package org.votingsystem.client;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.json.JSON;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.pane.BrowserVSPane;
import org.votingsystem.client.service.*;
import org.votingsystem.client.util.BrowserVSClient;
import org.votingsystem.client.util.Utils;
import org.votingsystem.client.util.WebKitHost;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVS extends Region implements WebKitHost {

    private static Logger log = Logger.getLogger(BrowserVS.class);
    private static final int BROWSER_WIDTH = 1200;
    private static final int BROWSER_HEIGHT = 1000;
    private static final int MAX_CHARACTERS_TAB_CAPTION = 25;

    private Stage browserStage;
    private Map<String, WebView> webViewMap = new HashMap<String, WebView>();
    private TextField locationField = new TextField("");
    private final BrowserVSPane browserHelper;
    private HBox toolBar;
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
        browserHelper.getSignatureService().setOnSucceeded(event -> {
            log.debug("signatureService - OnSucceeded");
            PlatformImpl.runLater(() -> {
                ResponseVS responseVS = browserHelper.getSignatureService().getValue();
                if(responseVS.getStatus() != null) {
                    NotificationService.getInstance().postToEventBus(responseVS);
                } else if(ResponseVS.SC_INITIALIZED == responseVS.getStatusCode()) {
                    log.debug("signatureService - OnSucceeded - ResponseVS.SC_INITIALIZED");
                } else if(ContentTypeVS.JSON == responseVS.getContentType()) {
                    sendMessageToBrowser(responseVS.getMessageJSON(),
                            browserHelper.getSignatureService().getOperationVS().getCallerCallback());
                } else sendMessageToBrowser(Utils.getMessageToBrowser(responseVS.getStatusCode(),
                        responseVS.getMessage()), browserHelper.getSignatureService().getOperationVS().
                        getCallerCallback());
            });
        });
        browserHelper.getSignatureService().setOnRunning(event -> log.debug("signatureService - OnRunning"));
        browserHelper.getSignatureService().setOnCancelled(event -> log.debug("signatureService - OnCancelled"));
        browserHelper.getSignatureService().setOnFailed(event -> log.debug("signatureService - OnFailed"));
        initComponents();
    }

    private void initComponents() {
        log.debug("initComponents");
        browserStage = new Stage();
        browserStage.initModality(Modality.WINDOW_MODAL);
        browserStage.setTitle(ContextVS.getMessage("mainDialogCaption"));
        browserStage.setResizable(true);
        browserStage.setOnCloseRequest(event -> {
            event.consume();
            browserStage.hide();
            browserHelper.getSignatureService().cancel();
            log.debug("browserStage.setOnCloseRequest");
        });
        VBox mainVBox = new VBox();
        prevButton = new Button();
        final Button forwardButton = new Button();
        final Button reloadButton = new Button();
        forwardButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHEVRON_RIGHT));
        forwardButton.setOnAction((event) -> {
            try {
                ((WebView)tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().getHistory().go(1);
                prevButton.setDisable(false);
            } catch(Exception ex) { forwardButton.setDisable(true); }
        });
        prevButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHEVRON_LEFT));
        prevButton.setOnAction(event -> {
            try {
                ((WebView) tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().getHistory().go(-1);
                forwardButton.setDisable(false);
            } catch (Exception ex) {
                prevButton.setDisable(true);
            }
        });
        reloadButton.setGraphic(Utils.getImage(FontAwesome.Glyph.REFRESH));
        reloadButton.setOnAction(event -> ((WebView) tabPane.getSelectionModel().getSelectedItem().getContent()).
                getEngine().load(locationField.getText()));
        prevButton.setDisable(true);
        forwardButton.setDisable(true);
        locationField.setPrefWidth(400);
        HBox.setHgrow(locationField, Priority.ALWAYS);
        locationField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                if (!"".equals(locationField.getText())) {
                    String targetURL = null;
                    if (locationField.getText().startsWith("http://") || locationField.getText().startsWith("https://")) {
                        targetURL = locationField.getText().trim();
                    } else targetURL = "http://" + locationField.getText().trim();
                    ((WebView) tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().load(targetURL);
                }
            }
        });
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        NotificationService.getInstance().setNotificationsButton(new Button());
        InboxService.getInstance().setInboxButton(new Button());
        toolBar.getChildren().addAll(prevButton, forwardButton, locationField, reloadButton, Utils.createSpacer(),
                NotificationService.getInstance().getNotificationsButton(), InboxService.getInstance().getInboxButton());
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
        addButton.setOnAction(event -> newTab(null, null, null));
        tabPainContainer.getChildren().addAll(tabPane, addButton);
        VBox.setVgrow(tabPainContainer, Priority.ALWAYS);
        mainVBox.getChildren().addAll(toolBar, tabPainContainer);
        mainVBox.getStylesheets().add(Utils.getResource("/css/browservs.css"));
        browserHelper.getChildren().add(0, mainVBox);
        browserStage.setScene(new Scene(browserHelper));
        browserStage.setWidth(BROWSER_WIDTH);
        browserStage.setHeight(BROWSER_HEIGHT);
        browserStage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
        locationField.setOnMouseClicked(event -> createHistoryMenu(((WebView)tabPane.getSelectionModel().getSelectedItem().
                getContent())).show(locationField, Side.BOTTOM, 0, 0));
    }

    public WebView newTab(String URL, String tabCaption, String jsCommand) {
        final WebView webView = new WebView();
        if(jsCommand != null) {
            webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newState) -> {
                    //log.debug("newState: " + newState);
                    if (newState == Worker.State.SUCCEEDED) webView.getEngine().executeScript(jsCommand.toString());
                });
        }
        final WebHistory history = webView.getEngine().getHistory();
        history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>(){
            @Override public void onChanged(Change<? extends WebHistory.Entry> c) {
                c.next();
                if(history.getCurrentIndex() > 0) prevButton.setDisable(false);
                //log.debug("currentIndex: " + history.getCurrentIndex() + " - num. entries: " + history.getEntries().size());
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
        webView.getEngine().setOnError(event ->  log.error(event.getMessage(), event.getException()));
        webView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        webView.getEngine().locationProperty().addListener((observable, oldValue, newValue) ->
                locationField.setText(newValue));
        webView.getEngine().setCreatePopupHandler(config -> {//handle popup windows
            //WebView newView = new WebView();
            //newView.setFontScale(0.8);
            //new BrowserVS(newView).show(700, 700, false);
            return newTab(null, null, null).getEngine();
        });
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
                            win.setMember("clientTool", new BrowserVSClient(webView));
                            webView.getEngine().executeScript(Utils.getSessionCoreSignalJSCommand(
                                    SessionService.getInstance().getBrowserSessionData()));
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
        newTab.setOnSelectionChanged(event -> {
                int selectedIdx = tabPane.getSelectionModel().getSelectedIndex();
                ObservableList<WebHistory.Entry> entries = ((WebView)tabPane.getSelectionModel().getSelectedItem().
                        getContent()).getEngine().getHistory().getEntries();
                if(entries.size() > 0){
                    WebHistory.Entry selectedEntry = entries.get(entries.size() -1);
                    if(entries.size() > 1 &&  selectedEntry.getTitle() != null) newTab.setText(selectedEntry.getTitle());
                    log.debug("selectedIdx: " + selectedIdx + " - selectedEntry: " + selectedEntry);
                    locationField.setText(selectedEntry.getUrl());
                }
            });
        if(tabCaption != null) newTab.setText(tabCaption.length() > MAX_CHARACTERS_TAB_CAPTION ?
                tabCaption.substring(0, MAX_CHARACTERS_TAB_CAPTION) + "...":tabCaption);
        else if(URL != null) newTab.setText(ContextVS.getMessage("loadingLbl") + " ...");
        else newTab.setText("                ");
        newTab.setContent(webView);
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
        if(URL != null) {
            PlatformImpl.runLater(() -> {
                webView.getEngine().load(URL);
                browserStage.show();
            });
        }
        browserStage.toFront();
        return webView;
    }

    @Override public void sendMessageToBrowser(JSON messageJSON, String callerCallback) {
        String message = messageJSON.toString();
        String logMsg = message.length() > 300 ? message.substring(0, 300) + "..." : message;
        log.debug("sendMessageToBrowser - messageJSON: " + logMsg);
        try {
            WebView operationWebView = webViewMap.remove(callerCallback);
            final String jsCommand = "setClientToolMessage('" + callerCallback + "','" +
                    Base64.getEncoder().encodeToString(message.getBytes("UTF8")) + "')";
            PlatformImpl.runLater(() -> {  operationWebView.getEngine().executeScript(jsCommand); });
        } catch(Exception ex) { log.error(ex.getMessage(), ex); }
    }

    @Override public void processOperationVS(OperationVS operationVS, String passwordDialogMessage) {
        browserHelper.processOperationVS(operationVS, passwordDialogMessage);
    }

    @Override public void processOperationVS(String password, OperationVS operationVS) {
        browserHelper.processOperationVS(password, operationVS);
    }

    @Override public void processSignalVS(Map signalData) {//{title:, url:}
        log.debug("processSignalVS - caption: " + signalData.get("caption"));
        if(signalData.containsKey("caption")) tabPane.getSelectionModel().getSelectedItem().setText(
                (String)signalData.get("caption"));
    }


    private ContextMenu createHistoryMenu(WebView webView) { // a menu of history items.
        final ContextMenu historyMenu = new ContextMenu();
        WebHistory history = webView.getEngine().getHistory();
        // determine an appropriate subset range of the history list to display.
        int minIdx = Math.max(0, history.getCurrentIndex() - 8); // min range (inclusive) of history items to show.
        int maxIdx = Math.min(history.getEntries().size(), history.getCurrentIndex() + 6); // min range (exclusive) of history items to show.
        // add menu items to the history list.
        for (int i = maxIdx - 1; i >= minIdx; i--) {
            final MenuItem nextMenuItem = new MenuItem(history.getEntries().get(i).getUrl());
            nextMenuItem.setOnAction(actionEvent -> webView.getEngine().load(nextMenuItem.getText()));
            historyMenu.getItems().add(nextMenuItem);
            if (i == history.getCurrentIndex()) {
                nextMenuItem.getStyleClass().add("current-menu");
            }
        }
        return historyMenu;
    }

    public void newTab(final String urlToLoad, String callback, String callbackMsg, final String caption,
            final boolean isToolbarVisible) {
        final StringBuilder jsCommand = new StringBuilder();
        if(callback != null && callbackMsg != null) jsCommand.append(callback + "(" + callbackMsg + ")");
        else if(callback != null) jsCommand.append(callback + "()");;
        log.debug("newTab - urlToLoad: " + urlToLoad + " - jsCommand: " + jsCommand.toString());
        newTab(urlToLoad, caption, "".equals(jsCommand.toString()) ? null : jsCommand.toString());
    }

    public void execCommandJS(String jsCommand) {
        PlatformImpl.runLater(() -> {
            for(WebView webView : webViewMap.values()) {
                webView.getEngine().executeScript(jsCommand);
            }
        });
    }

    public void execCommandJSCurrentView(String jsCommand) {
        PlatformImpl.runLater(() -> {
            ((WebView)tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().executeScript(jsCommand);
        });
    }

    public void registerCallerCallbackView(String callerCallback, WebView webView) {
        webViewMap.put(callerCallback, webView);
    }

}