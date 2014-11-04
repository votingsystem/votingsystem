package org.votingsystem.client;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import net.sf.json.JSON;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.controller.VicketPaneController;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.pane.BrowserVSPane;
import org.votingsystem.client.pane.DocumentVSBrowserStackPane;
import org.votingsystem.client.service.WebSocketService;
import org.votingsystem.client.service.WebSocketServiceAuthenticated;
import org.votingsystem.client.util.BrowserVSSessionUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.client.util.WebKitHost;
import org.votingsystem.client.util.WebSocketListener;
import org.votingsystem.model.*;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.vicket.model.Vicket;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVS extends Region implements WebKitHost, WebSocketListener {

    private static Logger log = Logger.getLogger(BrowserVS.class);
    private static final int BROWSER_WIDTH = 1200;
    private static final int BROWSER_HEIGHT = 1000;

    private Stage browserStage;
    private Map<String, WebView> webViewMap = new HashMap<>();
    private TextField locationField = new TextField("");
    private final BrowserVSPane browserHelper;
    private TabPane tabPane;
    private Button prevButton;
    private WebSocketServiceAuthenticated webSocketServiceAuthenticated;
    private WebSocketService webSocketService;
    private static final BrowserVS INSTANCE = new BrowserVS();

    public static BrowserVS getInstance() {
        return INSTANCE;
    }

    private BrowserVS() {
        browserHelper = new BrowserVSPane();
        Platform.setImplicitExit(false);
        browserHelper.getSignatureService().setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                log.debug("signatureService - OnSucceeded");
                PlatformImpl.runLater(new Runnable() {
                    @Override public void run() {
                        ResponseVS responseVS = browserHelper.getSignatureService().getValue();
                        if(ResponseVS.SC_INITIALIZED == responseVS.getStatusCode()) {
                            log.debug("signatureService - OnSucceeded - ResponseVS.SC_INITIALIZED");
                        } else if(ContentTypeVS.JSON == responseVS.getContentType()) {
                            sendMessageToBrowser(responseVS.getMessageJSON(),
                                    browserHelper.getSignatureService().getOperationVS().getCallerCallback());
                        } else sendMessageToBrowser(responseVS.getStatusCode(), responseVS.getMessage(),
                                browserHelper.getSignatureService().getOperationVS().getCallerCallback());
                    }
                });
            }
        });
        browserHelper.getSignatureService().setOnRunning(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                log.debug("signatureService - OnRunning");
            }
        });
        browserHelper.getSignatureService().setOnCancelled(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                log.debug("signatureService - OnCancelled");
            }
        });
        browserHelper.getSignatureService().setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent t) {
                log.debug("signatureService - OnFailed");
            }
        });
        initComponents();
    }

    private void initComponents() {
        log.debug("initComponents");
        browserStage = new Stage();
        browserStage.initModality(Modality.WINDOW_MODAL);
        browserStage.setTitle(ContextVS.getMessage("mainDialogCaption"));
        browserStage.setResizable(true);
        browserStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent event) {
                event.consume();
                browserStage.hide();
                log.debug("browserStage.setOnCloseRequest");
            }
        });
        VBox mainVBox = new VBox();
        prevButton = new Button();
        final Button forwardButton = new Button();
        final Button reloadButton = new Button();
        forwardButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHEVRON_RIGHT));
        forwardButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override public void handle(javafx.event.ActionEvent ev) {
                try {
                    ((WebView)tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().getHistory().go(1);
                    prevButton.setDisable(false);
                } catch(Exception ex) {
                    forwardButton.setDisable(true);
                }
            }
        });
        prevButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHEVRON_LEFT));
        prevButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override public void handle(javafx.event.ActionEvent ev) {
                try {
                    ((WebView)tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().getHistory().go(-1);
                    forwardButton.setDisable(false);
                } catch(Exception ex) {
                    prevButton.setDisable(true);
                }
            }
        });
        reloadButton.setGraphic(Utils.getImage(FontAwesome.Glyph.REFRESH));
        reloadButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override public void handle(javafx.event.ActionEvent ev) {
                //webView.getEngine().load(locationField.getText());
                ((WebView)tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().load(locationField.getText());
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
                        ((WebView)tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().load(targetURL);
                    }
                }
            }
        });
        HBox toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().addAll(prevButton, forwardButton, locationField, reloadButton , Utils.createSpacer());
        tabPane = new TabPane();
        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setSide(Side.TOP);
        HBox.setHgrow(tabPane, Priority.ALWAYS);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        final AnchorPane tabPainContainer = new AnchorPane();
        final Button addButton = new Button("+");
        addButton.getStyleClass().add("newtab-button");
        AnchorPane.setTopAnchor(tabPane, 0.0);
        AnchorPane.setLeftAnchor(tabPane, 0.0);
        AnchorPane.setRightAnchor(tabPane, 0.0);
        AnchorPane.setBottomAnchor(tabPane, 0.0);
        AnchorPane.setTopAnchor(addButton, 1.0);
        AnchorPane.setLeftAnchor(addButton, 5.0);
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                newTab(null, null, null);
            }
        });
        tabPainContainer.getChildren().addAll(tabPane, addButton);
        VBox.setVgrow(tabPainContainer, Priority.ALWAYS);
        mainVBox.getChildren().addAll(toolBar, tabPainContainer);
        mainVBox.getStylesheets().add(((Object)this).getClass().getResource("/css/browservs.css").toExternalForm());
        browserHelper.getChildren().add(0, mainVBox);
        browserStage.setScene(new Scene(browserHelper));
        browserStage.setWidth(BROWSER_WIDTH);
        browserStage.setHeight(BROWSER_HEIGHT);
        getChildren().addListener(new ListChangeListener<Node>() {
            @Override public void onChanged(Change<? extends Node> c) {}
        });
    }

    public WebView newTab(String URL, String tabCaption, String jsCommand) {
        final WebView webView = new WebView();
        if(jsCommand != null) {
            webView.getEngine().getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    @Override public void changed(ObservableValue<? extends Worker.State> ov,
                        Worker.State oldState, Worker.State newState) {
                        //log.debug("newState: " + newState);
                        if (newState == Worker.State.SUCCEEDED) {
                            webView.getEngine().executeScript(jsCommand.toString());
                        }
                    }
                }
            );
        }
        final WebHistory history = webView.getEngine().getHistory();
        history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>(){
            @Override public void onChanged(Change<? extends WebHistory.Entry> c) {
                c.next();
                if(history.getCurrentIndex() > 0) prevButton.setDisable(false);
                //log.debug("==== currentIndex: " + history.getCurrentIndex() + " - num. entries: " + history.getEntries().size());
                String params = "";
                if(locationField.getText().contains("?")) {
                    params = locationField.getText().substring(locationField.getText().indexOf("?"),
                            locationField.getText().length());
                }
                WebHistory.Entry selectedEntry = history.getEntries().get(history.getEntries().size() - 1);
                log.debug("history change - selectedEntry: " + selectedEntry);
                String newURL = selectedEntry.getUrl();
                if(!newURL.contains("?")) newURL = newURL + params;
                if(history.getEntries().size() > 1 && selectedEntry.getTitle() != null) tabPane.getSelectionModel().
                        getSelectedItem().setText(selectedEntry.getTitle());
                locationField.setText(newURL);
            }
        });
        webView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        webView.getEngine().locationProperty().addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                locationField.setText(newValue);
            }
        });
        webView.getEngine().setCreatePopupHandler(//handle popup windows
                new Callback<PopupFeatures, WebEngine>() {
                    @Override public WebEngine call(PopupFeatures config) {
                        //WebView newView = new WebView();
                        //newView.setFontScale(0.8);
                        //new BrowserVS(newView).show(700, 700, false);
                        return newTab(null, null, null).getEngine();
                    }
                }
        );
        webView.getEngine().getLoadWorker().stateProperty().addListener(
            new ChangeListener<Worker.State>() {
                @Override public void changed(ObservableValue<? extends Worker.State> ov,
                                    Worker.State oldState, Worker.State newState) {
                    //log.debug("newState: " + newState + " - " + webView.getEngine().getLocation());
                    if (newState == Worker.State.SUCCEEDED) {
                        Document doc = webView.getEngine().getDocument();
                        Element element = doc.getElementById("voting_system_page");
                        if(element != null) {
                            JSObject win = (JSObject) webView.getEngine().executeScript("window");
                            win.setMember("clientTool", new JavafxClient(webView));
                            webView.getEngine().executeScript(Utils.getSessionCoreSignalJSCommand(
                                    BrowserVSSessionUtils.getInstance().getBrowserSessionData()));
                        }
                    } else if (newState.equals(Worker.State.FAILED)) {
                        showMessage(new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("connectionErrorMsg")));
                    } else if (newState.equals(Worker.State.SCHEDULED)) { }
                    if(newState.equals(Worker.State.FAILED) || newState.equals(Worker.State.SUCCEEDED)) {  }
                }
            }
        );
        VBox.setVgrow(webView, Priority.ALWAYS);
        Tab newTab = new Tab();
        newTab.setOnSelectionChanged(new EventHandler < Event > (){
            @Override public void handle(Event event) {
                int selectedIdx = tabPane.getSelectionModel().getSelectedIndex();
                ObservableList<WebHistory.Entry> entries = ((WebView)tabPane.getSelectionModel().getSelectedItem().
                        getContent()).getEngine().getHistory().getEntries();
                if(entries.size() > 0){
                    WebHistory.Entry selectedEntry = entries.get(entries.size() -1);
                    if(entries.size() > 1 &&  selectedEntry.getTitle() != null) newTab.setText(selectedEntry.getTitle());
                    log.debug("selectedIdx: " + selectedIdx + " - selectedEntry: " + selectedEntry);
                    locationField.setText(selectedEntry.getUrl());
                }
            }
        });
        if(tabCaption != null) newTab.setText(tabCaption);
        else newTab.setText(ContextVS.getMessage("loadingLbl") + " ...");
        newTab.setContent(webView);
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
        if(URL != null) {
            PlatformImpl.runLater(new Runnable() {
                @Override public void run() {
                    webView.getEngine().load(URL);
                    browserStage.show();
                }
            });
        }
        browserStage.toFront();
        return webView;
    }

    @Override public void sendMessageToBrowser(int statusCode, String message, String callerCallback) {
        String logMsg = message.length() > 300 ? message.substring(0, 300) + "..." : message;
        log.debug("sendMessageToBrowser - statusCode: " + statusCode + " - message: " + logMsg);
        Map resultMap = new HashMap();
        resultMap.put("statusCode", statusCode);
        resultMap.put("message", message);
        try {
            WebView operationWebView = webViewMap.remove(callerCallback);
            JSONObject messageJSON = (JSONObject)JSONSerializer.toJSON(resultMap);
            final String jsCommand = "setClientToolMessage('" + callerCallback + "','" +
                    new String(Base64.getEncoder().encode(messageJSON.toString().getBytes("UTF8")), "UTF8") + "')";
            PlatformImpl.runLater(new Runnable() {
                @Override public void run() {
                    operationWebView.getEngine().executeScript(jsCommand);
                }
            });
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override public void sendMessageToBrowser(JSON messageJSON, String callerCallback) {
        String message = messageJSON.toString();
        String logMsg = message.length() > 300 ? message.substring(0, 300) + "..." : message;
        log.debug("sendMessageToBrowser - messageJSON: " + logMsg);
        try {
            WebView operationWebView = webViewMap.remove(callerCallback);
            final String jsCommand = "setClientToolMessage('" + callerCallback + "','" +
                    Base64.getEncoder().encodeToString(message.getBytes("UTF8")) + "')";
            PlatformImpl.runLater(new Runnable() {
                @Override public void run() {
                    operationWebView.getEngine().executeScript(jsCommand);
                }
            });
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override public void processOperationVS(OperationVS operationVS) {
        browserHelper.processOperationVS(operationVS, null);
    }

    @Override public void processSignalVS(Map signalData) {//{title:, url:}
        log.debug("processSignalVS - title: " + signalData.get("title"));
        if(signalData.containsKey("title")) tabPane.getSelectionModel().getSelectedItem().setText(
                (String)signalData.get("title"));
    }

    public void processResponseVS(OperationVS operationVS) {
        sendMessageToBrowser(operationVS.getStatusCode(), operationVS.getMessage(), operationVS.getCallerCallback());
    }

    public void showMessage(ResponseVS responseVS) {
        String message = responseVS.getMessage() == null? "":responseVS.getMessage();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) message = responseVS.getMessage();
        else message = ContextVS.getMessage("errorLbl") + " - " + responseVS.getMessage();
        final String msg = message;
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                MessageDialog messageDialog = new MessageDialog();
                messageDialog.showMessage(null, msg);
            }
        });
    }

    public void showMessage(Integer statusCode, String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                MessageDialog messageDialog = new MessageDialog();
                messageDialog.showMessage(statusCode, message);
            }
        });
    }

    public void newTab(final String urlToLoad, String callback, String callbackMsg, final String caption,
            final boolean isToolbarVisible) {
        final StringBuilder jsCommand = new StringBuilder();
        if(callback != null && callbackMsg != null) jsCommand.append(callback + "(" + callbackMsg + ")");
        else if(callback != null) jsCommand.append(callback + "()");;
        log.debug("newTab - urlToLoad: " + urlToLoad + " - jsCommand: " + jsCommand.toString());
        newTab(urlToLoad, caption, "".equals(jsCommand.toString()) ? null : jsCommand.toString());
    }

    @Override public void consumeWebSocketMessage(JSONObject messageJSON) {
        messageJSON = messageJSON.getJSONObject("message");
        if(messageJSON.containsKey("operation")) {
            TypeVS operation = TypeVS.valueOf(messageJSON.getString("operation"));
            log.debug("consumeWebSocketMessage: " + operation.toString());
            switch(operation) {
                case INIT_VALIDATED_SESSION:
                    execCommandJS(Utils.getWebSocketCoreSignalJSCommand(messageJSON, ConnectionStatus.OPEN));
                    break;
                case MESSAGEVS_SIGN:
                    showMessage(messageJSON.containsKey("statusCode")?messageJSON.getInt("statusCode"):null,
                            messageJSON.containsKey("message")?messageJSON.getString("message"):null);
                    break;
                default:
                    showMessage(messageJSON.containsKey("statusCode")?messageJSON.getInt("statusCode"):null,
                            messageJSON.containsKey("message")?messageJSON.getString("message"):null);
            }
        } else showMessage(messageJSON.containsKey("statusCode")?messageJSON.getInt("statusCode"):null,
                messageJSON.containsKey("message")?messageJSON.getString("message"):null);
    }

    @Override public void setConnectionStatus(ConnectionStatus status) {
        log.debug("setConnectionStatus - status: " + status.toString());
        switch (status) {
            case CLOSED:
                execCommandJS(Utils.getWebSocketCoreSignalJSCommand(null, ConnectionStatus.CLOSED));
                break;
            case OPEN:
                execCommandJS(Utils.getWebSocketCoreSignalJSCommand(null, ConnectionStatus.OPEN));
                break;
        }
    }

    public void execCommandJS(String jsCommand) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                for(WebView webView : webViewMap.values()) {
                    webView.getEngine().executeScript(jsCommand);
                }
            }
        });
    }

    public void execCommandJSCurrentView(String jsCommand) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                ((WebView)tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().executeScript(jsCommand);
            }
        });
    }

    public WebSocketService getWebSocketService(){
        if(webSocketService == null) {
            webSocketService = new WebSocketService(ContextVS.getInstance().getVotingSystemSSLCerts(),
                    ContextVS.getInstance().getVicketServer());
            webSocketService.addListener(this);
        }
        return webSocketService;
    }

    public WebSocketServiceAuthenticated getWebSocketServiceAuthenticated(){
        if(webSocketServiceAuthenticated == null) {
            webSocketServiceAuthenticated = new WebSocketServiceAuthenticated(ContextVS.getInstance().getVotingSystemSSLCerts(),
                    ContextVS.getInstance().getVicketServer());
            webSocketServiceAuthenticated.addListener(this);
        }
        return webSocketServiceAuthenticated;
    }

    public class JavafxClient {// JavaScript interface object
        private WebView webView;
        public JavafxClient(WebView webView) {
            this.webView = webView;
        }
        public void setJSONMessageToSignatureClient(String messageToSignatureClient) {
            try {
                String jsonStr =  StringUtils.decodeB64_TO_UTF8(messageToSignatureClient);
                String logMsg = jsonStr.length() > 300 ? jsonStr.substring(0, 300) + "..." : jsonStr;
                log.debug("JavafxClient.setJSONMessageToSignatureClient: " + logMsg);
                JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(jsonStr);
                OperationVS operationVS = OperationVS.parse(jsonObject);
                webViewMap.put(operationVS.getCallerCallback(), this.webView);
                switch (operationVS.getType()) {
                    case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED:
                        Utils.receiptCancellation(operationVS, BrowserVS.this);
                        break;
                    case CONNECT:
                        getWebSocketServiceAuthenticated().setConnectionEnabled(true, operationVS.getDocument());
                        break;
                    case DISCONNECT:
                        getWebSocketServiceAuthenticated().setConnectionEnabled(false, null);
                        break;
                    case MESSAGEVS_TO_DEVICE:
                        getWebSocketService().sendMessage(jsonStr);
                        break;
                    case  KEYSTORE_SELECT:
                        Utils.selectKeystoreFile(operationVS, BrowserVS.this);
                        break;
                    case SELECT_IMAGE:
                        Utils.selectImage(operationVS, BrowserVS.this);
                        break;
                    case OPEN_SMIME:
                        String smimeMessageStr = new String(Base64.getDecoder().decode(
                                operationVS.getMessage().getBytes()), "UTF-8");
                        DocumentVSBrowserStackPane.showDialog(smimeMessageStr, operationVS.getDocument());
                        break;
                    case OPEN_VICKET:
                        VicketPaneController.show((Vicket) ObjectUtils.deSerializeObject((
                                (String) operationVS.getDocument().get("object")).getBytes()));
                        break;
                    case OPEN_SMIME_FROM_URL:
                        browserHelper.processOperationVS(null, operationVS);
                        break;
                    case SAVE_SMIME:
                        Utils.saveReceipt(operationVS, BrowserVS.this);
                        break;
                    case SAVE_SMIME_ANONYMOUS_DELEGATION:
                        Utils.saveReceiptAnonymousDelegation(operationVS, BrowserVS.this);
                        break;
                    case MESSAGEVS_GET:
                        JSONObject documentJSON = (JSONObject) JSONSerializer.toJSON(operationVS.getDocument());
                        WebSocketServiceAuthenticated.getInstance().sendMessage(documentJSON.toString());
                        break;
                    case CERT_USER_NEW:
                        browserHelper.processOperationVS(operationVS, ContextVS.getMessage("newCertPasswDialogMsg"));
                        break;
                    default:
                        browserHelper.processOperationVS(operationVS, null);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                showMessage(new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("errorLbl") + " - " +
                        ex.getMessage()));
            }
        }

        public String call(String messageToSignatureClient) {
            String result = null;
            try {
                String jsonStr = StringUtils.decodeB64_TO_UTF8(messageToSignatureClient);
                JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(jsonStr);
                OperationVS operationVS = OperationVS.parse(jsonObject);
                switch (operationVS.getType()) {
                    case FORMAT_DATE:
                        Date dateToFormat = DateUtils.getDateFromString((String) operationVS.getDocument().get("dateStr"),
                                (String) operationVS.getDocument().get("dateFormat"));
                        String stringFormat = null;
                        if (operationVS.getDocument().get("stringFormat") == null && !JSONNull.getInstance().equals(
                                operationVS.getDocument().get("stringFormat"))) {
                            stringFormat = (String) operationVS.getDocument().get("stringFormat");
                        }
                        if (stringFormat != null) result = DateUtils.getDateStr(dateToFormat,
                                (String) operationVS.getDocument().get("stringFormat"));
                        else result = DateUtils.getDayWeekDateStr(dateToFormat);
                        return result;
                    case SIGNAL_VS:
                        processSignalVS(operationVS.getDocument());
                        break;
                    default:
                        return "Unknown operation: '" + operationVS.getType() + "'";
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                result = ex.getMessage();
            } finally {
                return result;
            }
        }
    }

}