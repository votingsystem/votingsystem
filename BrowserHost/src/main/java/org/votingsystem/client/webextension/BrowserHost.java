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
import org.votingsystem.client.webextension.dialog.*;
import org.votingsystem.client.webextension.pane.DocumentVSBrowserPane;
import org.votingsystem.client.webextension.pane.WalletPane;
import org.votingsystem.client.webextension.service.*;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.client.webextension.util.OperationVS;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.currency.Wallet;

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BrowserHost extends Application implements PasswordDialog.Listener {

    private static Logger log = Logger.getLogger(BrowserHost.class.getSimpleName());

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static BrowserHost INSTANCE;
    private Map<String, String> smimeMessageMap;
    private Map<String, org.votingsystem.model.currency.Currency.State> currencyStateMap = new HashMap<>();
    private static final Map<String, QRMessageDto> qrMessagesMap = new HashMap<>();
    private String accessControlServerURL;
    private String currencyServerURL;
    private Stage primaryStage;

    private OperationVS operationVS;
    private OperationService operationService = new OperationService();

    public String getSMIME(String smimeMessageURL) {
        if(smimeMessageMap ==  null) return null;
        else return smimeMessageMap.get(smimeMessageURL);
    }

    public void setSMIME(String smimeMessageURL, String smimeMessageStr) {
        if(smimeMessageMap ==  null) {
            smimeMessageMap = new HashMap<>();
        }
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

    public String getAccessControlServerURL() {
        return accessControlServerURL;
    }

    public Scene getScene() {
        return primaryStage.getScene();
    }

    @Override public void start(final Stage primaryStage) throws Exception {
        INSTANCE = this;
        INSTANCE.primaryStage = primaryStage;
        //dummy initilization of the stage in order to be available to other UI component
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(new Scene(new Group(), 1, 1));
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

    public void processOperationWithPassword(OperationVS operationVS, final String passwordDialogMessage) {
        this.operationVS = operationVS;
        if(CryptoTokenVS.MOBILE != BrowserSessionService.getCryptoTokenType()) {
            PlatformImpl.runAndWait(() ->
                    PasswordDialog.showWithoutPasswordConfirm(operationVS.getType(), this, passwordDialogMessage));
        } else operationService.processOperationVS(null, operationVS);
    }

    public void saveWallet() {
        PasswordDialog.showWithoutPasswordConfirm(TypeVS.WALLET_SAVE, this, ContextVS.getMessage("walletPinMsg"));
    }

    @Override public void setPassword(TypeVS passwordType, char[] password) {
        switch (passwordType) {
            case WALLET_SAVE:
                if(password != null) {
                    try {
                        Wallet.getWallet(password);
                        sendMessageToBrowser(MessageDto.SIGNAL(ResponseVS.SC_OK, "vs-wallet-save"));
                        InboxService.getInstance().removeMessagesByType(TypeVS.CURRENCY_IMPORT);
                    } catch (WalletException wex) {
                        Utils.showWalletNotFoundMessage();
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                }
                break;
            default:
                operationService.processOperationVS(password, operationVS);
        }
    }

    public static void main(String[] args) throws Exception {
        ContextVS.initSignatureClient("clientToolMessages", Locale.getDefault().getLanguage());
        if(args.length > 0) ContextVS.getInstance().initDirs(args[0]);
        launch(args);
    }

    private void processMessageToHost(byte[] messageBytes) throws Exception {
        log.info("processMessageToHost: " + new String(messageBytes));
        operationVS = JSON.getMapper().readValue(messageBytes, OperationVS.class);
        switch (operationVS.getType()) {
            case CONNECT:
                WebSocketAuthenticatedService.getInstance().setConnectionEnabled(true);
                break;
            case DISCONNECT:
                WebSocketAuthenticatedService.getInstance().setConnectionEnabled(false);
                break;
            case FILE_FROM_URL:
                operationService.processOperationVS(null, operationVS);
                break;
            case MESSAGEVS_TO_DEVICE:
                WebSocketService.getInstance().sendMessage(JSON.getMapper().writeValueAsString(operationVS));
                break;
            case KEYSTORE_SELECT:
                Utils.selectKeystoreFile(operationVS);
                break;
            case SELECT_IMAGE:
                Utils.selectImage(operationVS);
                break;
            case OPEN_SMIME:
                String smimeMessageStr = new String(Base64.getDecoder().decode(
                        operationVS.getMessage().getBytes()), "UTF-8");
                DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(smimeMessageStr, null);
                new DialogVS(documentVSBrowserPane, null).setCaption(documentVSBrowserPane.getCaption()).show();
                break;
            case CURRENCY_OPEN:
                CurrencyDialog.show((Currency) ObjectUtils.deSerializeObject((operationVS.getMessage()).getBytes()),
                        BrowserHost.getInstance().getScene().getWindow());
                break;
            case OPEN_SMIME_FROM_URL:
                operationService.processOperationVS(null, operationVS);
                break;
            case REPRESENTATIVE_ACCREDITATIONS_REQUEST:
                RepresentativeAccreditationsDialog.show(operationVS);
                break;
            case REPRESENTATIVE_VOTING_HISTORY_REQUEST:
                RepresentativeVotingHistoryDialog.show(operationVS);
                break;
            case SAVE_SMIME:
                Utils.saveReceipt(operationVS);
                break;
            case SEND_ANONYMOUS_DELEGATION:
                Utils.saveReceiptAnonymousDelegation(operationVS);
                break;
            case CERT_USER_NEW:
                INSTANCE.processOperationWithPassword(operationVS, ContextVS.getMessage("newCertPasswDialogMsg"));
                break;
            case WALLET_OPEN:
                WalletPane.showDialog();
                break;
            case VOTING_PUBLISHING:
                ElectionEditorDialog.show(operationVS);
                break;
            case NEW_REPRESENTATIVE:
            case EDIT_REPRESENTATIVE:
                RepresentativeEditorDialog.show(operationVS);
                break;
            case CURRENCY_GROUP_NEW:
            case CURRENCY_GROUP_EDIT:
                GroupVSEditorDialog.show(operationVS);
                break;
            case WALLET_SAVE:
                INSTANCE.saveWallet();
                break;
            case MESSAGEVS:
                if(operationVS.getDocumentToSign() != null) INSTANCE.processOperationWithPassword(operationVS, null);
                else operationService.processOperationVS(null, operationVS);
                break;
            case REPRESENTATIVE_STATE:
                sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(ResponseVS.SC_OK,
                        null, operationVS.getCallerCallback(),
                        BrowserSessionService.getInstance().getRepresentationState()));
                break;
            default:
                INSTANCE.processOperationWithPassword(operationVS, null);
        }
    }

}
