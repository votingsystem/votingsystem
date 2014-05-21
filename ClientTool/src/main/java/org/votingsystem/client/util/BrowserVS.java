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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.FXMessageDialog;
import org.votingsystem.model.*;
import org.votingsystem.util.FileUtils;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
    private VBox mainVBox;
    private BrowserVSStackPane browserHelper;
    private AtomicInteger offset = new AtomicInteger(0);


    public BrowserVS() {
        this(new WebView());
    }

    private BrowserVS(WebView webView) {
        this.webView = webView;
        Platform.setImplicitExit(false);
        signatureService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                logger.debug("signatureService - OnSucceeded");
                PlatformImpl.runLater(new Runnable() {
                    @Override public void run() {
                        ResponseVS responseVS = signatureService.getValue();
                        if(ContentTypeVS.JSON == responseVS.getContentType()) {
                            sendMessageToBrowserApp(responseVS.getJSONMessage(),
                                    signatureService.getOperationVS().getCallerCallback());
                        } else sendMessageToBrowserApp(responseVS.getStatusCode(), responseVS.getMessage(),
                                signatureService.getOperationVS().getCallerCallback());
                    }
                });
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
                webView.getEngine().loadContent("");
                browserStage.hide();
                logger.debug("browserStage.setOnCloseRequest");
            }
        });

        mainVBox = new VBox();
        VBox.setVgrow(webView, Priority.ALWAYS);

        final Button forwardButton = new Button();
        final Button prevButton = new Button();

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
                        new BrowserVS(smallView).show(700, 700, false);
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
        mainVBox.getChildren().addAll(toolBar, webView);
        browserHelper = new BrowserVSStackPane(signatureService);
        browserHelper.getChildren().add(0, mainVBox);

        Scene scene = new Scene(browserHelper, Color.web("#666970"));
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
        String jsCommand = callbackFunction + "(" + messageJSON.toString() + ")";
        logger.debug("sendMessageToBrowserApp - jsCommand: " + jsCommand);
        webView.getEngine().executeScript(jsCommand);
    }

    public void sendMessageToBrowserApp(JSONObject messageJSON, String callbackFunction) {
        logger.debug("sendMessageToBrowserApp - messageJSON: " + messageJSON.toString());
        String jsCommand = callbackFunction + "(" + messageJSON.toString() + ")";
        logger.debug("sendMessageToBrowserApp - jsCommand: " + jsCommand);
        webView.getEngine().executeScript(jsCommand);
    }

    private void openPopUp1(WebView popUpWebView) {
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
            logger.debug("JavafxClient.setJSONMessageToSignatureClient: " + messageToSignatureClient);
            try {
                JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(messageToSignatureClient);
                OperationVS operationVS = OperationVS.populate(jsonObject);
                switch(operationVS.getType()) {
                    case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED:
                        receiptCancellation(operationVS);
                        break;
                    case SELECT_IMAGE:
                        selectImage(operationVS);
                        break;
                    default:
                        browserHelper.processOperationVS(operationVS);
                }
            } catch(Exception ex) {
                showMessage( ContextVS.getMessage("errorLbl") + " - " + ex.getMessage());
            }
        }

        public void setTEXTMessageToSignatureClient(final String messageToSignatureClient, final String callbackFunction) {
            logger.debug("JavafxClient.setTEXTMessageToSignatureClient - callbackFunction: " + callbackFunction);
                PlatformImpl.runLater(new Runnable() {
                    @Override public void run() {
                        try {
                            if(callbackFunction.toLowerCase().contains("receipt")) {
                                saveReceipt(messageToSignatureClient, callbackFunction);
                            }
                        } catch(Exception ex) {
                            logger.error(ex.getMessage(), ex);
                            showMessage( ContextVS.getMessage("errorLbl") + " - " + ex.getMessage());
                        }
                    }
                });

        }

    }


    private void saveReceipt(String messageToSignatureClient, String callbackFunction) throws Exception{
        logger.debug("saveReceipt");
        if(callbackFunction.toLowerCase().contains("anonymousdelegation")) {
            ResponseVS responseVS = ContextVS.getInstance().getHashCertVSData(messageToSignatureClient);
            if(responseVS == null) {
                logger.error("Missing receipt data for hash: " + messageToSignatureClient);
                sendMessageToBrowserApp(ResponseVS.SC_ERROR, null, callbackFunction);
            } else {
                File fileToSave = Utils.getAnonymousRepresentativeSelectCancellationFile(responseVS);
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Zip (*.zip)",
                        "*." + ContentTypeVS.ZIP.getExtension());
                fileChooser.getExtensionFilters().add(extFilter);
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                fileChooser.setInitialFileName(ContextVS.getMessage("anonymousDelegationReceiptFileName"));
                File file = fileChooser.showSaveDialog(browserStage);
                if(file != null){
                    FileUtils.copyStreamToFile(new FileInputStream(fileToSave), file);
                    sendMessageToBrowserApp(ResponseVS.SC_OK, null, callbackFunction);
                } else sendMessageToBrowserApp(ResponseVS.SC_ERROR, null, callbackFunction);
            }
        } else {
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Signed files (*.p7s)",
                    "*." + ContentTypeVS.SIGNED.getExtension());
            fileChooser.getExtensionFilters().add(extFilter);
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setInitialFileName(ContextVS.getMessage("genericReceiptFileName"));
            File file = fileChooser.showSaveDialog(browserStage);
            if(file != null){
                Utils.saveFile(messageToSignatureClient, file);
                sendMessageToBrowserApp(ResponseVS.SC_OK, null, callbackFunction);
            } else sendMessageToBrowserApp(ResponseVS.SC_ERROR, null, callbackFunction);
        }
    }


    private void selectImage(final OperationVS operationVS) throws Exception {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                try {
                    FileChooser fileChooser = new FileChooser();
                    FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter(
                            "JPG files (*.jpg)", Arrays.asList("*.jpg", "*.JPG"));
                    FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter(
                            "PNG files (*.png)", Arrays.asList("*.png", "*.PNG"));
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
                /*try {
                    BufferedImage bufferedImage = ImageIO.read(file);
                    Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                    myImageView.setImage(image);
                } catch (IOException ex) {
                    Logger.getLogger(JavaFXPixel.class.getName()).log(Level.SEVERE, null, ex);
                }*/
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
                                "*." + ContentTypeVS.ZIP.getExtension());
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