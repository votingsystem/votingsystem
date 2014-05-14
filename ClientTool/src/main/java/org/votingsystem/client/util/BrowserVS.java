package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.FXMessageDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.model.*;
import org.votingsystem.util.HttpHelper;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class BrowserVS extends Region {

    private static Logger logger = Logger.getLogger(BrowserVS.class);

    private final SignatureService signatureService = new SignatureService();

    private Stage browserStage;
    private HBox toolBar;
    private FXMessageDialog messageDialog;
    private WebView webView;
    private WebView smallView;
    private ComboBox comboBox;
    private AtomicInteger offset = new AtomicInteger(0);

    public BrowserVS() {
        Platform.setImplicitExit(false);
        signatureService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                logger.debug("signatureService - OnSucceeded");
            ResponseVS responseVS = signatureService.getValue();
            sendMessageToBrowserApp(responseVS.getStatusCode(), responseVS.getMessage(),
                    signatureService.getOperationVS().getCallerCallback());
            }
        });

        signatureService.setOnRunning(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                logger.debug("signatureService - OnRunning");
            }
        });

        signatureService.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                logger.debug("signatureService - OnFailed");
            }
        });

        //Note: Key is that Scene needs to be created and run on "FX user thread" NOT on the AWT-EventQueue Thread
        //https://gist.github.com/anjackson/1640654
        PlatformImpl.startup(new Runnable() {
            @Override
            public void run() {
                initComponents();
            }
        });
    }

    private void initComponents() {
        Region progressRegion = new Region();
        progressRegion.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        progressRegion.setPrefSize(240, 160);

        VBox progressBox = new VBox();
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPrefWidth(400);
        progressBox.setPrefHeight(300);

        Text progressMessageText = new Text();
        progressMessageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #f9f9f9;");
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(10);
        progressMessageText.textProperty().bind(signatureService.messageProperty());
        progressBar.progressProperty().bind(signatureService.progressProperty());
        progressRegion.visibleProperty().bind(signatureService.runningProperty());
        progressBox.visibleProperty().bind(signatureService.runningProperty());
        progressBox.getChildren().addAll(progressMessageText, progressBar);

        webView = new WebView();
        final WebHistory history = webView.getEngine().getHistory();
        smallView = new WebView();
        comboBox = new ComboBox();
        browserStage = new Stage();
        browserStage.initModality(Modality.WINDOW_MODAL);
        browserStage.setTitle(ContextVS.getMessage("mainDialogCaption"));
        browserStage.setResizable(true);
        browserStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                event.consume();
                browserStage.hide();
                logger.debug("browserStage.setOnCloseRequest");
            }
        });

        VBox verticalBox = new VBox();
        VBox.setVgrow(webView, Priority.ALWAYS);

        final Button forwardButton = new Button();
        final Button prevButton = new Button();

        forwardButton.setGraphic(new ImageView(FXUtils.getImage(this, "fa-chevron-right")));
        forwardButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override public void handle(javafx.event.ActionEvent ev) {
                try {
                    history.go(1);
                    prevButton.setDisable(false);
                } catch(Exception ex) {
                    forwardButton.setDisable(true);
                }
            }
        });

        prevButton.setGraphic(new ImageView(FXUtils.getImage(this, "fa-chevron-left")));
        prevButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override public void handle(javafx.event.ActionEvent ev) {
                try {
                    history.go(-1);
                    forwardButton.setDisable(false);
                } catch(Exception ex) {
                    prevButton.setDisable(true);
                }
            }
        });

        prevButton.setDisable(true);
        forwardButton.setDisable(true);

        final TextField urlInputText = new TextField("");
        urlInputText.setPrefWidth(400);
        urlInputText.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent ke) {
                if (ke.getCode().equals(KeyCode.ENTER)) {
                    if(!"".equals(urlInputText.getText())) {
                        String targetURL = null;
                        if(urlInputText.getText().startsWith("http://")) {
                            targetURL = urlInputText.getText().trim();
                        } else targetURL = "http://" + urlInputText.getText().trim();
                        loadURL(targetURL);
                    }
                }
            }
        });


        comboBox.setPrefWidth(300);
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().addAll(prevButton, forwardButton, urlInputText, comboBox , createSpacer());

        //handle popup windows
        webView.getEngine().setCreatePopupHandler(
                new Callback<PopupFeatures, WebEngine>() {
                    @Override
                    public WebEngine call(PopupFeatures config) {
                        //smallView.setFontScale(0.8);
                        openPopUp(smallView);
                        return smallView.getEngine();
                    }
                }
        );

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
                if(history.getCurrentIndex() > 0) prevButton.setDisable(false);
                logger.debug("== currentIndex= " + history.getCurrentIndex() + " - num. entries: " + history.getEntries().size());
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
                            win.setMember("clientTool", new JavafxClient());
                        }else if (newState.equals(Worker.State.FAILED)) {
                            showMessage(ContextVS.getMessage("conectionErrorMsg"));
                        }
                        if(newState.equals(Worker.State.FAILED) || newState.equals(Worker.State.SUCCEEDED)) {
                        }
                    }
                }
        );
        verticalBox.getChildren().addAll(toolBar, webView);
        StackPane stack = new StackPane();
        stack.getChildren().addAll(verticalBox, progressRegion, progressBox);
        //stack.setPrefWidth(1000);
        //stack.setPrefHeight(1000);
        Scene scene = new Scene(stack, Color.web("#666970"));
        browserStage.setScene(scene);
        browserStage.setWidth(1000);
        browserStage.setHeight(1000);

        getChildren().addListener(new ListChangeListener<Node>() {
            @Override public void onChanged(Change<? extends Node> c) {}
        });

        webView.getEngine().documentProperty().addListener(new ChangeListener<Document>() {
            @Override public void changed(ObservableValue<? extends Document> prop, Document oldDoc, Document newDoc) {
                if(ContextVS.getInstance().getBoolProperty(ContextVS.IS_DEBUG_ENABLED, false)) enableFirebug( webView.getEngine());
            }
        });
    }

    public void sendMessageToBrowserApp(int statusCode, String message, String callbackFunction) {
        logger.debug("sendMessageToBrowserApp - statusCode: " + statusCode + " - message: " + message);
        Map resultMap = new HashMap();
        resultMap.put("statusCode", statusCode);
        resultMap.put("message", message);
        JSONObject messageJSON = (JSONObject)JSONSerializer.toJSON(resultMap);
        if(callbackFunction == null) callbackFunction = "setMessageFromSignatureClient";
        String jsCommand = callbackFunction + "(" + messageJSON.toString() + ")";
        logger.debug("sendMessageToBrowserApp - jsCommand: " + jsCommand);
        webView.getEngine().executeScript(jsCommand);
    }

    private void openPopUp(WebView popUpWebView) {
        //popUpWebView.setPrefSize(400, 400);
        final Stage stage = new Stage();
        //create root node of scene
        Group rootGroup = new Group();
        Scene scene = new Scene(rootGroup, 800, 600, Color.WHITESMOKE);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
        rootGroup.getChildren().add(popUpWebView);
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


    public void showMessage(ResponseVS responseVS) {
        String finalMsg = responseVS.getMessage() == null? "":responseVS.getMessage();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) finalMsg = responseVS.getMessage();
        else finalMsg = ContextVS.getMessage("errorLbl") + " - " + responseVS.getMessage();
        showMessage(finalMsg);
    }

    public void showMessage(final String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                if(messageDialog == null) messageDialog = new FXMessageDialog();
                messageDialog.showMessage(message);
            }
        });
    }


    public void loadURL(final String urlToLoad) {
        logger.debug("loadURL: " + urlToLoad);
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                webView.getEngine().load(urlToLoad);
                browserStage.show();
            }
        });
    }

    // JavaScript interface object
    public class JavafxClient {

        public void setMessageToSignatureClient(String messageToSignatureClient) {
            logger.debug("JavafxClient - setMessageToSignatureClient: " + messageToSignatureClient);
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(messageToSignatureClient);
            signatureService.processOperationVS(OperationVS.populate(jsonObject));
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

    @Override protected double computeMinWidth(double height) {
        return 1000;
    }

    @Override protected double computeMinHeight(double width) {
        return 1000;
    }

}