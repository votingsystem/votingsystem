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

    //public static final String CERT_CHAIN_URL_SUFIX = "certificado/cadenaCertificacion";
    public static final String SERVER_INFO_URL_SUFIX = "infoServidor";
    
    public static enum ModoEjecucion {APPLET, APLICACION}
    
    private Frame frame;
    private Timer recolectorOperaciones;
    private AtomicBoolean cancelado = new AtomicBoolean(false);
    private Operacion operacionEnCurso;
    public static String locale = "es";
    public static ModoEjecucion modoEjecucion = ModoEjecucion.APPLET;
    
    public static AppletFirma INSTANCIA;
    
    public AppletFirma() {
        INSTANCIA = this;
        logger.debug("AppletFirma");
    }
        
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
            logger.debug("Gestor previamente finalizado");
            return;
        }
        recolectorOperaciones.cancel();
        Operacion operacion = new Operacion();
        operacion.setTipo(Operacion.Tipo.MENSAJE_CIERRE_APPLET);
        enviarMensajeAplicacion(operacion);
        cancelado.set(true);
    }
    
    private void enviarMensajeAplicacion(Operacion operacion) {
        if (operacion == null) {
            logger.debug(" - enviarMensajeAplicacion - operación nula");
            return;
        }
        logger.debug(" - enviarMensajeAplicacion - operación: " + operacion.obtenerJSONStr());
        try {
            if(AppletFirma.modoEjecucion == AppletFirma.ModoEjecucion.APPLET) {
                Object[] args = {operacion.obtenerJSONStr()};
                Object object = netscape.javascript.JSObject.getWindow(this).
                        call("setClienteFirmaMessage", args);
            } else logger.debug("---------------> enviado mensaje de aplicación");
        } catch (Exception e) {
            //logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    public void responderCliente(int codigoEstado, String mensaje) {
        logger.debug("responderCliente - codigoEstado: " + codigoEstado);
        if(operacionEnCurso == null) {
            logger.debug("responderCliente ---> No hay ninguna operación en curso");
            return;
        }
        operacionEnCurso.setCodigoEstado(codigoEstado);
        operacionEnCurso.setMensaje(mensaje);
        enviarMensajeAplicacion(operacionEnCurso);
        operacionEnCurso = null;
    }
    
    public void cancelarOperacion () {
        if(operacionEnCurso == null) return;
        else if(operacionEnCurso.getCodigoEstado() == Operacion.SC_PROCESANDO) {
            operacionEnCurso.setMensaje(Contexto.INSTANCE.getString("operacionCancelada"));
            operacionEnCurso.setCodigoEstado(Operacion.SC_CANCELADO);
            enviarMensajeAplicacion(operacionEnCurso);
            operacionEnCurso = null;  
        }  
        if(AppletFirma.ModoEjecucion.APLICACION == 
                AppletFirma.modoEjecucion){
            logger.debug(" ------ System.exit(0) ------ ");
            System.exit(0);
        }
    }
        
    public void ejecutarOperacion(String operacionJSONStr) {
        logger.debug("ejecutarOperacion: " + operacionJSONStr);
        if(operacionJSONStr == null || "".equals(operacionJSONStr)) return;
        this.operacionEnCurso = Operacion.parse(operacionJSONStr);
        String errorValidacion = operacionEnCurso.getErrorValidacion();
        if(errorValidacion != null) {
            logger.debug("ejecutarOperacion - errorValidacion: " + errorValidacion);
            responderCliente(Operacion.SC_ERROR_PETICION, errorValidacion);
            return;
        }
        PreconditionsCheckerDialog preconditionsChecker = 
                new PreconditionsCheckerDialog(frame, true, operacionEnCurso);
        preconditionsChecker.setVisible(true);
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
                            .getResourceAsStream("testFiles/signClaim.json"), jsonFile);        
                        appletFirma.ejecutarOperacion(FileUtils.getStringFromFile(jsonFile));
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
            logger.debug("-- finalizando aplicación --- ");
            if(AppletFirma.ModoEjecucion.APLICACION == 
                AppletFirma.modoEjecucion){
                logger.debug(" ------ System.exit(0) ------ ");
                System.exit(0);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
}
