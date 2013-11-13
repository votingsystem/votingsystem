package org.votingsystem.applet.votingtool;

import org.votingsystem.model.AppHostVS;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.UIManager;
import org.apache.log4j.Logger;
import org.votingsystem.applet.votingtool.dialog.PreconditionsCheckerDialog;
import org.votingsystem.model.OperationVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.applet.model.AppletOperation;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Applet extends JApplet implements AppHostVS {
    
    private static Logger logger = Logger.getLogger(Applet.class);

    public static final String SERVER_INFO_URL_SUFIX = "infoServidor";
    
    public static enum ModoEjecucion {APPLET, APLICACION}
    
    private Timer recolectorOperaciones;
    private AtomicBoolean cancelado = new AtomicBoolean(false);
    private AppletOperation operacionEnCurso;
    public static String locale = "es";
    public static ModoEjecucion modoEjecucion = ModoEjecucion.APPLET;
    
    public Applet() { }
        
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
            VotingToolContext.init(this, "log4j.properties", "messages_", locale);
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
        if(modoEjecucion == ModoEjecucion.APPLET) {
            lanzarTimer();
            if(getParameter("locale") != null) locale = getParameter("locale");
        } 
        init();
        AppletOperation operacion = new AppletOperation(
                ResponseVS.SC_PROCESSING, 
                AppletOperation.Type.APPLET_MESSAGE, 
                ContextVS.INSTANCE.getString("appletInicializado"));
        sendMessageToHost(operacion);
    }

    public void stop() {
        logger.debug("stop");
        finalizar();
    }

    public void destroy() {
        logger.debug("destroy");
        finalizar();
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
                    Object object = jsObject.call("getMessageToSignatureClient", null);
                    if(object != null) {
                        ejecutarOperacion(object.toString());
                    } else {
                        //logger.debug("Testeando JSObject - responseVS nula");
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
        AppletOperation operacion = new AppletOperation();
        operacion.setType(AppletOperation.Type.APPLET_PAUSED_MESSAGE);
        sendMessageToHost(operacion);
        cancelado.set(true);
        VotingToolContext.INSTANCE.shutdown();
    }
 
    public void ejecutarOperacion(String operacionJSONStr) {
        logger.debug("ejecutarOperacion: " + operacionJSONStr);
        if(operacionJSONStr == null || "".equals(operacionJSONStr)) return;
        operacionEnCurso = AppletOperation.parse(operacionJSONStr);
        if(operacionEnCurso.getErrorValidacion() != null) {
            logger.debug("ejecutarOperacion - errorValidacion: " + 
                    operacionEnCurso.getErrorValidacion());
            operacionEnCurso.setStatusCode(ResponseVS.SC_ERROR_REQUEST);
            sendMessageToHost(operacionEnCurso);
            return;
        } else {
            PreconditionsCheckerDialog preconditionsChecker = 
                    new PreconditionsCheckerDialog(operacionEnCurso,
                    new JFrame(), true);
            preconditionsChecker.showDialog();
        }
    }
    
    public static void main (String[] args) { 
        modoEjecucion = ModoEjecucion.APLICACION;
        AppletOperation operation = new AppletOperation();
        String[] _args = {""};
        operation.setArgs(_args);
        logger.debug("operation: " + operation.toJSON());
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                        Applet appletFirma = new Applet();
                        appletFirma.start();                        
                        File jsonFile = File.createTempFile("signClaim", ".json");
                        jsonFile.deleteOnExit();
                        FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("testFiles/signClaim.json"), jsonFile);        
                        appletFirma.ejecutarOperacion(FileUtils.getStringFromFile(jsonFile));
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
            if(Applet.ModoEjecucion.APLICACION == 
                Applet.modoEjecucion){
                logger.debug(" ------ System.exit(0) ------ ");
                //System.exit(0);
            } else logger.debug("-- finalizando aplicación --- ");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
 
    @Override public void sendMessageToHost(OperationVS operacion) {
        if (operacion == null) {
            logger.debug(" - sendMessageToHost - Operacion null");
            return;
        }
        AppletOperation messageToHost = (AppletOperation)operacion;
        try {
            if(modoEjecucion == ModoEjecucion.APPLET) {
                String callbackFunction = "setMessageFromSignatureClient";
                if(messageToHost.getCallerCallback() != null) 
                    callbackFunction = messageToHost.getCallerCallback();        
                logger.debug(" - sendMessageToHost - status: " + 
                        messageToHost.getStatusCode() + " - operación: " + 
                        messageToHost.toJSON().toString() + " - callbackFunction: " + 
                        callbackFunction);          
                Object[] args = {messageToHost.toJSON().toString()};
                Object object = netscape.javascript.JSObject.getWindow(this).
                        call(callbackFunction, args);
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
        return operacionEnCurso; 
    }
}
