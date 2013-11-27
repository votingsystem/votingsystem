package org.votingsystem.applet.validationtool;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import netscape.javascript.*;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.dialog.MainDialog;
import org.votingsystem.applet.validationtool.panel.AboutPanel;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.*;
import javax.swing.*;
import java.security.Security;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Applet extends JApplet implements AppHostVS {
    
    private static Logger logger = Logger.getLogger(Applet.class);
    
    public static enum ExecutionMode {APPLET, APLICACION}

    private String locale = "es";
    private Timer operationGetter;
    public static ExecutionMode executionMode = ExecutionMode.APPLET;

    public Applet() { }

    @Override  public void init() {
        logger.debug("init");
        //Execute a job on the event-dispatching thread:
        //creating this applet's GUI.
        try {
            ContextVS.init(this, "log4jValidationTool.properties", "validationToolMessages_", locale);
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                        AboutPanel aboutPanel = new AboutPanel();
                        getContentPane().add(aboutPanel);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void start() {
        logger.debug("start");
        if(executionMode == ExecutionMode.APPLET) {
            if(getParameter("locale") != null) locale = getParameter("locale");
        }
        init();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    if(executionMode != ExecutionMode.APPLET) {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    }
                    MainDialog dialogo = new MainDialog(new JFrame(), false);
                    dialogo.setVisible(true);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        if(executionMode == ExecutionMode.APPLET) {
            initOperationGetter();
            if(getParameter("locale") != null) locale = getParameter("locale");
        }
        OperationVS operacion = new OperationVS(ResponseVS.SC_PROCESSING);
        operacion.setMessage(ContextVS.getInstance().getMessage("appletInitialized"));
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
        executionMode = ExecutionMode.APLICACION;
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

    private void initOperationGetter() {
        logger.info("initOperationGetter");
        operationGetter =  new Timer(true);
        final netscape.javascript.JSObject jsObject = netscape.javascript.JSObject.getWindow(this);
        operationGetter.scheduleAtFixedRate(
            new TimerTask(){
                public void run() {
                    Object object = jsObject.call(
                            "getMessageToValidationTool", null);
                    if(object != null) {
                        runOperation(object.toString());
                    } else { }
                }
            }, 0, 1000);
    }


    public void runOperation(String operacionJSONStr) {
        logger.debug("runOperation: " + operacionJSONStr);
        //if(operacionJSONStr == null || "".equals(operacionJSONStr)) return;
        //JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(operacionJSONStr);
        //OperationVS runningOperation = OperationVS.populate(jsonObject);
        MainDialog dialogo = new MainDialog(new JFrame(), false);
        dialogo.setVisible(true);
    }

    @Override public void sendMessageToHost(OperationVS operation) {
        if (operation == null) {
            logger.debug(" - sendMessageToHost - Operacion null");
            return;
        }
        OperationVS messageToHost = (OperationVS)operation;
        Map appletOperationDataMap = ((OperationVS)operation).getDataMap();
        JSONObject messageJSON = (JSONObject)JSONSerializer.toJSON(appletOperationDataMap);
        logger.debug(" - sendMessageToHost - status: " +  messageToHost.getStatusCode() +
                " - operación: " + messageJSON.toString());
        try {
            if(executionMode == ExecutionMode.APPLET) {
                Object[] args = {messageJSON.toString()};
                JSObject jsObject = null;
                Object object = netscape.javascript.JSObject.getWindow(this).
                        call("setMessageFromValidationTool", args);
            } else logger.debug("---> APP EXECUTION MODE: " + executionMode.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if(ExecutionMode.APLICACION == executionMode &&
                messageToHost.getStatusCode() == ResponseVS.SC_CANCELLED){
            logger.debug(" ------  System.exit(0) ------ ");
            System.exit(0);
        }
    }

}