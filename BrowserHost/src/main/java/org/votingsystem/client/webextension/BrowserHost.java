package org.votingsystem.client.webextension;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.votingsystem.client.webextension.dialog.MessageDialog;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.OperationVS;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

public class BrowserHost extends Application {

    private static Logger log = Logger.getLogger(BrowserHost.class.getSimpleName());

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static BrowserHost INSTANCE;
    private Map<String, String> smimeMessageMap;
    private Map<String, org.votingsystem.model.currency.Currency.State> currencyStateMap = new HashMap<>();
    private static final Map<String, QRMessageDto> qrMessagesMap = new HashMap<>();
    private String accessControlServerURL;
    private String currencyServerURL;
    private Stage primaryStage;

    public String getSMIME(String smimeMessageURL) {
        if(smimeMessageMap ==  null) return null;
        else return smimeMessageMap.get(smimeMessageURL);
    }

    public void setSMIME(String smimeMessageURL, String smimeMessageStr) {
        if(smimeMessageMap ==  null) smimeMessageMap = new HashMap<>();
        smimeMessageMap.put(smimeMessageURL, smimeMessageStr);
    }

    public void putQRMessage(QRMessageDto messageDto) {
        qrMessagesMap.put(messageDto.getUUID(), messageDto);
    }

    public QRMessageDto getQRMessage(String uuid) {
        return qrMessagesMap.get(uuid);
    }

    public QRMessageDto removeQRMessage(String uuid) {
        return qrMessagesMap.remove(uuid);
    }

    public void putCurrencyState(String hashCertVS, Currency.State state) {
        currencyStateMap.put(hashCertVS, state);
    }

    public static BrowserHost getInstance() {
        return INSTANCE;
    }

    public String getCurrencyServerURL() {
        return currencyServerURL;
    }

    public Scene getScene() {
        return primaryStage.getScene();
    }

    @Override public void start(final Stage primaryStage) throws Exception {
        INSTANCE = this;
        INSTANCE.primaryStage = primaryStage;
        //dummy initilization of the stage in order to be available to other UI component
        //primaryStage.initStyle(StageStyle.TRANSPARENT);
        //primaryStage.setScene(new Scene(new Group(), 1, 1));
        primaryStage.setScene(new Scene(new Group(), 100, 100));
        primaryStage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        primaryStage.show();

        new Thread(() -> {
            boolean loadedFromJar = false;
            if(BrowserHost.class.getResource(BrowserHost.this.getClass().getSimpleName() + ".class").
                    toString().contains("jar:file")) {
                loadedFromJar = true;
            }
            log.info("start - loadedFromJar: " + loadedFromJar + " - JavaFX version: " +
                    com.sun.javafx.runtime.VersionInfo.getRuntimeVersion());
            if(loadedFromJar) {
                accessControlServerURL = ContextVS.getMessage("prodAccessControlServerURL");
                currencyServerURL = ContextVS.getMessage("prodCurrencyServerURL");
            } else {
                accessControlServerURL = ContextVS.getMessage("devAccessControlServerURL");
                currencyServerURL = ContextVS.getMessage("devCurrencyServerURL");
            }
            ResponseVS responseVS = null;
            try {
                responseVS = Utils.checkServer(accessControlServerURL);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    ContextVS.getInstance().setAccessControl((AccessControlVS) responseVS.getData());
                    BrowserSessionService.getInstance().checkCSRRequest();
                }
            } catch(Exception ex) {log.log(Level.SEVERE,ex.getMessage(), ex);}
            try {
                responseVS = Utils.checkServer(currencyServerURL);
                ContextVS.getInstance().setCurrencyServer((CurrencyServer) responseVS.getData());
            } catch(Exception ex) {
                log.log(Level.SEVERE,ex.getMessage());
            }
        }).start();
        //this is the part the receives the messages from the browser extension
        executorService.execute(() -> {
            log.info("waiting for browser messages");
            try {
                while(true) {
                    ByteBuffer buf = ByteBuffer.allocate(100000);
                    ReadableByteChannel channel = Channels.newChannel(System.in);
                    channel.read(buf);
                    buf.flip();
                    byte[] bytes = Arrays.copyOfRange(buf.array(), 4, buf.limit());
                    processMessageToHost(bytes);
                }
            }catch (Exception ex) {
                log.log(Level.SEVERE,ex.getMessage(), ex);
            }
        });
        //processMessageToHost(ContextVS.getInstance().getResourceBytes("test.json"));
    }

    @Override public void stop() {
        log.info("stop");
        executorService.shutdownNow();
        System.exit(0);//Platform.exit();
    }

    public static void sendMessageToBrowser(MessageDto messageDto) {
        log.info("sendMessageToBrowser");
        try {
            String base64msg = Base64.getEncoder().encodeToString(JSON.getMapper().writeValueAsBytes(messageDto) );
            Map<String,String> map = new HashMap<>();
            map.put("native_message", base64msg);
            byte[] messageBytes = JSON.getMapper().writeValueAsBytes(map);
            System.out.write(MsgUtils.getWebExtensionMessagePrefix(messageBytes.length));
            System.out.write(messageBytes);
            System.out.flush();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            showMessage(ex.getMessage(), ContextVS.getMessage("errorLbl"));
        }
    }

    public static void showMessage(ResponseVS responseVS) {
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) showMessage(responseVS.getStatusCode(), responseVS.getMessage());
        else showMessage(responseVS.getStatusCode(), responseVS.getMessage());
    }

    public static void showMessage(Integer statusCode, String message) {
        PlatformImpl.runLater(() ->  new MessageDialog(getInstance().getScene().getWindow()).showMessage(statusCode, message));
    }

    public static void showMessage(final String message, final Button optionButton) {
        PlatformImpl.runLater(() -> new MessageDialog(getInstance().getScene().getWindow()).showHtmlMessage(message, optionButton));
    }

    public static void showMessage(final String message, final Parent parent, Window parentWindow) {
        PlatformImpl.runLater(() -> new MessageDialog(parentWindow).showHtmlMessage(message, parent));
    }

    public static void showMessage(final String message, final String caption) {
        PlatformImpl.runLater(() -> new MessageDialog(getInstance().getScene().getWindow()).showHtmlMessage(message, caption));
    }

    private void processMessageToHost(byte[] messageBytes) {
        log.info("processMessageToHost: " + new String(messageBytes));
        try {
            OperationVS operationVS = JSON.getMapper().readValue(messageBytes, OperationVS.class);
            operationVS.initProcess();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        ContextVS.initSignatureClient("clientToolMessages", Locale.getDefault().getLanguage());

        FileHandler fileHandler = new FileHandler(new File(ContextVS.APPDIR + "/app.log").getAbsolutePath());
        fileHandler.setFormatter(new SimpleFormatter());
        Logger.getLogger("").addHandler(fileHandler);

        if(args.length > 0) ContextVS.getInstance().initDirs(args[0]);
        launch(args);
    }

}
