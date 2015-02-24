package org.votingsystem.client.service;

import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.dialog.InboxDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.util.InboxDecryptTask;
import org.votingsystem.client.util.InboxMessage;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.Wallet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toList;
import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxService {

    private static Logger log = Logger.getLogger(InboxService.class);

    public static final int TIME_LIMITED_MESSAGE_LIVE = 30; //seconds

    private List<InboxMessage> messageList = new ArrayList<>();
    private List<InboxMessage> encryptedMessageList = new ArrayList<>();
    private InboxMessage timeLimitedInboxMessage;
    private File messagesFile;
    private static final InboxService INSTANCE = new InboxService();
    private Button inboxButton;
    private PasswordDialog passwordDialog;
    private AtomicBoolean isPasswordVisible = new AtomicBoolean(false);

    public static InboxService getInstance() {
        return INSTANCE;
    }

    private InboxService() {
        JSONArray messageArray = null;
        try {
            messagesFile = new File(ContextVS.APPDIR + File.separator + ContextVS.INBOX_FILE);
            if(messagesFile.createNewFile()) {
                messageArray = new JSONArray();
                flush();
            } else messageArray = (JSONArray) JSONSerializer.toJSON(FileUtils.getStringFromFile(messagesFile));
            for(int i = 0; i < messageArray.size(); i++) {
                InboxMessage inboxMessage = new InboxMessage((net.sf.json.JSONObject) messageArray.get(i));
                if(inboxMessage.isEncrypted()) encryptedMessageList.add(inboxMessage);
                else messageList.add(inboxMessage);
            }
            List<Cooin> cooinList = Wallet.getCooinListFromPlainWallet();
            if(cooinList.size() > 0) {
                log.debug("found cooins in not secured wallet");
                InboxMessage inboxMessage = new InboxMessage();
                inboxMessage.setMessage(MsgUtils.getPlainWalletNotEmptyMsg(Cooin.getCurrencyMap(
                        cooinList))).setTypeVS(TypeVS.COOIN_IMPORT);
                newMessage(inboxMessage);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void setInboxButton(Button inboxButton) {
        this.inboxButton = inboxButton;
        inboxButton.setOnAction((event) -> {
            if(!encryptedMessageList.isEmpty()) {
                showPasswordDialog(ContextVS.getMessage("inboxPinDialogMsg"), false);
            } else InboxDialog.showDialog();
        });
        if(messageList.size() > 0 || encryptedMessageList.size() > 0) inboxButton.setVisible(true);
        else inboxButton.setVisible(false);
    }

    private void showPasswordDialog(final String pinDialogMessage, final boolean isTimeLimited) {
        if(isPasswordVisible.getAndSet(true)) return;
        PlatformImpl.runLater(() -> {
            if (SessionService.getCryptoTokenType() != CryptoTokenVS.MOBILE) {
                passwordDialog = new PasswordDialog();
                String dialogMessage = null;
                if (pinDialogMessage == null) dialogMessage = ContextVS.getMessage("messageToDevicePasswordMsg");
                else dialogMessage = pinDialogMessage;
                Integer visibilityInSeconds = null;
                if(isTimeLimited) {
                    visibilityInSeconds = TIME_LIMITED_MESSAGE_LIVE;
                }
                passwordDialog.showWithoutPasswordConfirm(dialogMessage, visibilityInSeconds);
                String password = passwordDialog.getPassword();
                isPasswordVisible.set(false);
                if (password != null) {
                    try {
                        KeyStore keyStore = ContextVS.getUserKeyStore(password.toCharArray());
                        PrivateKey privateKey = (PrivateKey) keyStore.getKey(ContextVS.KEYSTORE_USER_CERT_ALIAS,
                                password.toCharArray());
                        ProgressDialog.showDialog(new InboxDecryptTask(privateKey, timeLimitedInboxMessage), 
                                ContextVS.getMessage("decryptingMessagesMsg"), BrowserVS.getInstance().getScene().getWindow());
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("cryptoTokenPasswdErrorMsg"));
                    }
                } else InboxDialog.showDialog();
                timeLimitedInboxMessage = null;
            } else showMessage(new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("messageToDeviceService") +
                    " - " + ContextVS.getMessage("jksRequiredMsg")));
        });
    }

    public void newMessage(InboxMessage inboxMessage) {
        switch(inboxMessage.getTypeVS()) {
            case MESSAGEVS://message comes decrypted with session keys
                messageList.add(inboxMessage);
                PlatformImpl.runLater(() -> {
                    inboxButton.setVisible(true);
                    InboxDialog.showDialog();
                });
                flush();
                break;
            case MESSAGEVS_TO_DEVICE:
                if(!inboxMessage.isTimeLimited()) {
                    encryptedMessageList.add(inboxMessage);
                    flush();
                    PlatformImpl.runLater(() -> inboxButton.setVisible(true));
                } else timeLimitedInboxMessage = inboxMessage;
                showPasswordDialog(null, inboxMessage.isTimeLimited());
                break;
            default:
                log.error("newMessage - unprocessed message: " + inboxMessage.getTypeVS());
        }
    }

    public void removeMessage(InboxMessage inboxMessage) {
        messageList = messageList.stream().filter(m ->  !m.getMessageID().equals(inboxMessage.getMessageID())).
                collect(toList());
        encryptedMessageList = encryptedMessageList.stream().filter(m ->  !m.getMessageID().equals(inboxMessage.getMessageID())).
                collect(toList());
        if(messageList.size() == 0) PlatformImpl.runLater(() -> inboxButton.setVisible(false));
        else PlatformImpl.runLater(() -> inboxButton.setVisible(true));
        InboxDialog.getInstance().removeMessage(inboxMessage);
        flush();
    }

    public void processMessage(InboxMessage inboxMessage) {
        log.debug("processMessage - type: " + inboxMessage.getTypeVS() + " - state: " + inboxMessage.getState());
        PasswordDialog passwordDialog = null;
        String password = null;
        switch(inboxMessage.getState()) {
            case LAPSED:
                log.debug("discarding LAPSED message");
                return;
            case REMOVED:
                removeMessage(inboxMessage);
                return;
        }
        switch(inboxMessage.getTypeVS()) {
            case COOIN_WALLET_CHANGE:
                passwordDialog = new PasswordDialog();
                passwordDialog.showWithoutPasswordConfirm(ContextVS.getMessage("walletPinMsg"));
                password = passwordDialog.getPassword();
                if(password != null) {
                    try {
                        Wallet.saveToWallet(inboxMessage.getWebSocketMessage().getCooinList(), password);
                        EventBusService.getInstance().post(
                                inboxMessage.setState(InboxMessage.State.PROCESSED));
                        removeMessage(inboxMessage);
                        WebSocketServiceAuthenticated.getInstance().sendMessage(inboxMessage.getWebSocketMessage().
                                getResponse(ResponseVS.SC_OK, null).toString());
                    } catch (WalletException wex) {
                        Utils.showWalletNotFoundMessage();
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                }
                break;
            case MESSAGEVS:
                String msg = MsgUtils.getWebSocketFormattedMessage(inboxMessage);
                showMessage(msg, ContextVS.getMessage("messageLbl"));
                break;
            case COOIN_IMPORT:
                passwordDialog = new PasswordDialog();
                passwordDialog.showWithoutPasswordConfirm(ContextVS.getMessage("walletPinMsg"));
                password = passwordDialog.getPassword();
                if(password != null) {
                    try {
                        Wallet.importPlainWallet(password);
                        EventBusService.getInstance().post(inboxMessage.setState(InboxMessage.State.PROCESSED));
                    } catch (WalletException wex) {
                        Utils.showWalletNotFoundMessage();
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                }
                break;
            case MESSAGEVS_TO_DEVICE:
                showPasswordDialog(ContextVS.getMessage("decryptMsgLbl"), inboxMessage.isTimeLimited());
                break;
            default:log.debug(inboxMessage.getTypeVS() + " not processed");
        }
    }

    public List<InboxMessage> getMessageList() {
        List<InboxMessage> result =  new ArrayList<>(messageList);
        result.addAll(encryptedMessageList);
        return result;
    }

    public List<InboxMessage> getEncryptedMessageList() {
        List<InboxMessage> result =  new ArrayList<>(encryptedMessageList);
        return result;
    }

    private void flush() {
        log.debug("flush");
        try {
            JSONArray messageArray = new JSONArray();
            for(InboxMessage inboxMessage : getMessageList()) {
                messageArray.add(inboxMessage.toJSON());
            }
            FileUtils.copyStreamToFile(new ByteArrayInputStream(messageArray.toString().getBytes()), messagesFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void updateDecryptedMessages(List<InboxMessage> messageList) {
        List<String> updateMessagesUUIDList = messageList.stream().map(m -> m.getUUID()).collect(toList());
        encryptedMessageList = encryptedMessageList.stream().filter(
                m -> !updateMessagesUUIDList.contains(m.getUUID())).collect(toList());
        this.messageList.addAll(messageList);
        flush();
        InboxDialog.showDialog();
    }
}
