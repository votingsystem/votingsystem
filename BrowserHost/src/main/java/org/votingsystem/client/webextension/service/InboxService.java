package org.votingsystem.client.webextension.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.dialog.InboxDialog;
import org.votingsystem.client.webextension.dialog.MessageVSDialog;
import org.votingsystem.client.webextension.dialog.PasswordDialog;
import org.votingsystem.client.webextension.dialog.ProgressDialog;
import org.votingsystem.client.webextension.dto.InboxMessageDto;
import org.votingsystem.client.webextension.task.InboxDecryptTask;
import org.votingsystem.client.webextension.util.InboxMessage;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.service.EventBusService;
import org.votingsystem.signature.util.CryptoTokenVS;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxService {

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

    private PasswordDialog.Listener currencyWalletChangePasswordListener = new PasswordDialog.Listener() {
        @Override public void processPassword(char[] password) {
            if(password == null) return;
            try {
                BrowserHost.getInstance().saveToWallet(currentMessage.getWebSocketMessage().getCurrencySet(), password);
                EventBusService.getInstance().post(currentMessage.setState(InboxMessage.State.PROCESSED));
                removeMessage(currentMessage);
                SocketMessageDto messageDto = currentMessage.getWebSocketMessage();
                Long deviceFromId = BrowserSessionService.getInstance().getConnectedDevice().getId();
                String socketMsgStr = JSON.getMapper().writeValueAsString(messageDto.
                        getResponse(ResponseVS.SC_OK, null, deviceFromId, messageDto.getOperation()));
                WebSocketAuthenticatedService.getInstance().sendMessage(socketMsgStr);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }
    };

    public static InboxService getInstance() {
        return INSTANCE;
    }

    private InboxService() {
        List<InboxMessageDto> messageListDto = null;
        try {
            messagesFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.INBOX_FILE);
            if(messagesFile.createNewFile()) {
                messageListDto = new ArrayList<>();
                messageList = new ArrayList<>();
                flush();
            } else messageListDto =  JSON.getMapper().readValue(messagesFile, new TypeReference<List<InboxMessageDto>>() {
            });
            for(InboxMessageDto dto : messageListDto) {
                InboxMessage inboxMessage = new InboxMessage(dto);
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
            if (!encryptedMessageList.isEmpty()) {
                showPasswordDialog(ContextVS.getMessage("inboxPinDialogMsg"), false);
            } else InboxDialog.showDialog();
        });
    }

    private void showPasswordDialog(final String pinDialogMessage, final boolean isTimeLimited) {
        if(isPasswordVisible.getAndSet(true)) return;
        PlatformImpl.runLater(() -> {
            if (BrowserSessionService.getCryptoTokenType() != CryptoTokenVS.MOBILE) {
                String dialogMessage = pinDialogMessage;
                if (pinDialogMessage == null) dialogMessage = ContextVS.getMessage("messageToDevicePasswordMsg");
                Integer visibilityInSeconds = null;
                if(isTimeLimited) visibilityInSeconds = TIME_LIMITED_MESSAGE_LIVE;
                PasswordDialog.showWithoutPasswordConfirm(password -> {
                    if(password == null) {
                        InboxDialog.showDialog();
                        timeLimitedInboxMessage = null;
                        isPasswordVisible.set(false);
                    } else {
                        try {
                            KeyStore keyStore = ContextVS.getInstance().getUserKeyStore(password);
                            PrivateKey privateKey = (PrivateKey) keyStore.getKey(ContextVS.KEYSTORE_USER_CERT_ALIAS,
                                    password);
                            ProgressDialog.show(new InboxDecryptTask(privateKey, timeLimitedInboxMessage),
                                    ContextVS.getMessage("decryptingMessagesMsg"));
                        } catch (Exception ex) {
                            log.log(Level.SEVERE, ex.getMessage(), ex);
                            BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("cryptoTokenPasswdErrorMsg"));
                        }
                        timeLimitedInboxMessage = null;
                        isPasswordVisible.set(false);
                    }
                }, dialogMessage, visibilityInSeconds);
            } else BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("messageToDeviceService") +
                    " - " + ContextVS.getMessage("jksRequiredMsg"));
        });
    }

    public void newMessage(InboxMessage inboxMessage, boolean openInboxDialog) {
        switch(inboxMessage.getTypeVS()) {
            case CURRENCY_IMPORT:
                messageList.add(inboxMessage);
                if(openInboxDialog) InboxDialog.showDialog();
                break;
            case MESSAGEVS://message comes decrypted with session keys
                messageList.add(inboxMessage);
                if(openInboxDialog) InboxDialog.showDialog();
                flush();
                break;
            case MESSAGEVS_TO_DEVICE:
                if(!inboxMessage.isTimeLimited()) {
                    encryptedMessageList.add(inboxMessage);
                    flush();
                } else timeLimitedInboxMessage = inboxMessage;
                showPasswordDialog(null, inboxMessage.isTimeLimited());
                break;
            case CURRENCY_WALLET_CHANGE:
                currentMessage = inboxMessage;
                PasswordDialog.showWithoutPasswordConfirm(currencyWalletChangePasswordListener, ContextVS.getMessage("walletPinMsg"));
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
        InboxDialog.getInstance().removeMessagesByType(typeToRemove);
        flush();
    }

    public void removeMessage(InboxMessage inboxMessage) {
        messageList = messageList.stream().filter(m ->  !m.getMessageID().equals(inboxMessage.getMessageID())).
                collect(toList());
        encryptedMessageList = encryptedMessageList.stream().filter(m ->  !m.getMessageID().equals(inboxMessage.getMessageID())).
                collect(toList());
        InboxDialog.getInstance().removeMessage(inboxMessage);
        flush();
    }

    public void processMessage(InboxMessage inboxMessage) {
        log.info("processMessage - type: " + inboxMessage.getTypeVS() + " - state: " + inboxMessage.getState());
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
                PasswordDialog.showWithoutPasswordConfirm(currencyWalletChangePasswordListener, ContextVS.getMessage("walletPinMsg"));
                break;
            case MESSAGEVS:
                String msg = MsgUtils.getWebSocketFormattedMessage(inboxMessage);
                Button responseButton = new Button(ContextVS.getMessage("answerLbl"), Utils.getIcon(FontAwesome.Glyph.MAIL_REPLY));
                responseButton.setOnAction(actionEvent -> {
                    if(!Utils.checkConnection()) return;
                    MessageVSDialog.show(inboxMessage.getWebSocketMessage());
                });
                BrowserHost.showMessage(ContextVS.getMessage("messageLbl"), msg,  responseButton, null);
                break;
            case CURRENCY_IMPORT:
                PasswordDialog.showWithoutPasswordConfirm(passw -> {
                    if(passw == null) return;
                    if(BrowserHost.getInstance().loadWallet(passw) != null) removeMessage(currentMessage);
                }, ContextVS.getMessage("walletPinMsg"));
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
            List<InboxMessageDto> messageListDto = new ArrayList<>();
            for(InboxMessage inboxMessage : getMessageList()) {
                messageListDto.add(new InboxMessageDto(inboxMessage));
            }
            FileUtils.copyStreamToFile(new ByteArrayInputStream(JSON.getMapper().writeValueAsBytes(messageListDto)),
                    messagesFile);
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

}
