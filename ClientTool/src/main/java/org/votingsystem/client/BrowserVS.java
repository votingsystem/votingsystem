package org.votingsystem.client;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sf.json.JSON;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.pane.BrowserVSPane;
import org.votingsystem.client.pane.BrowserVSToolbar;
import org.votingsystem.client.service.NotificationService;
import org.votingsystem.client.util.*;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVS extends VBox implements WebKitHost {

    private static Logger log = Logger.getLogger(BrowserVS.class);

    private Stage browserStage;
    private FullScreenHelper fullScreenHelper;
    private BrowserVSToolbar toolBar;
    private BrowserVSPane browserHelper;
    private Map<String, WebView> webViewMap = new HashMap<String, WebView>();
    private static BrowserVS INSTANCE;

    public static BrowserVS init(Stage browserStage) {
        INSTANCE = new BrowserVS(browserStage);
        return INSTANCE;
    }

    public static BrowserVS getInstance() {
        return INSTANCE;
    }

    public void show() {
        browserStage.show();
        browserStage.toFront();
    }

    private BrowserVS(Stage browserStage) {
        getStylesheets().add(Utils.getResource("/css/browservs.css"));
        getStyleClass().add("main-dialog");
        this.browserStage = browserStage;
        fullScreenHelper = new FullScreenHelper(this.browserStage);
        browserHelper = new BrowserVSPane();
        Platform.setImplicitExit(false);
        browserHelper.getSignatureService().setOnSucceeded(event -> {
            log.debug("signatureService - OnSucceeded");
            ResponseVS responseVS = browserHelper.getSignatureService().getValue();
            if(responseVS.getStatus() != null) {
                NotificationService.getInstance().postToEventBus(responseVS);
            } else if(ResponseVS.SC_INITIALIZED == responseVS.getStatusCode()) {
                log.debug("signatureService - OnSucceeded - ResponseVS.SC_INITIALIZED");
            } else if(ContentTypeVS.JSON == responseVS.getContentType()) {
                sendMessageToBrowser(responseVS.getMessageJSON(),
                        browserHelper.getSignatureService().getOperationVS().getCallerCallback());
            } else sendMessageToBrowser(Utils.getMessageToBrowser(responseVS.getStatusCode(), responseVS.getMessage()),
                    browserHelper.getSignatureService().getOperationVS().getCallerCallback());
        });
        browserStage.setTitle(ContextVS.getMessage("mainDialogCaption"));
        browserStage.setResizable(true);
        browserStage.setOnCloseRequest(event -> {
            event.consume();
            browserStage.hide();
            browserHelper.getSignatureService().cancel();
            log.debug("browserStage.setOnCloseRequest");
        });
        browserHelper.getChildren().add(0, this);
        browserStage.setScene(new Scene(browserHelper));
        browserStage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
        browserStage.initStyle(StageStyle.UNDECORATED);
        Utils.addMouseDragSupport(browserStage);
        ResizeHelper.addResizeListener(browserStage);
        toolBar = new BrowserVSToolbar();
        setMargin(toolBar, new Insets(6, 6, 6, 6));
        getChildren().addAll(toolBar, toolBar.getTabPainContainer());
        NotificationService.getInstance().showIfPendingNotifications();
    }

    @Override public void sendMessageToBrowser(JSON messageJSON, String callerCallback) {
        String message = messageJSON.toString();
        log.debug("sendMessageToBrowser - messageJSON: " + MsgUtils.truncateLog(message));
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
        if(signalData.containsKey("caption")) toolBar.getTabPane().getSelectionModel().getSelectedItem().setText(
                (String)signalData.get("caption"));
    }



    public void toggleFullScreen() {
        fullScreenHelper.toggleFullScreen();
    }

    public void openCooinURL(final String URL, final String caption) {
        log.debug("openCooinURL: " + URL);
        if(ContextVS.getInstance().getCooinServer() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"), ContextVS.getMessage("errorLbl"));
        } else Platform.runLater(() -> toolBar.newTab(URL, caption, null));
    }

    public void openVotingSystemURL(final String URL, final String caption) {
        log.debug("openVotingSystemURL: " + URL);
        if(ContextVS.getInstance().getAccessControl() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"), ContextVS.getMessage("errorLbl"));
        } else Platform.runLater(() -> toolBar.newTab(URL, caption, null));
    }

    public static void showMessage(ResponseVS responseVS) {
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) showMessage(responseVS.getStatusCode(), responseVS.getMessage());
        else showMessage(responseVS.getStatusCode(), ContextVS.getMessage("errorLbl") + " - " + responseVS.getMessage());
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

    public void newTab(String URL, String tabCaption, String jsCommand) {
        toolBar.newTab(URL, tabCaption, jsCommand);
    }

    public void newTab(final Pane tabContent, final String caption){
        PlatformImpl.runLater(() -> {
            toolBar.newTab(tabContent, caption);
            show();
        });
    }

    public void execCommandJS(String jsCommand) {
        PlatformImpl.runLater(() -> {
            for(WebView webView : webViewMap.values()) {
                webView.getEngine().executeScript(jsCommand);
            }
        });
    }

    public void setCooinServerAvailable(boolean available) {
        toolBar.setCooinServerAvailable(available);
    }

    public void setVotingSystemAvailable(boolean available) {
        toolBar.setVotingSystemAvailable(available);
    }

    public void execCommandJSCurrentView(String jsCommand) {
        PlatformImpl.runLater(() -> {
            ((WebView)toolBar.getTabPane().getSelectionModel().getSelectedItem().getContent()).getEngine().executeScript(jsCommand);
        });
    }

    public void registerCallerCallbackView(String callerCallback, WebView webView) {
        webViewMap.put(callerCallback, webView);
    }

}