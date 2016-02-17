package org.votingsystem.client.webextension;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.dialog.DebugDialog;
import org.votingsystem.client.webextension.dialog.MainDialog;
import org.votingsystem.client.webextension.dialog.MessageDialog;
import org.votingsystem.client.webextension.util.EventBusTransactionResponseListener;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.service.EventBusService;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.currency.CurrencyCheckResponse;
import org.votingsystem.util.currency.Wallet;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toSet;

public class BrowserHost extends Application {

    private static Logger log = Logger.getLogger(BrowserHost.class.getName());

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static final int MAX_MESSAGE_SIZE = 1024000;

    private static BrowserHost INSTANCE;
    private Map<String, String> smimeMessageMap = new HashMap<>();
    private Map<String, org.votingsystem.model.currency.Currency.State> currencyStateMap = new HashMap<>();
    private static final Map<String, QRMessageDto> qrMessagesMap = new HashMap<>();
    private Wallet wallet;
    private Stage primaryStage;
    private String chromeExtensionId;
    private boolean debugSession = false;

    @Override public void start(final Stage primaryStage) throws Exception {
        try {
            INSTANCE = this;
            INSTANCE.primaryStage = primaryStage;
            new MainDialog(primaryStage).show();
            //this is the part the receives the messages from the browser extension
            executorService.execute(() -> {
                log.info("waiting for browser messages");
                try {
                    byte[] messagePart = null;
                    ByteBuffer buffer = null;
                    ReadableByteChannel channel = null;
                    while(true) {
                        buffer = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
                        channel = Channels.newChannel(System.in);
                        channel.read(buffer);
                        buffer.flip();
                        byte[] bytes = null;
                        if(messagePart != null) {
                            bytes = new byte[messagePart.length + buffer.limit()];
                            System.arraycopy(messagePart, 0, bytes, 0, messagePart.length);
                            System.arraycopy(buffer.array(), 0, bytes, messagePart.length, buffer.limit());
                        } else {
                            bytes = Arrays.copyOfRange(buffer.array(), 4, buffer.limit());
                        }
                        try {
                            JSON.getMapper().readValue(bytes, OperationVS.class).initProcess();
                            messagePart = null;
                        } catch (JsonMappingException ex) {
                            log.info("--- message broken waiting for part---");
                            if(messagePart == null) messagePart = Arrays.copyOfRange(buffer.array(), 4, buffer.limit());
                            else {
                                byte[] messageSumParts = new byte[messagePart.length  +  buffer.limit()];
                                System.arraycopy(messagePart, 0, messageSumParts, 0, messagePart.length);
                                System.arraycopy(buffer.array(), 0, messageSumParts, messagePart.length, buffer.limit());
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

            try {
                for(String param : getParameters().getRaw()) {
                    URI uri = new URI(param);
                    if(uri.getScheme() != null) {
                        switch(uri.getScheme().toLowerCase()) {
                            case "vs":
                                JSON.getMapper().readValue(FileUtils.getBytesFromFile(new File(uri.getPath())),
                                        OperationVS.class).initProcess();
                                break;
                            case "chrome-extension":
                                chromeExtensionId = uri.getHost();
                                break;
                            default:
                                log.info("unknown schema: " + uri.getScheme());
                        }
                    } else {
                        if("debugSession".equals(param)) {
                            this.debugSession = true;
                            DebugDialog.showDialog();
                        }
                    }
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
            EventBusService.getInstance().register(new EventBusTransactionResponseListener());
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
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

    public Set<Currency> getWalletCurrencySet() {
        return wallet.getCurrencySet();
    }

    public boolean isWalletLoaded() {
        return (wallet!= null);
    }

    public void saveToWallet (Set<Currency> currencyToAddSet, char[] password) throws Exception {
        loadWallet(password);
        wallet.saveToWallet(currencyToAddSet, password);
    }

    public void createEmptyWallet (char[] password) {
        try {
            Wallet wallet = new Wallet();
            wallet.createWallet(new HashSet<>(), password);
        } catch (Exception ex) { log.log(Level.SEVERE,ex.getMessage(), ex); }
    }

    public Set<Currency> loadWallet(char[] password) {
        try {
            wallet = wallet.load(password);
            Set<Currency> currencySet = wallet.getCurrencySet();
            Future<CurrencyCheckResponse> future = executorService.submit(() -> Wallet.validateWithServer(currencySet));
            CurrencyCheckResponse response = future.get();
            if(ResponseVS.SC_OK == response.getStatusCode()) {
                return currencySet;
            } else {
                AtomicBoolean walletRestored = new AtomicBoolean(false);
                if(!response.getCurrencyWithErrorSet().isEmpty()) {
                    Button deleteButton = new Button(ContextVS.getMessage("deleteCurrencyWithErrorsLbl"),
                            Utils.getIcon(FontAwesome.Glyph.TIMES));
                    deleteButton.setOnAction(event -> {
                        Set<String> hashCertToRemoveSet = response.getCurrencyWithErrorSet().stream().map(
                                currency -> {return currency.getHashCertVS();}).collect(toSet());
                        try {
                            wallet.removeSet(hashCertToRemoveSet, password);
                            walletRestored.set(true);
                        } catch (Exception ex) {
                            log.log(Level.SEVERE, ex.getMessage(), ex);
                            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                        }
                    });
                    MessageDialog messageDialog = showMessage(ContextVS.getMessage("currencyValidationErrorlbl"),
                            response.getErrorMessage(), deleteButton, null);
                    messageDialog.addCloseListener(event -> {if(!walletRestored.get()) wallet = null;});
                }
                return null;
            }
        } catch (WalletException wex) {
            Utils.showWalletNotFoundMessage();
            return null;
        }  catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            wallet = null;
            return null;
        }
    }

    public boolean isDebugSession() {
        return debugSession;
    }

}
