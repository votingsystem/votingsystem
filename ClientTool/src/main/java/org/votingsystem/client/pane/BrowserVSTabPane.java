package org.votingsystem.client.pane;

import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Side;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.votingsystem.client.Browser;
import org.votingsystem.client.service.BrowserSessionService;
import org.votingsystem.client.util.BrowserVSClient;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSTabPane extends TabPane {

    private static Logger log = Logger.getLogger(BrowserVSTabPane.class.getSimpleName());

    private static final int MAX_CHARACTERS_TAB_CAPTION = 35;
    private static final String TAB_CAPTION_EMPTY = "                ";

    private BrowserVSToolbar toolbar;

    public BrowserVSTabPane(BrowserVSToolbar toolbar) {
        this.toolbar = toolbar;
        this.toolbar.getLocationField().setOnMouseClicked(event -> {
            if(getSelectionModel().getSelectedItem() != null) {
                Object content = getSelectionModel().getSelectedItem().getContent();
                if (content instanceof WebView) Utils.createHistoryMenu((WebView) content).show(
                        toolbar.getLocationField(), Side.BOTTOM, 0, 0);
            }
        });
        this.toolbar.getLocationField().setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                if (!"".equals(this.toolbar.getLocationField().getText())) {
                    String targetURL = null;
                    if (this.toolbar.getLocationField().getText().startsWith("http://") ||
                            this.toolbar.getLocationField().getText().startsWith("https://")) {
                        targetURL = this.toolbar.getLocationField().getText().trim();
                    } else targetURL = "http://" + this.toolbar.getLocationField().getText().trim();
                    Object content = null;
                    if (getSelectionModel().getSelectedItem() != null) content =
                            getSelectionModel().getSelectedItem().getContent();
                    if (content instanceof WebView) ((WebView) content).getEngine().load(targetURL);
                    else newTab(targetURL, null, null);
                }
            }
        });
        this.toolbar.getForwardButton().setOnAction((event) -> {
            try {
                ((WebView) getSelectionModel().getSelectedItem().getContent()).getEngine().getHistory().go(1);
                this.toolbar.getPrevButton().setDisable(false);
            } catch (Exception ex) {
                this.toolbar.getForwardButton().setDisable(true);
            }
        });
        this.toolbar.getPrevButton().setOnAction(event -> {
            try {
                ((WebView) getSelectionModel().getSelectedItem().getContent()).getEngine().getHistory().go(-1);
                toolbar.getForwardButton().setDisable(false);
            } catch (Exception ex) {
                toolbar.getPrevButton().setDisable(true);
            }
        });
        this.toolbar.getReloadButton().setOnAction(event -> {
            if(getSelectionModel().getSelectedItem() != null) {
                ((WebView) getSelectionModel().getSelectedItem().getContent()).
                        getEngine().load(this.toolbar.getLocationField().getText());
            }
        });
        setRotateGraphic(false);
        setTabClosingPolicy(TabClosingPolicy.SELECTED_TAB);
        setSide(Side.TOP);
        HBox.setHgrow(this, Priority.ALWAYS);
        VBox.setVgrow(this, Priority.ALWAYS);
    }

    public void newTab(final Pane tabContent, final String caption){
        Tab newTab = Utils.getTab(tabContent, caption);
        newTab.setOnSelectionChanged(event -> toolbar.getLocationField().setText("") );
        getTabs().add(newTab);
        getSelectionModel().select(newTab);
    }

    public WebView newTab(String URL, String tabCaption, String jsCommand) {
        final WebView webView = new WebView();
        if(jsCommand != null) {
            webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newState) -> {
                //log.info("newState: " + newState);
                if (newState == Worker.State.SUCCEEDED) webView.getEngine().executeScript(jsCommand.toString());
            });
        }
        final WebHistory history = webView.getEngine().getHistory();
        history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>() {
            @Override
            public void onChanged(Change<? extends WebHistory.Entry> c) {
                c.next();
                if (history.getCurrentIndex() > 0) toolbar.getPrevButton().setDisable(false);
                //log.info("currentIndex: " + history.getCurrentIndex() + " - num. entries: " + history.getEntries().size());
                String params = "";
                if (toolbar.getLocationField().getText().contains("?")) {
                    params = toolbar.getLocationField().getText().substring(toolbar.getLocationField().getText().indexOf("?"),
                            toolbar.getLocationField().getText().length());
                }
                WebHistory.Entry selectedEntry = history.getEntries().get(history.getEntries().size() - 1);
                log.info("history change - selectedEntry: " + selectedEntry);
                String newURL = selectedEntry.getUrl();
                if (!newURL.contains("?")) newURL = newURL + params;
                if (history.getEntries().size() > 1 && selectedEntry.getTitle() != null) getSelectionModel().
                        getSelectedItem().setText(selectedEntry.getTitle());
                toolbar.getLocationField().setText(newURL);
            }
        });
        webView.getEngine().setOnError(event -> log.log(Level.SEVERE, event.getMessage(), event.getException()));
        webView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        webView.getEngine().locationProperty().addListener((observable, oldValue, newValue) ->
                toolbar.getLocationField().setText(newValue));
        webView.getEngine().setCreatePopupHandler(config -> {//handle popup windows
            return newTab(null, null, null).getEngine();
        });
        VBox.setVgrow(webView, Priority.ALWAYS);
        Tab newTab = new Tab();
        webView.getEngine().getLoadWorker().stateProperty().addListener(
            new ChangeListener<Worker.State>() {
                @Override public void changed(ObservableValue<? extends Worker.State> ov,
                                              Worker.State oldState, Worker.State newState) {
                    //log.info("newState: " + newState + " - " + webView.getEngine().getLocation());
                    switch (newState) {
                        case SCHEDULED:
                            toolbar.getReloadButton().setGraphic(Utils.getIcon(FontAwesomeIcon.COG));
                            break;
                        case SUCCEEDED:
                            if(tabCaption == null && URL != null) newTab.setText(TAB_CAPTION_EMPTY);
                            toolbar.getReloadButton().setGraphic(Utils.getIcon(FontAwesomeIcon.REFRESH));
                            Document doc = webView.getEngine().getDocument();
                            Element element = doc.getElementById("voting_system_page");
                            if(element != null) {
                                JSObject win = (JSObject) webView.getEngine().executeScript("window");
                                win.setMember("clientTool", new BrowserVSClient(webView));
                                Browser.getInstance().fireCoreSignal("vs-session-data",
                                        BrowserSessionService.getInstance().getBrowserSessionData(), true);
                                Browser.getInstance().runJSCommandCurrentView("setClientToolConnected()");
                            }
                            break;
                        case FAILED:
                            toolbar.getReloadButton().setGraphic(Utils.getIcon(FontAwesomeIcon.REFRESH));
                            showMessage(new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("connectionErrorMsg")));
                            break;
                    }
                }
            }
        );
        newTab.setOnSelectionChanged(event -> {
            log.info("selectedIdx - EventType: " + event.getEventType());
            int selectedIdx = getSelectionModel().getSelectedIndex();
            ObservableList<WebHistory.Entry> entries = ((WebView)getSelectionModel().getSelectedItem().
                    getContent()).getEngine().getHistory().getEntries();
            if(entries.size() > 0){
                WebHistory.Entry selectedEntry = entries.get(entries.size() -1);
                if(entries.size() > 1 &&  selectedEntry.getTitle() != null) newTab.setText(selectedEntry.getTitle());
                log.info("selectedIdx: " + selectedIdx + " - selectedEntry: " + selectedEntry);
                toolbar.getLocationField().setText(selectedEntry.getUrl());
            }
        });
        if(tabCaption != null) newTab.setText(tabCaption.length() > MAX_CHARACTERS_TAB_CAPTION ?
                tabCaption.substring(0, MAX_CHARACTERS_TAB_CAPTION) + "...":tabCaption);
        else if(URL != null) newTab.setText(ContextVS.getMessage("loadingLbl") + " ...");
        else newTab.setText(TAB_CAPTION_EMPTY);
        newTab.setContent(webView);
        getTabs().add(newTab);
        getSelectionModel().select(newTab);
        if(URL != null) PlatformImpl.runLater(() -> webView.getEngine().load(URL));
        Browser.getInstance().show();
        return webView;
    }

}
