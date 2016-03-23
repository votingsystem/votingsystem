package org.votingsystem.client;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.votingsystem.client.dialog.MainDialog;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.ContextVS;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApp extends Application {

    private static Logger log = Logger.getLogger(MainApp.class.getName());

    private static MainApp INSTANCE;
    private Map<String, String> cmsMessageMap = new HashMap<>();
    private Map<String, org.votingsystem.model.currency.Currency.State> currencyStateMap = new HashMap<>();
    private Stage primaryStage;
    private boolean debugSession = false;

    @Override public void start(final Stage primaryStage) throws Exception {
        try {
            INSTANCE = this;
            INSTANCE.primaryStage = primaryStage;
            new MainDialog(primaryStage).show();
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}

    }

    @Override public void stop() {
        log.info("stop");
        System.exit(0);//Platform.exit();
    }

    public String getCMS(String cmsMessageURL) {
        return cmsMessageMap.get(cmsMessageURL);
    }

    public void setCMS(String cmsMessageURL, String cmsMessageStr) {
        cmsMessageMap.put(cmsMessageURL, cmsMessageStr);
    }

    public void putCurrencyState(String hashCertVS, Currency.State state) {
        currencyStateMap.put(hashCertVS, state);
    }

    public static MainApp getInstance() {
        return INSTANCE;
    }

    public Scene getScene() {
        return primaryStage.getScene();
    }

    public void toFront() {
        PlatformImpl.runLater(() -> primaryStage.toFront());
    }

    public static void showMessage(Integer statusCode, String message) {
        PlatformImpl.runLater(() ->  new MessageDialog(getInstance().getScene().getWindow()).showMessage(statusCode, message));
    }

    public static MessageDialog showMessage(final String caption, final String message, final Parent parent, final Window parentWindow) {
        final ResponseVS<MessageDialog> responseVS = ResponseVS.OK();
        PlatformImpl.runAndWait(() -> {
            MessageDialog messageDialog = new MessageDialog(parentWindow != null ? parentWindow : getInstance().getScene().getWindow());
            messageDialog.showHtmlMessage(caption, message, parent);
            responseVS.setData(messageDialog);
        });
        return responseVS.getData();
    }

    public static void showMessage(final String message, final String caption) {
        PlatformImpl.runLater(() -> new MessageDialog(getInstance().getScene().getWindow()).showHtmlMessage(message, caption));
    }

    public static void main(String[] args) throws Exception {
        new ContextVS("clientToolMessages", Locale.getDefault().getLanguage()).initDirs(System.getProperty("user.home"));
        launch(args);
    }

    public boolean isDebugSession() {
        return debugSession;
    }

}
