package org.votingsystem.admintool.util;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.votingsystem.admintool.dialog.FXMessageDialog;
import org.votingsystem.admintool.dialog.FXProgressDialog;
import org.votingsystem.admintool.dialog.PasswordDialog;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.HttpHelper;
import org.w3c.dom.Document;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class BrowserVS extends Region {

    private static Logger logger = Logger.getLogger(BrowserVS.class);

    private static final Map<String, ActorVS> actorMap = new HashMap<String, ActorVS>();

    private Stage stage;
    private HBox toolBar;
    private FXMessageDialog messageDialog;
    private FXProgressDialog progressDialog;
    private WebView webView;
    private WebView smallView;
    private ComboBox comboBox;
    private AtomicBoolean firstLoad = new AtomicBoolean(true);

    public BrowserVS() {
        Platform.setImplicitExit(false);
        //Note: Key is that Scene needs to be created and run on "FX user thread" NOT on the AWT-EventQueue Thread
        //https://gist.github.com/anjackson/1640654
        PlatformImpl.startup(new Runnable() {
            @Override public void run() {
                initComponents();
            }
        });
    }

    private void initComponents() {
        webView = new WebView();
        stage = new Stage();
        smallView = new WebView();
        comboBox = new ComboBox();
        stage.setTitle(ContextVS.getMessage("mainDialogCaption"));
        stage.setResizable(true);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                event.consume();
                stage.hide();
                logger.debug("stage.setOnCloseRequest");
            }
        });
        Scene scene = new Scene(BrowserVS.this, 900, 700, Color.web("#666970"));
        stage.setScene(scene);

        comboBox.setPrefWidth(400);
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().add(comboBox);
        toolBar.getChildren().add(createSpacer());
        smallView.setPrefSize(120, 80);

        //handle popup windows
        webView.getEngine().setCreatePopupHandler(
                new Callback<PopupFeatures, WebEngine>() {
                    @Override
                    public WebEngine call(PopupFeatures config) {
                        smallView.setFontScale(0.8);
                        if (!toolBar.getChildren().contains(smallView)) {
                            toolBar.getChildren().add(smallView);
                        }
                        return smallView.getEngine();
                    }
                }
        );

        //process history
        final WebHistory history = webView.getEngine().getHistory();
        history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>(){
            @Override
        public void onChanged(Change<? extends WebHistory.Entry> c) {
            c.next();
            for (WebHistory.Entry e : c.getRemoved()) {
                comboBox.getItems().remove(e.getUrl());
            }
            for (WebHistory.Entry e : c.getAddedSubList()) {
                comboBox.getItems().add(e.getUrl());
            }
            }
        });

        //set the behavior for the history combobox
        comboBox.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent ev) {
                int offset = comboBox.getSelectionModel().getSelectedIndex() - history.getCurrentIndex();
                history.go(offset);
            }
        });

        // process page loading
        webView.getEngine().getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> ov,
                                        Worker.State oldState, Worker.State newState) {
                        //logger.debug("newState: " + newState);
                        if (newState == Worker.State.SUCCEEDED) {
                            JSObject win = (JSObject) webView.getEngine().executeScript("window");
                            win.setMember("javafxClient", new JavafxClient());
                        }else if (newState.equals(Worker.State.FAILED)) {
                            showMessage(ContextVS.getMessage("conectionErrorMsg"));
                        }
                        if(newState.equals(Worker.State.FAILED) || newState.equals(Worker.State.SUCCEEDED)) {
                            if(firstLoad.get()) showProgressDialog(null, false);
                            firstLoad.set(false);
                        }
                    }
                }
        );
        getChildren().add(toolBar);
        getChildren().add(webView);
        getChildren().addListener(new ListChangeListener<Node>() {
            @Override public void onChanged(Change<? extends Node> c) {}
        });

        webView.getEngine().documentProperty().addListener(new ChangeListener<Document>() {
            @Override public void changed(ObservableValue<? extends Document> prop, Document oldDoc, Document newDoc) {
                enableFirebug( webView.getEngine());
            }
        });
    }

    /**
     * waiting until the WebView has loaded a document before trying to trigger Firebug.
     * http://stackoverflow.com/questions/17387981/javafx-webview-webengine-firebuglite-or-some-other-debugger
     * Enables Firebug Lite for debugging a webEngine.
     * @param engine the webEngine for which debugging is to be enabled.
     */
    private static void enableFirebug(final WebEngine engine) {
        engine.executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && " +
                "document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : " +
                "document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', " +
                "'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');" +
                "(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);" +
                "E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");
    }

    public void showMessage(final String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                if(messageDialog == null) messageDialog = new FXMessageDialog(BrowserVS.this.getScene().getWindow());
                messageDialog.showMessage(message);
            }
        });
    }

    public void showProgressDialog(final String message, final boolean isVisible) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                if(BrowserVS.this.getScene() == null) {
                    logger.debug("showProgressDialog scene null");
                    return;
                }
                if(progressDialog == null) progressDialog = new FXProgressDialog(BrowserVS.this.getScene().getWindow());
                progressDialog.show(message, isVisible);
            }
        });

    }

    public void loadURL(final String urlToLoad) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                webView.getEngine().load(urlToLoad);
                stage.show();
                firstLoad.set(true);
                showProgressDialog(null, true);
            }
        });
    }

    // JavaScript interface object
    public class JavafxClient {
        //public void exit() {Platform.exit();}
        public void setMessageToSignatureClient(String messageToSignatureClient) {
            logger.debug("JavafxClient - setMessageToSignatureClient: " + messageToSignatureClient);
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(messageToSignatureClient);
            final OperationVS operation = OperationVS.populate(jsonObject);
            try {
                ActorVS actorVS = actorMap.get(operation.getServerURL().trim());
                if(actorVS == null) {
                    showProgressDialog(ContextVS.getMessage("fetchingServerInfoMsg"), true);
                    String serverInfoURL = ActorVS.getServerInfoURL(operation.getServerURL());
                    ResponseVS responseVS = HttpHelper.getInstance().getData(serverInfoURL, ContentTypeVS.JSON);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        jsonObject = (JSONObject)JSONSerializer.toJSON(responseVS.getMessage());
                        actorVS = ActorVS.populate(jsonObject);
                        responseVS.setData(actorVS);
                        logger.error("checkActorVS - adding " + operation.getServerURL().trim() + " to actor map");
                        actorMap.put(operation.getServerURL().trim(), actorVS);
                        switch(actorVS.getType()) {
                            case ACCESS_CONTROL:
                                ContextVS.getInstance().setAccessControl((AccessControlVS) actorVS);
                                break;
                            case VICKETS:
                                ContextVS.getInstance().setVicketServer((VicketServer) actorVS);
                                ContextVS.getInstance().setTimeStampServerCert(actorVS.getTimeStampCert());
                                break;
                            case CONTROL_CENTER:
                                ContextVS.getInstance().setControlCenter((ControlCenterVS) actorVS);
                                break;
                            default:
                                logger.debug("Unprocessed actor:" + actorVS.getType());
                        }
                    } else if(ResponseVS.SC_NOT_FOUND == responseVS.getStatusCode()) {

                    }
                }
                showProgressDialog(null, false);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            showProgressDialog(operation.getSignedMessageSubject(), true);
            /*switch(operation.getType()) {
                case VICKET_GROUP_NEW:
                    break;
                case VICKET_GROUP_SUBSCRIBE:
                    break;
                default:
                    logger.debug("Unprocessed operation type: " + operation.getType());
            }*/
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        PasswordDialog passwordDialog = new PasswordDialog (new JFrame(), true);
                        passwordDialog.setVisible(true);
                        String password = passwordDialog.getPassword();
                        if (password != null) processOperation(password, operation);
                    } catch(final Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        BrowserVS.this.showMessage(ContextVS.getMessage("errorLbl") + " - " + ex.getMessage());
                    } finally {
                        BrowserVS.this.showProgressDialog(null, false);
                    }
                }
            });
        }
    }


    private void processOperation(String password, OperationVS operation) throws Exception {
        JSONObject documentToSignJSON = (JSONObject)JSONSerializer.toJSON(operation.getDocumentToSignMap());
        SMIMEMessageWrapper smimeMessage = SMIMEContentSigner.genMimeMessage(null,
                operation.getNormalizedReceiverName(), documentToSignJSON.toString(),
                password.toCharArray(), operation.getSignedMessageSubject(), null);
        SMIMESignedSender senderWorker = new SMIMESignedSender(smimeMessage, operation.getServiceURL(),
                ContextVS.getInstance().getVicketServer().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED, null,
                ContextVS.getInstance().getVicketServer().getX509Certificate());
        ResponseVS responseVS =  senderWorker.call();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            showMessage(responseVS.getMessage());
        } else {
            showMessage(ContextVS.getMessage("errorLbl") + " - " + responseVS.getMessage());
        }
    }

    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    @Override protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        double tbHeight = toolBar.prefHeight(w);
        layoutInArea(webView,0,0,w,h-tbHeight,0, HPos.CENTER, VPos.CENTER);
        layoutInArea(toolBar,0,h-tbHeight,w,tbHeight,0,HPos.CENTER,VPos.CENTER);
    }

    @Override protected double computePrefWidth(double height) {
        return 750;
    }

    @Override protected double computePrefHeight(double width) {
        return 600;
    }


}