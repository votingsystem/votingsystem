package org.votingsystem.client;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sf.json.JSON;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.pane.BrowserVSPane;
import org.votingsystem.client.pane.DocumentVSBrowserPane;
import org.votingsystem.client.pane.MainOptionsPane;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.service.NotificationService;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.client.util.BrowserVSClient;
import org.votingsystem.client.util.ResizeHelper;
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
    private BrowserVSPane browserHelper;
    private MainOptionsPane mainOptionsPane;
    private HBox toolBar;
    private TabPane tabPane;
    private Button prevButton;
    private static BrowserVS INSTANCE;

    public static BrowserVS init(Stage browserStage) {
        INSTANCE = new BrowserVS(browserStage);
        return INSTANCE;
    }

    public static BrowserVS getInstance() {
        return INSTANCE;
    }

    public void open() {
        newTab(mainOptionsPane, ContextVS.getMessage("operationsLbl"));
    }

    private BrowserVS(Stage browserStage) {
        browserHelper = new BrowserVSPane();
        mainOptionsPane = new MainOptionsPane();
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
        initComponents(browserStage);
    }

    private void initComponents(Stage browserStage) {
        log.debug("initComponents");
        this.browserStage = browserStage;
        //browserStage.initModality(Modality.WINDOW_MODAL);
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
        forwardButton.getStyleClass().add("toolbar-button");
        forwardButton.setOnAction((event) -> {
            try {
                ((WebView) tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().getHistory().go(1);
                prevButton.setDisable(false);
            } catch (Exception ex) {
                forwardButton.setDisable(true);
            }
        });
        prevButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHEVRON_LEFT));
        prevButton.getStyleClass().add("toolbar-button");
        prevButton.setOnAction(event -> {
            try {
                ((WebView) tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().getHistory().go(-1);
                forwardButton.setDisable(false);
            } catch (Exception ex) {
                prevButton.setDisable(true);
            }
        });
        reloadButton.setGraphic(Utils.getImage(FontAwesome.Glyph.REFRESH));
        reloadButton.getStyleClass().add("toolbar-button");
        reloadButton.setOnAction(event -> ((WebView) tabPane.getSelectionModel().getSelectedItem().getContent()).
                getEngine().load(locationField.getText()));
        prevButton.setDisable(true);
        forwardButton.setDisable(true);
        final Button newTabButton = new Button();
        newTabButton.setGraphic(Utils.getImage(FontAwesome.Glyph.PLUS));
        newTabButton.getStyleClass().add("toolbar-button");
        newTabButton.setOnAction(event -> newTab(null, null, null));

        locationField.setPrefWidth(400);
        HBox.setHgrow(locationField, Priority.ALWAYS);
        locationField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                if (!"".equals(locationField.getText())) {
                    String targetURL = null;
                    if (locationField.getText().startsWith("http://") || locationField.getText().startsWith("https://")) {
                        targetURL = locationField.getText().trim();
                    } else targetURL = "http://" + locationField.getText().trim();
                    Object content = tabPane.getSelectionModel().getSelectedItem().getContent();
                    if (content instanceof WebView) ((WebView) content).getEngine().load(targetURL);
                    else newTab(targetURL, null, null);
                }
            }
        });
        toolBar = new HBox();
        toolBar.setSpacing(10);
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        NotificationService.getInstance().setNotificationsButton(new Button());
        InboxService.getInstance().setInboxButton(new Button());
        Button menuButton = new Button();
        menuButton.setGraphic(Utils.getImage(FontAwesome.Glyph.BARS));
        menuButton.getStyleClass().add("toolbar-button");
        Button closeButton = new Button();
        closeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES));
        closeButton.getStyleClass().addAll("toolbar-button", "close-button");
        closeButton.setOnAction(actionEvent -> VotingSystemApp.getInstance().stop());

        toolBar.getChildren().addAll(newTabButton, prevButton, forwardButton, locationField, reloadButton, Utils.createSpacer(),
                NotificationService.getInstance().getNotificationsButton(), InboxService.getInstance().getInboxButton(), menuButton, closeButton);
        toolBar.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
                    if(mouseEvent.getClickCount() == 2){
                        browserStage.setFullScreenExitHint("");
                        browserStage.setFullScreen(!browserStage.isFullScreen());
                    }
                }
            }
        });

        tabPane = new TabPane();
        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setSide(Side.TOP);
        HBox.setHgrow(tabPane, Priority.ALWAYS);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        final AnchorPane tabPainContainer = new AnchorPane();
        AnchorPane.setTopAnchor(tabPane, 0.0);
        AnchorPane.setLeftAnchor(tabPane, 0.0);
        AnchorPane.setRightAnchor(tabPane, 0.0);
        AnchorPane.setBottomAnchor(tabPane, 0.0);
        tabPainContainer.getChildren().addAll(tabPane);
        VBox.setVgrow(tabPainContainer, Priority.ALWAYS);
        mainVBox.setMargin(toolBar, new Insets(6, 6, 6, 6));
        mainVBox.getChildren().addAll(toolBar, tabPainContainer);
        mainVBox.getStylesheets().add(Utils.getResource("/css/browservs.css"));
        mainVBox.getStyleClass().add("main-dialog");
        browserHelper.getChildren().add(0, mainVBox);
        browserStage.setScene(new Scene(browserHelper));
        browserStage.setWidth(BROWSER_WIDTH);
        browserStage.setHeight(BROWSER_HEIGHT);
        browserStage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
        browserStage.initStyle(StageStyle.UNDECORATED);
        locationField.setOnMouseClicked(event -> {
            Object content = tabPane.getSelectionModel().getSelectedItem().getContent();
            if (content instanceof WebView) createHistoryMenu((WebView) content).show(locationField, Side.BOTTOM, 0, 0);
        });
        Utils.addMouseDragSupport(browserStage);
        ResizeHelper.addResizeListener(browserStage);
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
        history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>() {
            @Override
            public void onChanged(Change<? extends WebHistory.Entry> c) {
                c.next();
                if (history.getCurrentIndex() > 0) prevButton.setDisable(false);
                //log.debug("currentIndex: " + history.getCurrentIndex() + " - num. entries: " + history.getEntries().size());
                String params = "";
                if (locationField.getText().contains("?")) {
                    params = locationField.getText().substring(locationField.getText().indexOf("?"),
                            locationField.getText().length());
                }
                WebHistory.Entry selectedEntry = history.getEntries().get(history.getEntries().size() - 1);
                log.debug("history change - selectedEntry: " + selectedEntry);
                String newURL = selectedEntry.getUrl();
                if (!newURL.contains("?")) newURL = newURL + params;
                if (history.getEntries().size() > 1 && selectedEntry.getTitle() != null) tabPane.getSelectionModel().
                        getSelectedItem().setText(selectedEntry.getTitle());
                locationField.setText(newURL);
            }
        });
        webView.getEngine().setOnError(event -> log.error(event.getMessage(), event.getException()));
        webView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        webView.getEngine().locationProperty().addListener((observable, oldValue, newValue) ->
                locationField.setText(newValue));
        webView.getEngine().setCreatePopupHandler(config -> {//handle popup windows
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
                log.debug("selectedIdx - EventType: " + event.getEventType());
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

    public static void showMessage(ResponseVS responseVS) {
        String message = responseVS.getMessage() == null? "":responseVS.getMessage();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) message = responseVS.getMessage();
        else message = ContextVS.getMessage("errorLbl") + " - " + responseVS.getMessage();
        showMessage(responseVS.getStatusCode(), message);
    }

    public static void showMessage(Integer statusCode, String message) {
        PlatformImpl.runLater(() -> {
            MessageDialog messageDialog = new MessageDialog();
            messageDialog.showMessage(statusCode, message);
        });
    }

    public static void showMessage(final String message, final Button optionButton) {
        PlatformImpl.runLater(() -> new MessageDialog().showHtmlMessage(message, optionButton));
    }

    public static void showMessage(final String message, final String caption) {
        PlatformImpl.runLater(() -> new MessageDialog().showHtmlMessage(message, caption));
    }

    public void showDocumentVS(final String signedDocumentStr, File fileParam, Map operationDocument) {
        PlatformImpl.runLater(() -> {
            DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(
                    signedDocumentStr, fileParam, operationDocument);
            Tab newTab = new Tab();
            newTab.setText(documentVSBrowserPane.getCaption());
            newTab.setContent(documentVSBrowserPane);
            newTab.setOnSelectionChanged(event -> {
                log.debug("selectedIdx DocumentVS - EventType: " + event.getEventType());
                locationField.setText("");
            });
            tabPane.getTabs().add(newTab);
            tabPane.getSelectionModel().select(newTab);
            browserStage.show();
            browserStage.toFront();
        });
    }

    public void newTab(final Pane tabContent, final String caption){
        PlatformImpl.runLater(() -> {
            Tab newTab = Utils.getTab(tabContent, caption);
            tabPane.getTabs().add(newTab);
            tabPane.getSelectionModel().select(newTab);
            browserStage.show();
            browserStage.toFront();
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

    public void execCommandJS(String jsCommand) {
        PlatformImpl.runLater(() -> {
            for(WebView webView : webViewMap.values()) {
                webView.getEngine().executeScript(jsCommand);
            }
        });
    }

    public void setCooinServerAvailable(boolean available) {
        mainOptionsPane.setCooinServerAvailable(available);
    }

    public void setVotingSystemAvailable(boolean available) {
        mainOptionsPane.setVotingSystemAvailable(available);
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