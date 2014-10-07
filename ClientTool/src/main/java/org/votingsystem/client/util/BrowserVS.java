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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.pane.BrowserVSPane;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVS extends Region {

    private static Logger logger = Logger.getLogger(BrowserVS.class);

    private Stage browserStage;
    private HBox toolBar;
    private MessageDialog messageDialog;
    private WebView webView;
    private VBox mainVBox;
    private TextField locationField = new TextField("");
    private final BrowserVSPane browserHelper;
    private String caption;

    public BrowserVS() {
        this(new WebView());
    }

    private BrowserVS(WebView webView) {
        browserHelper = new BrowserVSPane();
        this.webView = webView;
        this.webView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        Platform.setImplicitExit(false);
        browserHelper.getSignatureService().setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                logger.debug("signatureService - OnSucceeded");
                PlatformImpl.runLater(new Runnable() {
                    @Override public void run() {
                        ResponseVS responseVS = browserHelper.getSignatureService().getValue();
                        if(ResponseVS.SC_INITIALIZED == responseVS.getStatusCode()) {
                            logger.debug("signatureService - OnSucceeded - ResponseVS.SC_INITIALIZED");
                        } else if(ContentTypeVS.JSON == responseVS.getContentType()) {
                            sendMessageToBrowserApp(responseVS.getMessageJSON(),
                                    browserHelper.getSignatureService().getOperationVS().getCallerCallback());
                        } else sendMessageToBrowserApp(responseVS.getStatusCode(), responseVS.getMessage(),
                                browserHelper.getSignatureService().getOperationVS().getCallerCallback());
                    }
                });
            }
        });

        browserHelper.getSignatureService().setOnRunning(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                logger.debug("signatureService - OnRunning");
            }
        });

        browserHelper.getSignatureService().setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                logger.debug("signatureService - OnFailed");
            }
        });
        PlatformImpl.startup(new Runnable() {
            @Override
            public void run() {
                initComponents();
            }
        });
    }

    private void initComponents() {
        final WebHistory history = webView.getEngine().getHistory();
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

        mainVBox = new VBox();
        VBox.setVgrow(webView, Priority.ALWAYS);

        final Button forwardButton = new Button();
        final Button prevButton = new Button();
        final Button reloadButton = new Button();

        forwardButton.setGraphic(new ImageView(Utils.getImage(this, "fa-chevron-right")));
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

        prevButton.setGraphic(new ImageView(Utils.getImage(this, "fa-chevron-left")));
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

        reloadButton.setGraphic(new ImageView(Utils.getImage(this, "fa-refresh")));
        reloadButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override public void handle(javafx.event.ActionEvent ev) {
                webView.getEngine().reload();
            }
        });

        prevButton.setDisable(true);
        forwardButton.setDisable(true);

        locationField.setPrefWidth(400);
        HBox.setHgrow(locationField, Priority.ALWAYS);
        locationField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent ke) {
                if (ke.getCode().equals(KeyCode.ENTER)) {
                    if(!"".equals(locationField.getText())) {
                        String targetURL = null;
                        if(locationField.getText().startsWith("http://") || locationField.getText().startsWith("https://")) {
                            targetURL = locationField.getText().trim();
                        } else targetURL = "http://" + locationField.getText().trim();
                        loadURL(targetURL, null);
                    }
                }
            }
        });

        webView.getEngine().locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                locationField.setText(newValue);
            }
        });

        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().addAll(prevButton, forwardButton, locationField, reloadButton , createSpacer());

        //handle popup windows
        webView.getEngine().setCreatePopupHandler(
                new Callback<PopupFeatures, WebEngine>() {
                    @Override
                    public WebEngine call(PopupFeatures config) {
                        WebView smallView = new WebView();
                        //smallView.setFontScale(0.8);
                        new BrowserVS(smallView).show(700, 700, false);
                        return smallView.getEngine();
                    }
                }
        );


        history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>(){
        @Override public void onChanged(Change<? extends WebHistory.Entry> c) {
                c.next();
                if(history.getCurrentIndex() > 0) prevButton.setDisable(false);
                //logger.debug("==== currentIndex: " + history.getCurrentIndex() + " - num. entries: " + history.getEntries().size());
                String params = "";
                if(locationField.getText().contains("?")) {
                    params = locationField.getText().substring(locationField.getText().indexOf("?"),
                            locationField.getText().length());
                }
                String newURL = history.getEntries().get(history.getEntries().size() - 1).getUrl();
                if(!newURL.contains("?")) newURL = newURL + params;
                locationField.setText(newURL);
            }
        });

        // process page loading
        webView.getEngine().getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    Document document;
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> ov,
                                        Worker.State oldState, Worker.State newState) {
                        //logger.debug("newState: " + newState + " - " + webView.getEngine().getLocation());
                        if (newState == Worker.State.SUCCEEDED) {
                            Document doc = webView.getEngine().getDocument();
                            Element element = doc.getElementById("voting_system_page");
                            if(element != null) {
                                JSObject win = (JSObject) webView.getEngine().executeScript("window");
                                win.setMember("clientTool", new JavafxClient());
                                webView.getEngine().executeScript("notifiyClientToolConnection()");
                            }
                        } else if (newState.equals(Worker.State.FAILED)) {
                            showMessage(ContextVS.getMessage("connectionErrorMsg"));
                        } else if (newState.equals(Worker.State.SCHEDULED)) { }
                        if(newState.equals(Worker.State.FAILED) || newState.equals(Worker.State.SUCCEEDED)) {


                        }
                    }
                }
        );
        mainVBox.getChildren().addAll(toolBar, webView);
        browserHelper.getChildren().add(0, mainVBox);

        Scene scene = new Scene(browserHelper, Color.web("#666970"));
        browserStage.setScene(scene);
        browserStage.setWidth(1050);
        browserStage.setHeight(1000);

        getChildren().addListener(new ListChangeListener<Node>() {
            @Override public void onChanged(Change<? extends Node> c) {}
        });

    }

    public void sendMessageToBrowserApp(int statusCode, String message, String callerCallback) {
        logger.debug("sendMessageToBrowserApp - statusCode: " + statusCode + " - message: " + message);
        Map resultMap = new HashMap();
        resultMap.put("statusCode", statusCode);
        resultMap.put("message", message);
        try {
            JSONObject messageJSON = (JSONObject)JSONSerializer.toJSON(resultMap);
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

    public void showMessage(ResponseVS responseVS) {
        String finalMsg = responseVS.getMessage() == null? "":responseVS.getMessage();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) finalMsg = responseVS.getMessage();
        else finalMsg = ContextVS.getMessage("errorLbl") + " - " + responseVS.getMessage();
        showMessage(finalMsg);
    }

    public void showMessage(final String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                if(messageDialog == null) messageDialog = new MessageDialog();
                messageDialog.showMessage(message);
            }
        });
    }


    public void loadURL(final String urlToLoad, String caption) {
        final StringBuilder browserCaption = new StringBuilder();
        if(caption == null && this.caption != null) browserCaption.append(this.caption);
        else if(caption != null) browserCaption.append(caption);
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                webView.getEngine().load(urlToLoad);
                if(browserCaption != null) browserStage.setTitle(browserCaption.toString());
                browserStage.show();
            }
        });
    }

    public void loadURL(final String urlToLoad, String callback, String callbackMsg, final String caption,
            final boolean isToolbarVisible) {
        logger.debug("loadURL: " + urlToLoad);
        final StringBuilder jsCommand = new StringBuilder();
        if(callback != null && callbackMsg != null) jsCommand.append(callback + "(" + callbackMsg + ")");
        else if(callback != null) jsCommand.append(callback + "()");;
        logger.debug("jsCommand: " + jsCommand.toString());
        if(!"".equals(jsCommand.toString())) {
            webView.getEngine().getLoadWorker().stateProperty().addListener(
                    new ChangeListener<Worker.State>() {
                        @Override
                        public void changed(ObservableValue<? extends Worker.State> ov,
                                            Worker.State oldState, Worker.State newState) {
                            //logger.debug("newState: " + newState);
                            if (newState == Worker.State.SUCCEEDED) {
                                webView.getEngine().executeScript(jsCommand.toString());
                            }
                        }
                    }
            );
        }
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                if(!isToolbarVisible) mainVBox.getChildren().removeAll(toolBar);
                webView.getEngine().load(urlToLoad);
                if(caption != null) browserStage.setTitle(caption);
                browserStage.show();
            }
        });
    }

    public void executeScript (String jsCommand) {
        webView.getEngine().executeScript(jsCommand);
    }

    public void loadBackgroundURL(final String urlToLoad) {
        logger.debug("loadBackgroundURL: " + urlToLoad);
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                webView.getEngine().load(urlToLoad);
            }
        });
    }

    private void show(final int width, final int height, final boolean isToolbarVisible) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                browserStage.setWidth(width);
                browserStage.setHeight(height);
                if(!isToolbarVisible) mainVBox.getChildren().removeAll(toolBar);
                browserStage.show();
            }
        });
    }

    // JavaScript interface object
    public class JavafxClient {

        public void setJSONMessageToSignatureClient(String messageToSignatureClient) {
            try {
                String jsonStr = new String(Base64.getDecoder().decode(messageToSignatureClient.getBytes()),"UTF-8");
                String logMsg = messageToSignatureClient.length() > 350 ? messageToSignatureClient.substring(0, 350) +
                        "..." : messageToSignatureClient;
                logger.debug("JavafxClient.setJSONMessageToSignatureClient: " + logMsg);
                JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(jsonStr);
                OperationVS operationVS = OperationVS.populate(jsonObject);
                switch(operationVS.getType()) {
                    case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED:
                        receiptCancellation(operationVS);
                        break;
                    case SELECT_IMAGE:
                        selectImage(operationVS);
                        break;
                    case OPEN_RECEIPT:
                        openReceipt(operationVS);
                        break;
                    case OPEN_RECEIPT_FROM_URL:
                        browserHelper.processOperationVS(null, operationVS);
                        break;
                    case SAVE_RECEIPT:
                        saveReceipt(operationVS);
                        break;
                    case SAVE_RECEIPT_ANONYMOUS_DELEGATION:
                        saveReceiptAnonymousDelegation(operationVS);
                        break;
                    case MESSAGEVS_GET:
                        JSONObject documentJSON = (JSONObject)JSONSerializer.toJSON(operationVS.getDocument());
                        WebSocketService.getInstance().sendMessage(documentJSON.toString());
                        break;
                    default:
                        browserHelper.processOperationVS(operationVS);
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                showMessage( ContextVS.getMessage("errorLbl") + " - " + ex.getMessage());
            }
        }

    }

    private void saveReceiptAnonymousDelegation(OperationVS operation) throws Exception{
        logger.debug("saveReceiptAnonymousDelegation - hashCertVSBase64: " + operation.getMessage() +
                " - callbackId: " + operation.getCallerCallback());
        ResponseVS responseVS = ContextVS.getInstance().getHashCertVSData(operation.getMessage());
        if(responseVS == null) {
            logger.error("Missing receipt data for hash: " + operation.getMessage());
            sendMessageToBrowserApp(ResponseVS.SC_ERROR, null, operation.getCallerCallback());
        } else {
            File fileToSave = Utils.getReceiptBundle(responseVS);
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Zip (*.zip)",
                    "*" + ContentTypeVS.ZIP.getExtension());
            fileChooser.getExtensionFilters().add(extFilter);
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setInitialFileName(ContextVS.getMessage("anonymousDelegationReceiptFileName"));
            File file = fileChooser.showSaveDialog(browserStage);
            if(file != null){
                FileUtils.copyStreamToFile(new FileInputStream(fileToSave), file);
                sendMessageToBrowserApp(ResponseVS.SC_OK, null, operation.getCallerCallback());
            } else sendMessageToBrowserApp(ResponseVS.SC_ERROR, null, operation.getCallerCallback());
        }
    }

    private void saveReceipt(OperationVS operation) throws Exception{
        logger.debug("saveReceipt");
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                ContextVS.getMessage("signedFileFileFilterMsg"), "*" + ContentTypeVS.SIGNED.getExtension());
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setInitialFileName(ContextVS.getMessage("genericReceiptFileName"));
        File file = fileChooser.showSaveDialog(browserStage);
        if(file != null){
            FileUtils.copyStringToFile(operation.getMessage(), file);
            sendMessageToBrowserApp(ResponseVS.SC_OK, null, operation.getCallerCallback());
        } else sendMessageToBrowserApp(ResponseVS.SC_ERROR, null, operation.getCallerCallback());
    }

    private void openReceipt(OperationVS operation) throws Exception{
        logger.debug("openReceipt");
        String smimeMessageStr = new String(Base64.getDecoder().decode(operation.getMessage()), "UTF-8");
        SignedDocumentsBrowser.showDialog(smimeMessageStr, operation.getDocument());
    }

    private void selectImage(final OperationVS operationVS) throws Exception {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                try {
                    FileChooser fileChooser = new FileChooser();
                    FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter(
                            "JPG (*.jpg)", Arrays.asList("*.jpg", "*.JPG"));
                    FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter(
                            "PNG (*.png)", Arrays.asList("*.png", "*.PNG"));
                    fileChooser.getExtensionFilters().addAll(extFilterJPG, extFilterPNG);
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                    File selectedImage = fileChooser.showOpenDialog(null);
                    if(selectedImage != null){
                        byte[] imageFileBytes = FileUtils.getBytesFromFile(selectedImage);
                        logger.debug(" - imageFileBytes.length: " + imageFileBytes.length);
                        if(imageFileBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
                            logger.debug(" - MAX_FILE_SIZE exceeded ");
                            sendMessageToBrowserApp(ResponseVS.SC_ERROR,
                                    ContextVS.getMessage("fileSizeExceeded", ContextVS.IMAGE_MAX_FILE_SIZE_KB),
                                    operationVS.getCallerCallback());
                        } else sendMessageToBrowserApp(ResponseVS.SC_OK, selectedImage.getAbsolutePath(),
                                operationVS.getCallerCallback());
                    } else sendMessageToBrowserApp(ResponseVS.SC_ERROR, null, operationVS.getCallerCallback());
                } catch(Exception ex) {
                    sendMessageToBrowserApp(ResponseVS.SC_ERROR, ex.getMessage(), operationVS.getCallerCallback());
                }
            }
        });
    }

    private void receiptCancellation(final OperationVS operationVS) throws Exception {
        logger.debug("receiptCancellation");
        switch(operationVS.getType()) {
            case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED:
                PlatformImpl.runLater(new Runnable() {
                    @Override public void run() {
                        FileChooser fileChooser = new FileChooser();
                        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Zip (*.zip)",
                                "*" + ContentTypeVS.ZIP.getExtension());
                        fileChooser.getExtensionFilters().add(extFilter);
                        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                        File file = fileChooser.showSaveDialog(browserStage);
                        if(file != null){
                            operationVS.setFile(file);
                            browserHelper.processOperationVS(operationVS);
                        } else sendMessageToBrowserApp(ResponseVS.SC_ERROR, null, operationVS.getCallerCallback());
                    }
                });
                break;
            default:
                logger.debug("receiptCancellation - unknown receipt type: " + operationVS.getType());
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