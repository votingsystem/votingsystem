package org.votingsystem.applet.validationtool;

import java.awt.Frame;
import java.security.Security;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.UIManager;
import org.apache.log4j.Logger;
import org.votingsystem.applet.model.AppletOperation;
import org.votingsystem.model.AppHostVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Applet extends JApplet implements AppHostVS {
    
    private static Logger logger = Logger.getLogger(Applet.class);
    
    public static enum ModoEjecucion {APPLET, APLICACION}
    
    private Frame frame;
    private String locale = "es";
    private Timer recolectorOperaciones;
    private OperationVS operacionEnCurso;
    public static ModoEjecucion modoEjecucion = ModoEjecucion.APPLET;
    
    public Applet() { }
        
    @Override  public void init() {
        logger.debug("init");
        //Execute a job on the event-dispatching thread:
        //creating this applet's GUI.
        try {
            ValidationToolContext.init(this, "log4jValidationTool.properties", 
                    "validationToolMessages_", locale);
            
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
        if(modoEjecucion == ModoEjecucion.APPLET) {
            if(getParameter("locale") != null) locale = getParameter("locale");
        } 
        init();
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
        AppletOperation operacion = new AppletOperation(ResponseVS.SC_PROCESSING);
        operacion.setType(AppletOperation.Type.MENSAJE_HERRAMIENTA_VALIDACION);
        operacion.setMessage(ContextVS.INSTANCE.getString("appletHerramientaInicializado"));
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
                        Applet appletFirma = new Applet();
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
        operacionEnCurso = AppletOperation.parse(operacionJSONStr);
        DialogoPrincipal dialogo = new DialogoPrincipal(new JFrame(), false);
        dialogo.setVisible(true);
    }
    
    @Override public void sendMessageToHost(OperationVS operation) {
        if (operation == null) {
            logger.debug(" - sendMessageToHost - Operacion null");
            return;
        }
        AppletOperation messageToHost = (AppletOperation)operation;
        logger.debug(" - sendMessageToHost - status: " + 
                messageToHost.getStatusCode() + " - operación: " + 
                messageToHost.toJSON().toString());
        try {
            if(modoEjecucion == ModoEjecucion.APPLET) {
                Object[] args = {messageToHost.toJSON().toString()};
                Object object = netscape.javascript.JSObject.getWindow(this).
                        call("setMessageFromValidationTool", args);
            } else logger.debug("---> APP EXECUTION MODE: " + modoEjecucion.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if(ModoEjecucion.APLICACION == modoEjecucion && 
                messageToHost.getStatusCode() == ResponseVS.SC_CANCELLED){
            logger.debug(" ------  System.exit(0) ------ ");
            System.exit(0);
        }
    }

    @Override public OperationVS getPendingOperation() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
}