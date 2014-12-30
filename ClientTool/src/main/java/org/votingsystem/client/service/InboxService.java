package org.votingsystem.client.service;

import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.InboxDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.client.util.WebSocketMessage;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.Wallet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxService {

    private static Logger log = Logger.getLogger(InboxService.class);

    private List<WebSocketMessage> socketMsgList = new ArrayList<>();
    private List<WebSocketMessage> encryptedSocketMsgList = new ArrayList<>();
    private WebSocketMessage timeLimitedWebSocketMessage;
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
                socketMsgList.add(new WebSocketMessage((net.sf.json.JSONObject) messageArray.get(i)));
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public Button getInboxButton() {
        return inboxButton;
    }

    public void setInboxButton(Button inboxButton) {
        inboxButton.setGraphic(Utils.getImage(FontAwesome.Glyph.ENVELOPE, Utils.COLOR_RED_DARK));
        this.inboxButton = inboxButton;
        inboxButton.setOnAction((event) -> {
            if(!encryptedSocketMsgList.isEmpty()) {
                showPasswordDialog(ContextVS.getMessage("inboxPinDialogMsg"), false);
            } else InboxDialog.show(null, null);
        });
        if(socketMsgList.size() > 0) inboxButton.setVisible(true);
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
                    visibilityInSeconds = WebSocketMessage.TIME_LIMITED_MESSAGE_LIVE;
                }
                passwordDialog.showWithoutPasswordConfirm(dialogMessage, visibilityInSeconds);
                String password = passwordDialog.getPassword();
                isPasswordVisible.set(false);
                if (password != null) {
                    try {
                        KeyStore keyStore = ContextVS.getUserKeyStore(password.toCharArray());
                        PrivateKey privateKey = (PrivateKey) keyStore.getKey(ContextVS.KEYSTORE_USER_CERT_ALIAS,
                                password.toCharArray());
                        InboxDialog.show(privateKey, timeLimitedWebSocketMessage);
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("cryptoTokenPasswdErrorMsg"));
                    }
                }
                timeLimitedWebSocketMessage = null;
            } else showMessage(new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("messageToDeviceService") +
                    " - " + ContextVS.getMessage("jksRequiredMsg")));
        });
    }

    public void addMessage(WebSocketMessage socketMsg) {
        socketMsg.setDate(Calendar.getInstance().getTime());
        switch(socketMsg.getOperation()) {
            case MESSAGEVS://message comes decrypted with session keys
                socketMsgList.add(socketMsg);
                PlatformImpl.runLater(() -> {
                    InboxDialog.show(null, null);
                    showMessage(socketMsg.getFormattedMessage());});
                flush();
                break;
            case MESSAGEVS_TO_DEVICE:
                if(!socketMsg.isTimeLimited()) {
                    encryptedSocketMsgList.add(socketMsg);
                    PlatformImpl.runLater(() -> inboxButton.setVisible(true));
                } else timeLimitedWebSocketMessage = socketMsg;
                showPasswordDialog(null, socketMsg.isTimeLimited());
                break;
            default:
                log.error("addMessage - unprocessed message: " + socketMsg.getOperation());
        }
    }

    public void removeMessage(WebSocketMessage socketMsg) {
        socketMsgList = socketMsgList.stream().filter(m -> m.getDate().getTime() !=  socketMsg.getDate().getTime()).
                collect(Collectors.toList());
        encryptedSocketMsgList = encryptedSocketMsgList.stream().filter(m -> m.getDate().getTime() !=  socketMsg.getDate().getTime()).
                collect(Collectors.toList());
        if(socketMsgList.size() == 0) PlatformImpl.runLater(() -> inboxButton.setVisible(false));
        else PlatformImpl.runLater(() -> inboxButton.setVisible(true));
        flush();
    }

    public void processMessage(WebSocketMessage socketMsg) {
        switch(socketMsg.getState()) {
            case LAPSED:
                log.debug("discarding LAPSED message");
                return;
            case REMOVED:
                removeMessage(socketMsg);
                return;
        }
        switch(socketMsg.getOperation()) {
            case COOIN_WALLET_CHANGE:
                PasswordDialog passwordDialog = new PasswordDialog();
                passwordDialog.showWithoutPasswordConfirm(ContextVS.getMessage("walletPinMsg"));
                String password = passwordDialog.getPassword();
                if(password != null) {
                    try {
                        Wallet.saveToWallet(socketMsg.getCooinList(), password);
                        NotificationService.getInstance().postToEventBus(
                                socketMsg.setState(WebSocketMessage.State.PROCESSED));
                        removeMessage(socketMsg);
                        WebSocketServiceAuthenticated.getInstance().sendMessage(socketMsg.getResponse(
                                ResponseVS.SC_OK, null).toString());
                    } catch (WalletException wex) {
                        Utils.showWalletNotFoundMessage();
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                }
                break;
            case MESSAGEVS:
                showMessage(socketMsg.getFormattedMessage());
                break;
            default:log.debug(socketMsg.getOperation() + " not processed");
        }
    }

    public List<WebSocketMessage> getMessageList() {
        List<WebSocketMessage> result =  new ArrayList<>(socketMsgList);
        result.addAll(encryptedSocketMsgList);
        return result;
    }

    private void flush() {
        log.debug("flush");
        try {
            JSONArray messageArray = new JSONArray();
            for(WebSocketMessage socketMsg: socketMsgList) {
                messageArray.add(socketMsg.getMessageJSON());
            }
            FileUtils.copyStreamToFile(new ByteArrayInputStream(messageArray.toString().getBytes()), messagesFile);
            messageArray = new JSONArray();
            for(WebSocketMessage socketMsg: encryptedSocketMsgList) {
                messageArray.add(socketMsg.getMessageJSON());
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
