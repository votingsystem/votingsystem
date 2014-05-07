package org.votingsystem.applet.validationtool;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.dialog.MainDialog;
import org.votingsystem.applet.validationtool.panel.AboutPanel;
import org.votingsystem.model.AppHostVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;

import javax.swing.*;
import java.applet.Applet;
import java.lang.reflect.Method;
import java.security.Security;
import java.util.Timer;
import java.util.TimerTask;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ValidationApplet extends JApplet implements AppHostVS {
    
    private static Logger logger = Logger.getLogger(ValidationApplet.class);
    
    public static enum ExecutionMode {APPLET, APLICACION}

    private String locale = "es";
    private Timer operationGetter;
    public static ExecutionMode executionMode = ExecutionMode.APPLET;

    public ValidationApplet() { }

    @Override  public void init() {
        //Execute a job on the event-dispatching thread:
        //creating this org.votingsystem.applet's GUI.
        try {
            ContextVS.init(this, "log4jValidationTool.properties", "validationToolMessages.properties", locale);
            logger.debug("init");
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
                    MainDialog mainDialog = new MainDialog(new JFrame(), false);
                    mainDialog.setVisible(true);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        if(executionMode == ExecutionMode.APPLET) {
            initOperationGetter();
            if(getParameter("locale") != null) locale = getParameter("locale");
        }
        OperationVS operation = new OperationVS(ResponseVS.SC_PROCESSING);
        operation.setMessage(ContextVS.getInstance().getMessage("appletInitialized"));
        sendMessageToHost(operation);
    }

    public void stop() {
        logger.debug("stop");
    }

    public void destroy() {
        logger.debug("destroy");
    }

    public static void main (String[] args) {
        executionMode = ExecutionMode.APLICACION;
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        ValidationApplet validationApplet = new ValidationApplet();
                        validationApplet.start();
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
        try {
            Class JSObjectClass = Class.forName("netscape.javascript.JSObject");
            final Method staticInitializerMethod = JSObjectClass.getMethod("getWindow", Applet.class);
            final Object jsObject = staticInitializerMethod.invoke(null, this);
            final Method javascriptCallMethod = JSObjectClass.getMethod("call", String.class, java.lang.Object[].class);
            operationGetter.scheduleAtFixedRate(
                    new TimerTask(){
                        public void run() {
                            try {
                                Object object = javascriptCallMethod.invoke(jsObject, "getMessageToValidationTool", null);
                                if(object != null) {
                                    runOperation(object.toString());
                                } else { }
                            } catch(Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    }, 0, 1000);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }


    public void runOperation(String operationJSONStr) {
        logger.debug("runOperation: " + operationJSONStr);
        //if(operationJSONStr == null || operationJSONStr.trim().isEmpty()) return;
        //JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(operationJSONStr);
        //OperationVS runningOperation = OperationVS.populate(jsonObject);
        MainDialog mainDialog = new MainDialog(new JFrame(), false);
        mainDialog.setVisible(true);
    }

    @Override public void sendMessageToHost(OperationVS messageToHost) {
        if (messageToHost == null) logger.debug(" - sendMessageToHost - Operacion null");
        else {
            JSONObject messageJSON = (JSONObject)JSONSerializer.toJSON(messageToHost.getDataMap());
            logger.debug(" - sendMessageToHost - status: " +  messageToHost.getStatusCode() +
                    " - message: " + messageJSON.toString());
            try {
                if(executionMode == ExecutionMode.APPLET) {
                    Object[] args = {messageJSON.toString()};
                    Class JSObjectClass = Class.forName("netscape.javascript.JSObject");
                    Method staticInitializerMethod = JSObjectClass.getMethod("getWindow", Applet.class);
                    Object jsObject = staticInitializerMethod.invoke(null, this);
                    Method javascriptCallMethod = JSObjectClass.getMethod("call", String.class, java.lang.Object[].class);
                    Object object = javascriptCallMethod.invoke(jsObject, "setMessageFromValidationTool", args);
                } else logger.debug("---> APP EXECUTION MODE: " + executionMode.toString());
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            if(ExecutionMode.APLICACION == executionMode && messageToHost.getStatusCode() == ResponseVS.SC_CANCELLED){
                logger.debug(" ------  System.exit(0) ------ ");
                System.exit(0);
            }
        }
    }

}