package org.votingsystem.vicket.util;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.web.WebView;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import java.io.File;
import java.util.Base64;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * JavaFX needs to be initialized
 * http://stackoverflow.com/questions/20370888/generating-image-at-server-side-using-java-fx
 * http://stackoverflow.com/questions/11273773/javafx-2-1-toolkit-not-initialized
 */
public class WebViewWrapper {

    private static Logger logger = Logger.getLogger(WebViewWrapper.class);

    private WebView webView = null;
    private JFXPanel jfxPanel = null;
    private static WebViewWrapper instance;

    public static WebViewWrapper getInstance() {
        if(instance == null) {
            try {
                instance = new WebViewWrapper();
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return instance;
    }

    private WebViewWrapper() throws ExecutionException, InterruptedException {
        final FutureTask future = new FutureTask(new Callable() {
            @Override public Object call() throws Exception {
                jfxPanel = new JFXPanel(); // initializes JavaFX environment
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        webView = new WebView();
                        webView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
                        webView.getEngine().getLoadWorker().stateProperty().addListener(
                            new ChangeListener<Worker.State>() {
                                Document document;
                                @Override public void changed(ObservableValue<? extends Worker.State> ov,
                                                              Worker.State oldState, Worker.State newState) {
                                    //logger.debug("newState: " + newState + " - " + webView.getEngine().getLocation());
                                    if (newState == Worker.State.SUCCEEDED) {
                                        Document doc = webView.getEngine().getDocument();
                                        Element element = doc.getElementById("voting_system_page");
                                        if (element != null) {
                                            JSObject win = (JSObject) webView.getEngine().executeScript("window");
                                            win.setMember("clientTool", new JavafxClient());
                                            String jsCommand = "fireCoreSignal('" +
                                                    Base64.getEncoder().encodeToString("{}".getBytes()) + "')";
                                            webView.getEngine().executeScript(jsCommand);
                                        }
                                    } else if (newState.equals(Worker.State.FAILED)) {
                                        logger.error("Worker.State.FAILED");
                                    } else if (newState.equals(Worker.State.SCHEDULED)) {
                                    }
                                    if (newState.equals(Worker.State.FAILED) || newState.equals(Worker.State.SUCCEEDED)) {
                                    }
                                }
                            }
                        );
                    }
                });
                return "OK";
            }
        });
        SwingUtilities.invokeLater(future);
        logger.debug(future.get());
    }

    public void  loadWebView(final String urlToLoad) {
        logger.debug("loadWebView - urlToLoad: " + urlToLoad);
        Platform.runLater(new Runnable() {
            @Override public void run() {
                webView.getEngine().load(urlToLoad);
            }
        });
    }

    public void executeScript (final String jsCommand) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                webView.getEngine().executeScript(jsCommand);
            }
        });

    }

    public void sendMessageToBrowserApp(JSONObject messageJSON, String callerCallback) {
        logger.debug("sendMessageToBrowserApp - messageJSON: " + messageJSON.toString());
        try {
            final String jsCommand = "setClientToolMessage('" + callerCallback + "','" +
                    new String(Base64.getEncoder().encode(messageJSON.toString().getBytes("UTF8")), "UTF8") + "')";
            logger.debug("sendMessageToBrowserApp - jsCommand: " + jsCommand);
            PlatformImpl.runLater(new Runnable() {
                @Override public void run() {
                    webView.getEngine().executeScript(jsCommand);
                }
            });
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    // JavaScript interface object
    public class JavafxClient {
        public void setJSONMessageToSignatureClient(String messageToSignatureClient) {
            logger.debug("JavafxClient.setJSONMessageToSignatureClient: " + messageToSignatureClient);
            try {
                JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(messageToSignatureClient);
                logger.debug("JavafxClient - TODO - process message");
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

}
