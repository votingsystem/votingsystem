package org.votingsystem.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import org.votingsystem.client.Browser;
import org.votingsystem.client.dialog.InboxDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.util.InboxDecryptTask;
import org.votingsystem.client.util.InboxMessage;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.currency.MapUtils;
import org.votingsystem.util.currency.Wallet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;
import static org.votingsystem.client.Browser.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxService implements PasswordDialog.Listener {

    private static Logger log = Logger.getLogger(InboxService.class.getSimpleName());

    public static final int TIME_LIMITED_MESSAGE_LIVE = 30; //seconds

    private List<InboxMessage> messageList = new ArrayList<>();
    private List<InboxMessage> encryptedMessageList = new ArrayList<>();
    private InboxMessage timeLimitedInboxMessage;
    private InboxMessage currentMessage;
    private File messagesFile;
    private static final InboxService INSTANCE = new InboxService();
    private Button inboxButton;
    private PasswordDialog passwordDialog;
    private AtomicBoolean isPasswordVisible = new AtomicBoolean(false);

    public static InboxService getInstance() {
        return INSTANCE;
    }

    private InboxService() {
        List messageList = null;
        try {
            messagesFile = new File(ContextVS.APPDIR + File.separator + ContextVS.INBOX_FILE);
            if(messagesFile.createNewFile()) {
                messageList = new ArrayList<>();
                flush();
            } else messageList =  JSON.getMapper().readValue(messagesFile, new TypeReference<List>() {
            });
            for(int i = 0; i < messageList.size(); i++) {
                InboxMessage inboxMessage = new InboxMessage((Map) messageList.get(i));
                if(inboxMessage.isEncrypted()) encryptedMessageList.add(inboxMessage);
                else messageList.add(inboxMessage);
            }
            Set<Currency> currencySet = Wallet.getPlainWallet();
            if(currencySet.size() > 0) {
                log.info("found currency in not secured wallet");
                InboxMessage inboxMessage = new InboxMessage(ContextVS.getMessage("systemLbl"),
                        new Date());
                inboxMessage.setMessage(MsgUtils.getPlainWalletNotEmptyMsg(MapUtils.getCurrencyMap(
                        currencySet))).setTypeVS(TypeVS.CURRENCY_IMPORT);
                newMessage(inboxMessage);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
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
            if (BrowserSessionService.getCryptoTokenType() != CryptoTokenVS.MOBILE) {
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
                        KeyStore keyStore = ContextVS.getInstance().getUserKeyStore(password.toCharArray());
                        PrivateKey privateKey = (PrivateKey) keyStore.getKey(ContextVS.KEYSTORE_USER_CERT_ALIAS,
                                password.toCharArray());
                        ProgressDialog.showDialog(new InboxDecryptTask(privateKey, timeLimitedInboxMessage), 
                                ContextVS.getMessage("decryptingMessagesMsg"));
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("cryptoTokenPasswdErrorMsg"));
                    }
                } else InboxDialog.showDialog();
                timeLimitedInboxMessage = null;
            } else showMessage(new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("messageToDeviceService") +
                    " - " + ContextVS.getMessage("jksRequiredMsg")));
        });
    }

    public void newMessage(InboxMessage inboxMessage, boolean openInboxDialog) {
        switch(inboxMessage.getTypeVS()) {
            case CURRENCY_IMPORT:
                messageList.add(inboxMessage);
                PlatformImpl.runLater(() -> {
                    inboxButton.setVisible(true);
                    if(openInboxDialog) InboxDialog.showDialog();
                });
                break;
            case MESSAGEVS://message comes decrypted with session keys
                messageList.add(inboxMessage);
                PlatformImpl.runLater(() -> {
                    inboxButton.setVisible(true);
                    if(openInboxDialog) InboxDialog.showDialog();
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
                log.log(Level.SEVERE, "newMessage - unprocessed message: " + inboxMessage.getTypeVS());
        }
    }

    public void newMessage(InboxMessage inboxMessage) {
        newMessage(inboxMessage, true);
    }

    public void removeMessagesByType(TypeVS typeToRemove) {
        messageList = messageList.stream().filter(m ->  m.getTypeVS() != typeToRemove).collect(toList());
        encryptedMessageList = encryptedMessageList.stream().filter(m ->  m.getTypeVS() != typeToRemove).collect(toList());
        if(messageList.size() == 0) PlatformImpl.runLater(() -> inboxButton.setVisible(false));
        else PlatformImpl.runLater(() -> inboxButton.setVisible(true));
        InboxDialog.getInstance().removeMessagesByType(typeToRemove);
        flush();
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
        log.info("processMessage - type: " + inboxMessage.getTypeVS() + " - state: " + inboxMessage.getState());
        PasswordDialog passwordDialog = null;
        String password = null;
        switch(inboxMessage.getState()) {
            case LAPSED:
                log.info("discarding LAPSED message");
                return;
            case REMOVED:
                removeMessage(inboxMessage);
                return;
        }
        currentMessage = inboxMessage;
        switch(inboxMessage.getTypeVS()) {
            case CURRENCY_WALLET_CHANGE:
                PasswordDialog.showWithoutPasswordConfirm(TypeVS.CURRENCY_WALLET_CHANGE, this,
                        ContextVS.getMessage("walletPinMsg"));
                break;
            case MESSAGEVS:
                String msg = MsgUtils.getWebSocketFormattedMessage(inboxMessage);
                showMessage(msg, ContextVS.getMessage("messageLbl"));
                break;
            case CURRENCY_IMPORT:
                PasswordDialog.showWithoutPasswordConfirm(TypeVS.CURRENCY_IMPORT, this, ContextVS.getMessage("walletPinMsg"));
                break;
            case MESSAGEVS_TO_DEVICE:
                showPasswordDialog(ContextVS.getMessage("decryptMsgLbl"), inboxMessage.isTimeLimited());
                break;
            default:log.info(inboxMessage.getTypeVS() + " not processed");
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
        log.info("flush");
        try {
            List messageList = new ArrayList<>();
            for(InboxMessage inboxMessage : getMessageList()) {
                messageList.add(inboxMessage.toMap());
            }
            FileUtils.copyStreamToFile(new ByteArrayInputStream(messageList.toString().getBytes()), messagesFile);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
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

    @Override public void setPassword(TypeVS passwordType, String password) {
        switch (passwordType) {
            case CURRENCY_IMPORT:
                if(password != null) {
                    try {
                        Wallet.getWallet(password);
                        removeMessage(currentMessage);
                    } catch (WalletException wex) {
                        Utils.showWalletNotFoundMessage();
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                }
                break;
            case CURRENCY_WALLET_CHANGE:
                if(password != null) {
                    try {
                        Wallet.saveToWallet(currentMessage.getWebSocketMessage().getCurrencySet(), password);
                        EventBusService.getInstance().post(currentMessage.setState(InboxMessage.State.PROCESSED));
                        removeMessage(currentMessage);
                        SocketMessageDto messageDto = currentMessage.getWebSocketMessage();
                        WebSocketAuthenticatedService.getInstance().sendMessage(currentMessage.getWebSocketMessage().
                                getResponse(ResponseVS.SC_OK, null, messageDto.getOperation()).toString());
                    } catch (WalletException wex) {
                        Utils.showWalletNotFoundMessage();
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                }
                break;
        }
    }
}
