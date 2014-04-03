package org.votingsystem.applet.votingtool;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.applet.votingtool.dialog.PreconditionsCheckerDialog;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.*;
import org.votingsystem.util.FileUtils;
import javax.swing.*;
import java.applet.Applet;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.votingsystem.applet.votingtool.panel.AboutPanel;
import org.votingsystem.util.NifUtils;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingApplet extends JApplet implements AppHostVS {
    
    private static Logger logger = Logger.getLogger(VotingApplet.class);

    private static enum ExecutionMode {APPLET, APPLICATION}

    private Timer operationGetter;
    public static String locale = "es";
    public static ExecutionMode executionMode = ExecutionMode.APPLET;

    public VotingApplet() { }

    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.debug("ShutdownHook - ShutdownHook - ShutdownHook");
                terminate();
        }});
        //Execute a job on the event-dispatching thread:
        //creating this applet's GUI.
        try {
            ContextVS.initSignatureApplet(this, "log4jVotingTool.properties", "votingToolMessages.properties", locale);
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                        AboutPanel aboutPanel = new AboutPanel();
                        getContentPane().add(aboutPanel);
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
        logger.debug("start - java version: " + System.getProperty("java.version"));
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
        if(executionMode == ExecutionMode.APPLET) {
            initOperationGetter();
            if(getParameter("locale") != null) locale = getParameter("locale");
        }
        init();
        OperationVS operation = new OperationVS(ResponseVS.SC_PROCESSING,
                ContextVS.getInstance().getMessage("appletInitMsg"));
        sendMessageToHost(operation);
    }

    public void stop() {
        logger.debug("stop");
        terminate();
    }

    public void destroy() {
        logger.debug("destroy");
        terminate();
    }

    /*
     * Timer that checks pending operations on web client app
     */
    private void initOperationGetter() {
        operationGetter =  new Timer(true);
        try {
            //Class JSObjectClass = netscape.javascript.JSObject.class;
            Class JSObjectClass = Class.forName("netscape.javascript.JSObject");
            final Method method = JSObjectClass.getMethod("getWindow", this.getClass());
            operationGetter.scheduleAtFixedRate(
                    new TimerTask(){
                        public void run() {
                            //logger.debug("Comprobando operaciones pendientes");
                            try {
                                Object object = method.invoke(null, "getMessageToSignatureClient", null);
                                if(object != null) {
                                    runOperation(object.toString());
                                } else {
                                    //logger.debug("Testeando JSObject - responseVS nula");
                                }
                            } catch(Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    }, 0, 1000);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

    }


    public void terminate() {
        logger.debug(" --- terminate ---");
        if(operationGetter != null) {
            operationGetter.cancel();
            sendMessageToHost(new OperationVS(TypeVS.TERMINATED));
            ContextVS.getInstance().shutdown();
        }
    }

    public void runOperation(String operationJSONStr) {
        logger.debug("runOperation: " + operationJSONStr);
        if(operationJSONStr == null || operationJSONStr.trim().isEmpty()) return;
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(operationJSONStr);
        OperationVS runningOperation = OperationVS.populate(jsonObject);
        if(runningOperation.getType() == null) {
            logger.error("runOperation - missin operation type");
            runningOperation.setMessage("missin operation type");
            runningOperation.setStatusCode(ResponseVS.SC_ERROR_REQUEST);
            sendMessageToHost(runningOperation);
        } else {
            PreconditionsCheckerDialog preconditionsChecker = new PreconditionsCheckerDialog(new JFrame(), true);
            preconditionsChecker.checkOperation(runningOperation);
        }
    }

    public static void main (final String[] args) {
        executionMode = ExecutionMode.APPLICATION;
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                        VotingApplet votingAppletFirma = new VotingApplet();
                        votingAppletFirma.start();
                        String operationStr = null;
                        if(args.length > 0) {
                            operationStr = args[0];
                        } else {
                            File jsonFile = File.createTempFile("publishVoting", ".json");
                            jsonFile.deleteOnExit();
                            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                                    .getResourceAsStream("testFiles/data.json"), jsonFile);
                            votingAppletFirma.runOperation(FileUtils.getStringFromFile(jsonFile));
                        }
                        if(operationStr != null) votingAppletFirma.runOperation(operationStr);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
            if(ExecutionMode.APPLICATION == VotingApplet.executionMode){
                logger.debug(" ------ System.exit(0) ------ ");
                //System.exit(0);
            } else logger.debug("-- exiting App --- ");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void sendMessageToHost(OperationVS messageToHost) {
        if (messageToHost == null) logger.debug(" - sendMessageToHost - Operacion null");
        else {
            JSONObject messageJSON = (JSONObject)JSONSerializer.toJSON(messageToHost.getDataMap());
            try {
                if(executionMode == ExecutionMode.APPLET) {
                    String callbackFunction = "setMessageFromSignatureClient";
                    if(messageToHost.getCallerCallback() != null) callbackFunction = messageToHost.getCallerCallback();
                    logger.debug(" - sendMessageToHost - status: " + messageToHost.getStatusCode() +
                            " - callbackFunction: " + callbackFunction + " - message: " + messageJSON.toString());
                    Object[] args = {messageJSON.toString()};
                    //Class JSObjectClass = netscape.javascript.JSObject.class;
                    Class JSObjectClass = Class.forName("netscape.javascript.JSObject");
                    final Method method = JSObjectClass.getMethod("getWindow", this.getClass());
                    Object object = method.invoke(null, callbackFunction, args);
                } else logger.debug("---> APP EXECUTION MODE: " + executionMode.toString());
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            if(ExecutionMode.APPLICATION == executionMode && messageToHost.getStatusCode() == ResponseVS.SC_CANCELLED){
                logger.debug(" ------  System.exit(0) ------ ");
                System.exit(0);
            }
        }
    }

}
