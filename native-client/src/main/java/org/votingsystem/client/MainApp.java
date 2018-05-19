package org.votingsystem.client;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.votingsystem.client.dialog.MainDialog;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.StringUtils;
import org.votingsystem.xml.XML;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainApp extends Application {

    private static Logger log = Logger.getLogger(MainApp.class.getName());

    private String votingServiceEntityId;
    private String identityServiceEntityId;
    private String currencyServiceEntityId;
    private String applicationDirPath;
    private String tempDir;


    private static MainApp INSTANCE;
    private Map<String, String> signedDocumentMap = new HashMap<>();
    private Map<String, Currency.State> currencyStateMap = new HashMap<>();
    private static final Map<String, MetadataDto> systemEntityMap = new HashMap<>();


    private X509Certificate timestampServerCert;
    private Stage primaryStage;
    private boolean debugSession = false;

    @Override public void start(final Stage primaryStage) throws Exception {
        try {
            INSTANCE = this;
            INSTANCE.primaryStage = primaryStage;
            applicationDirPath = System.getProperty("votingsystem_native_client");
            if(StringUtils.isEmpty(applicationDirPath))
                applicationDirPath = System.getProperty("user.home");
            File tempDirFile = new File(applicationDirPath + File.separator + "votingsystem-native-client");
            log.info("applicationDirPath: " + applicationDirPath);
            tempDir = tempDirFile.getAbsolutePath();
            new MainDialog(primaryStage).show();
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}

    }

    @Override public void stop() {
        log.info("stop");
        System.exit(0);//Platform.exit();
    }

    public String getVotingServiceEntityId() {
        return votingServiceEntityId;
    }

    public MainApp setVotingServiceEntityId(String votingServiceEntityId) {
        this.votingServiceEntityId = votingServiceEntityId;
        return this;
    }

    public String getIdentityServiceEntityId() {
        return identityServiceEntityId;
    }

    public MainApp setIdentityServiceEntityId(String identityServiceEntityId) {
        this.identityServiceEntityId = identityServiceEntityId;
        return this;
    }

    public String getCurrencyServiceEntityId() {
        return currencyServiceEntityId;
    }

    public MainApp setCurrencyServiceEntityId(String currencyServiceEntityId) {
        this.currencyServiceEntityId = currencyServiceEntityId;
        return this;
    }

    public String getSignedDocument(String cmsMessageURL) {
        return signedDocumentMap.get(cmsMessageURL);
    }

    public void setSignedDocument(String cmsMessageURL, String cmsMessageStr) {
        signedDocumentMap.put(cmsMessageURL, cmsMessageStr);
    }

    public void putCurrencyState(String hashCertVS, Currency.State state) {
        currencyStateMap.put(hashCertVS, state);
    }

    public static MainApp instance() {
        return INSTANCE;
    }

    public Scene getScene() {
        return primaryStage.getScene();
    }

    public void toFront() {
        PlatformImpl.runLater(() -> primaryStage.toFront());
    }

    public static void showMessage(Integer statusCode, String message) {
        PlatformImpl.runLater(() ->  new MessageDialog(instance().getScene().getWindow()).showMessage(statusCode, message));
    }

    public static MessageDialog showMessage(final String caption, final String message, final Parent parent, final Window parentWindow) {
        final ResponseDto<MessageDialog> responseVS = ResponseDto.OK();
        PlatformImpl.runAndWait(() -> {
            MessageDialog messageDialog = new MessageDialog(parentWindow != null ? parentWindow : instance().getScene().getWindow());
            messageDialog.showHtmlMessage(caption, message, parent);
            responseVS.setData(messageDialog);
        });
        return responseVS.getData();
    }

    public static void showMessage(final String message, final String caption) {
        PlatformImpl.runLater(() -> new MessageDialog(instance().getScene().getWindow()).showHtmlMessage(message, caption));
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    public MetadataDto getSystemEntity(final String entityId, boolean forceHTTPLoad) {
        MetadataDto result = systemEntityMap.get(entityId);
        if(result == null && forceHTTPLoad) {
            try {
                result = getSystemEntityFromURL(CurrencyOperation.GET_METADATA.getUrl(entityId));
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    private MetadataDto getSystemEntityFromURL(String entityId) throws Exception {
        ResponseDto responseDto = HttpConn.getInstance().doGetRequest(entityId, MediaType.XML);
        log.severe("validate metadata signature!!!");
        MetadataDto systemEntity = new XML().getMapper().readValue(responseDto.getMessageBytes(), MetadataDto.class);
        systemEntityMap.put(entityId, systemEntity);
        return systemEntity;
    }


    public boolean isDebugSession() {
        return debugSession;
    }

    public String getTempDir() {
        return tempDir;
    }

    public X509Certificate getTimestampServerCert() {
        return timestampServerCert;
    }

    public void setTimestampServerCert(X509Certificate timestampServerCert) {
        this.timestampServerCert = timestampServerCert;
    }
}