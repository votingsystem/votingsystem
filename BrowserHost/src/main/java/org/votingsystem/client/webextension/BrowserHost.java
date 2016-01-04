package org.votingsystem.client.webextension;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.votingsystem.client.webextension.dialog.MainDialog;
import org.votingsystem.client.webextension.dialog.MessageDialog;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BrowserHost extends Application {

    private static Logger log = Logger.getLogger(BrowserHost.class.getSimpleName());

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static final int MAX_MESSAGE_SIZE = 1024000;

    private static BrowserHost INSTANCE;
    private Map<String, String> smimeMessageMap = new HashMap<>();
    private Map<String, org.votingsystem.model.currency.Currency.State> currencyStateMap = new HashMap<>();
    private static final Map<String, QRMessageDto> qrMessagesMap = new HashMap<>();
    private Stage primaryStage;
    private String chromeExtensionId;

    @Override public void start(final Stage primaryStage) throws Exception {
        INSTANCE = this;
        INSTANCE.primaryStage = primaryStage;
        new MainDialog(primaryStage).show();
        //this is the part the receives the messages from the browser extension
        executorService.execute(() -> {
            log.info("waiting for browser messages");
            try {
                byte[] messagePart = null;
                while(true) {
                    ByteBuffer buf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
                    ReadableByteChannel channel = Channels.newChannel(System.in);
                    channel.read(buf);
                    buf.flip();
                    byte[] bytes = null;
                    if(messagePart != null) {
                        bytes = new byte[messagePart.length + buf.limit()];
                        System.arraycopy(messagePart, 0, bytes, 0, messagePart.length);
                        System.arraycopy(buf.array(), 0, bytes, messagePart.length, buf.limit());
                    } else {
                        bytes = Arrays.copyOfRange(buf.array(), 4, buf.limit());
                    }
                    try {
                        JSON.getMapper().readValue(bytes, OperationVS.class).initProcess();
                        messagePart = null;
                    } catch (JsonMappingException ex) {
                        log.info("--- message broken waiting for part---");
                        if(messagePart == null) messagePart = Arrays.copyOfRange(buf.array(), 4, buf.limit());
                        else {
                            byte[] messageSumParts = new byte[messagePart.length  +  buf.limit()];
                            System.arraycopy(messagePart, 0, messageSumParts, 0, messagePart.length);
                            System.arraycopy(buf.array(), 0, messageSumParts, messagePart.length, buf.limit());
                            messagePart = messageSumParts;
                        }
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                }
            }catch (Exception ex) {
                log.log(Level.SEVERE,ex.getMessage(), ex);
            }
        });

        for(String param : getParameters().getRaw()) {
            URI uri = new URI(param);
            switch(uri.getScheme().toLowerCase()) {
                case "vs":
                    try {
                        JSON.getMapper().readValue(FileUtils.getBytesFromFile(new File(uri.getPath())),
                                OperationVS.class).initProcess();
                    } catch (Exception ex) {
                        log.log(Level.SEVERE,ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                    break;
                case "chrome-extension":
                    chromeExtensionId = uri.getHost();
                    break;
                default:
                    log.info("unknown schema: " + uri.getScheme());
            }
        }
        BrowserSessionService.getInstance().checkCSR();
    }


    public static void sendMessageToBrowser(MessageDto messageDto) {
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

    @Override public void stop() {
        log.info("stop");
        executorService.shutdownNow();
        System.exit(0);//Platform.exit();
    }

    public String getSMIME(String smimeMessageURL) {
        return smimeMessageMap.get(smimeMessageURL);
    }

    public void setSMIME(String smimeMessageURL, String smimeMessageStr) {
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

    public Scene getScene() {
        return primaryStage.getScene();
    }

    public void toFront() {
        PlatformImpl.runLater(() -> primaryStage.toFront());
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

    public static void main(String[] args) throws Exception {
        new ContextVS("clientToolMessages", Locale.getDefault().getLanguage()).initDirs(System.getProperty("user.home"));
        launch(args);
    }

}
