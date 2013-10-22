package org.sistemavotacion.herramientavalidacion;

import java.awt.Frame;
import java.io.IOException;
import java.security.Security;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.UIManager;
import org.apache.log4j.PropertyConfigurator;
import org.sistemavotacion.AppHost;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Operacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AppletHerramienta extends JApplet implements AppHost {
    
    private static Logger logger = LoggerFactory.getLogger(AppletHerramienta.class);
    
    public static enum ModoEjecucion {APPLET, APLICACION}
    
    private Frame frame;
    private String locale = "es";
    private Timer recolectorOperaciones;
    private Operacion operacionEnCurso;
    public static ModoEjecucion modoEjecucion = ModoEjecucion.APPLET;
    
    public AppletHerramienta() { }
        
    @Override  public void init() {
        logger.debug("init");
        //Execute a job on the event-dispatching thread:
        //creating this applet's GUI.
        try {
            Contexto.INSTANCE.init(this);
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                        AcercaDePanel acercaDePanel = new AcercaDePanel();
                        getContentPane().add(acercaDePanel);           
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public Frame getFrame() {
        return frame;
    }
        
    public void start() {
        logger.debug("start");
        init();
        Properties props = new Properties();
        try {
            props.load(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("log4jHerramienta.properties"));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        PropertyConfigurator.configure(props);
        if(modoEjecucion == ModoEjecucion.APPLET) {
            if(getParameter("locale") != null) locale = getParameter("locale");
        } 
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    if(modoEjecucion != ModoEjecucion.APPLET) {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    }
                    DialogoPrincipal dialogo = new DialogoPrincipal(
                            new JFrame(), false);
                    dialogo.setVisible(true);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        if(modoEjecucion == ModoEjecucion.APPLET) {
            lanzarTimer();
            if(getParameter("locale") != null) locale = getParameter("locale");
        } 
        Operacion operacion = new Operacion();
        operacion.setCodigoEstado(Operacion.SC_PROCESANDO);
        operacion.setTipo(Operacion.Tipo.MENSAJE_HERRAMIENTA_VALIDACION);
        operacion.setMensaje(Contexto.INSTANCE.getString("appletHerramientaInicializado"));
        sendMessageToHost(operacion);
    }
 
    public void stop() {
        logger.debug("stop");
    }

    public void destroy() {
        logger.debug("destroy");
    }

    public static void main (String[] args) { 
        logger.info("Arrancando aplicación");
        modoEjecucion = ModoEjecucion.APLICACION;
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        AppletHerramienta appletFirma = new AppletHerramienta();
                        appletFirma.start();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    private void lanzarTimer() {
        logger.info("lanzarTimer");
        recolectorOperaciones =  new Timer(true);
        final netscape.javascript.JSObject jsObject = 
                netscape.javascript.JSObject.getWindow(this);
        recolectorOperaciones.scheduleAtFixedRate(
            new TimerTask(){
                public void run() { 
                    Object object = jsObject.call(
                            "getMessageToValidationTool", null);
                    if(object != null) {
                        ejecutarOperacion(object.toString());
                    } else { }
                }
            }, 0, 1000);
    }
    
    
    public void ejecutarOperacion(String operacionJSONStr) {
        logger.debug("ejecutarOperacion: " + operacionJSONStr);
        if(operacionJSONStr == null || "".equals(operacionJSONStr)) return;
        operacionEnCurso = Operacion.parse(operacionJSONStr);
        DialogoPrincipal dialogo = new DialogoPrincipal(new JFrame(), false);
        dialogo.setVisible(true);
    }
    
    @Override public void sendMessageToHost(Operacion operacion) {
        if (operacion == null) {
            logger.debug(" - sendMessageToHost - Operacion null");
            return;
        }
        logger.debug(" - sendMessageToHost - status: " + 
                operacion.getCodigoEstado() + " - operación: " + 
                operacion.toJSON().toString());
        try {
            if(modoEjecucion == ModoEjecucion.APPLET) {
                Object[] args = {operacion.toJSON().toString()};
                Object object = netscape.javascript.JSObject.getWindow(this).
                        call("setMessageFromValidationTool", args);
            } else logger.debug("---> APP EXECUTION MODE: " + modoEjecucion.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if(ModoEjecucion.APLICACION == modoEjecucion && 
                operacion.getCodigoEstado() == Operacion.SC_CANCELADO){
            logger.debug(" ------  System.exit(0) ------ ");
            System.exit(0);
        }
    }

    @Override public Operacion getPendingOperation() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
}