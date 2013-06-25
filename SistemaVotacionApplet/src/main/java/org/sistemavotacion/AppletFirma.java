package org.sistemavotacion;

import java.awt.Frame;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.UIManager;
import org.sistemavotacion.dialogo.PreconditionsCheckerDialog;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AppletFirma extends JApplet {
    
    private static Logger logger = LoggerFactory.getLogger(AppletFirma.class);

    public static final String SERVER_INFO_URL_SUFIX = "infoServidor";
    
    public static enum ModoEjecucion {APPLET, APLICACION}
    
    private Frame frame;
    private Timer recolectorOperaciones;
    private AtomicBoolean cancelado = new AtomicBoolean(false);
    private Operacion operacionEnCurso;
    public static String locale = "es";
    public static ModoEjecucion modoEjecucion = ModoEjecucion.APPLET;
    
    public AppletFirma() { }
        
    public void init() {
        logger.debug("------ init");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.debug("ShutdownHook - ShutdownHook - ShutdownHook");
                finalizar();
        }});
        //Execute a job on the event-dispatching thread:
        //creating this applet's GUI.
        try {
            Contexto.INSTANCE.init();
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                        AcercaDePanel acercaDePanel = new AcercaDePanel();
                        getContentPane().add(acercaDePanel);
                        getContentPane().repaint();
                    } catch (Exception e) {
                        e.printStackTrace();
                        //logger.error(e.getMessage(), e);
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            //logger.error(ex.getMessage(), ex);
        }
    }
        
    public void start() {
        logger.debug("start - java version: " + 
                System.getProperty("java.version"));
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    //logger.error(e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        });
        Frame[] frames = JFrame.getFrames();
        if(frames.length == 0 || frames[0] == null) frame = new javax.swing.JFrame();
        else frame = frames[0];
        if(AppletFirma.modoEjecucion == AppletFirma.ModoEjecucion.APPLET) {
            lanzarTimer();
            if(getParameter("locale") != null) locale = getParameter("locale");
        } 
        Operacion operacion = new Operacion(Operacion.SC_PROCESANDO, 
                Operacion.Tipo.MENSAJE_APPLET, 
                Contexto.INSTANCE.getString("appletInicializado"));
        enviarMensajeAplicacion(operacion);
    }

    public void stop() {
        logger.debug("stop");
        finalizar();
    }

    public void destroy() {
        logger.debug("destroy");
        finalizar();
    }

    public Operacion getOperacionEnCurso() {
        return operacionEnCurso;
    }
    
    /*
     * Timer that checks pending operations on web client app
     */
    private void lanzarTimer() {
        recolectorOperaciones =  new Timer(true);
        final netscape.javascript.JSObject jsObject = 
                netscape.javascript.JSObject.getWindow(this);
        recolectorOperaciones.scheduleAtFixedRate(
            new TimerTask(){
                public void run() { 
                    //logger.debug("Comprobando operaciones pendientes");
                    Object object = jsObject.call("obtenerOperacion", null);
                    if(object != null) {
                        ejecutarOperacion(object.toString());
                    } else {
                        //logger.debug("Testeando JSObject - respuesta nula");
                    }
                }
            }, 0, 1000);
    }
    
        
    public void finalizar() {
        logger.debug("finalizar");
        if(cancelado.get()) {
            logger.debug("finalizar - already cancelled");
            return;
        }
        recolectorOperaciones.cancel();
        Operacion operacion = new Operacion();
        operacion.setTipo(Operacion.Tipo.MENSAJE_CIERRE_APPLET);
        enviarMensajeAplicacion(operacion);
        cancelado.set(true);
    }
    
    public void enviarMensajeAplicacion(Operacion operacion) {
        if (operacion == null) {
            logger.debug(" - enviarMensajeAplicacion - Operacion null");
            return;
        }
        logger.debug(" - enviarMensajeAplicacion - status: " + 
                operacion.getCodigoEstado() + "\n - operación: " + 
                operacion.obtenerJSONStr());
        try {
            if(AppletFirma.modoEjecucion == AppletFirma.ModoEjecucion.APPLET) {
                Object[] args = {operacion.obtenerJSONStr()};
                Object object = netscape.javascript.JSObject.getWindow(this).
                        call("setClienteFirmaMessage", args);
            } else logger.debug("---> APP EXECUTION MODE: " +  
                    AppletFirma.modoEjecucion.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        operacionEnCurso = null;
        if(AppletFirma.ModoEjecucion.APLICACION == 
                AppletFirma.modoEjecucion && operacion.getCodigoEstado() == 
                Operacion.SC_CANCELADO){
            logger.debug(" ------ System.exit(0) ------ ");
            System.exit(0);
        }
    }
 
    public void ejecutarOperacion(String operacionJSONStr) {
        logger.debug("ejecutarOperacion: " + operacionJSONStr);
        if(operacionJSONStr == null || "".equals(operacionJSONStr)) return;
        operacionEnCurso = Operacion.parse(operacionJSONStr);
        if(operacionEnCurso.getErrorValidacion() != null) {
            logger.debug("ejecutarOperacion - errorValidacion: " + 
                    operacionEnCurso.getErrorValidacion());
            operacionEnCurso.setCodigoEstado(Operacion.SC_ERROR_PETICION);
            enviarMensajeAplicacion(operacionEnCurso);
            return;
        } else {
            PreconditionsCheckerDialog preconditionsChecker = 
                new PreconditionsCheckerDialog(frame, true, operacionEnCurso, this);
            preconditionsChecker.setVisible(true);
        }
    }
    
    public static void main (String[] args) { 
        modoEjecucion = ModoEjecucion.APLICACION;
        Operacion ope = new Operacion();
        String[] _args = {""};
        ope.setArgs(_args);
        logger.debug("ope: " + ope.obtenerJSONStr());
        try {
            Contexto.INSTANCE.init();
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                        AppletFirma appletFirma = new AppletFirma();
                        appletFirma.start();
                        File jsonFile = File.createTempFile("operacion", ".json");
                        jsonFile.deleteOnExit();
                        FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("testFiles/newRepresentative.json"), jsonFile);        
                        appletFirma.ejecutarOperacion(FileUtils.getStringFromFile(jsonFile));
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
            if(AppletFirma.ModoEjecucion.APLICACION == 
                AppletFirma.modoEjecucion){
                logger.debug(" ------ System.exit(0) ------ ");
                System.exit(0);
            } else logger.debug("-- finalizando aplicación --- ");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
}
