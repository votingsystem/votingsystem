package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.service.NotificationService;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSToolbar extends HBox{

    private static Logger log = Logger.getLogger(BrowserVSToolbar.class);

    private static final int MAX_CHARACTERS_TAB_CAPTION = 35;

    private TextField locationField = new TextField("");
    private TabPane tabPane;
    private Button reloadButton;
    private Button prevButton;
    private BrowserVSMenuButton menuButton;
    private AnchorPane tabPainContainer;

    public BrowserVSToolbar() {
        setSpacing(10);
        setAlignment(Pos.CENTER);
        getStyleClass().add("browser-toolbar");
        final Button forwardButton = Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.CHEVRON_RIGHT));;
        forwardButton.setOnAction((event) -> {
            try {
                ((WebView) tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().getHistory().go(1);
                prevButton.setDisable(false);
            } catch (Exception ex) { forwardButton.setDisable(true); }
        });
        prevButton =  Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.CHEVRON_LEFT));
        prevButton.setOnAction(event -> {
            try {
                ((WebView) tabPane.getSelectionModel().getSelectedItem().getContent()).getEngine().getHistory().go(-1);
                forwardButton.setDisable(false);
            } catch (Exception ex) { prevButton.setDisable(true); }
        });
        reloadButton = Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.REFRESH));
        reloadButton.setOnAction(event -> ((WebView) tabPane.getSelectionModel().getSelectedItem().getContent()).
                getEngine().load(locationField.getText()));
        prevButton.setDisable(true);
        forwardButton.setDisable(true);
        final Button newTabButton = Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.PLUS));
        newTabButton.setOnAction(event -> newTab(null, null, null));

        HBox.setHgrow(locationField, Priority.ALWAYS);
        locationField.getStyleClass().add("location-text");
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
        NotificationService.getInstance().setNotificationsButton(
                Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.INFO_CIRCLE, Utils.COLOR_YELLOW_ALERT)));
        InboxService.getInstance().setInboxButton(Utils.getToolBarButton(
                Utils.getImage(FontAwesome.Glyph.ENVELOPE, Utils.COLOR_RED_DARK)));

        menuButton = new BrowserVSMenuButton();
        menuButton.getStyleClass().add("toolbar-button");

        Button closeButton =  Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.TIMES, "#ba0011"));
        closeButton.setOnAction(actionEvent -> VotingSystemApp.getInstance().stop());

        HBox navButtonBox = new HBox();
        navButtonBox.getChildren().addAll(prevButton, forwardButton);
        getChildren().addAll(newTabButton, navButtonBox, locationField, reloadButton, Utils.createSpacer(),
                NotificationService.getInstance().getNotificationsButton(), InboxService.getInstance().getInboxButton(),
                menuButton, closeButton);
        setOnMouseClicked(mouseEvent -> BrowserVS.getInstance().toggleFullScreen());
        tabPane = new TabPane();
        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setSide(Side.TOP);
        HBox.setHgrow(tabPane, Priority.ALWAYS);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        tabPainContainer = new AnchorPane();
        AnchorPane.setTopAnchor(tabPane, 0.0);
        AnchorPane.setLeftAnchor(tabPane, 0.0);
        AnchorPane.setRightAnchor(tabPane, 0.0);
        AnchorPane.setBottomAnchor(tabPane, 0.0);
        tabPainContainer.getChildren().addAll(tabPane);
        VBox.setVgrow(tabPainContainer, Priority.ALWAYS);
        locationField.setOnMouseClicked(event -> {
            Object content = tabPane.getSelectionModel().getSelectedItem().getContent();
            if (content instanceof WebView) Utils.createHistoryMenu((WebView) content).show(locationField, Side.BOTTOM, 0, 0);
        });
    }

    public AnchorPane getTabPainContainer() {
        return tabPainContainer;
    }

    public void newTab(final Pane tabContent, final String caption){
        Tab newTab = Utils.getTab(tabContent, caption);
        newTab.setOnSelectionChanged(event -> locationField.setText("") );
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
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
                    switch (newState) {
                        case SCHEDULED:
                            reloadButton.setGraphic(Utils.getImage(FontAwesome.Glyph.COG));
                            break;
                        case SUCCEEDED:
                            reloadButton.setGraphic(Utils.getImage(FontAwesome.Glyph.REFRESH));
                            break;
                    }
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
        if(URL != null) PlatformImpl.runLater(() ->  webView.getEngine().load(URL) );
        BrowserVS.getInstance().show();
        return webView;
    }

    public TabPane getTabPane () {
        return tabPane;
    }

    public void setCooinServerAvailable(boolean available) {
        menuButton.setCooinServerAvailable(available);
    }

    public void setVotingSystemAvailable(boolean available) {
        menuButton.setVotingSystemAvailable(available);
    }

}
