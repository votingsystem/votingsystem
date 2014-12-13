package org.votingsystem.client.service;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.InboxDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.util.Notification;
import org.votingsystem.client.util.SessionVSUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.Wallet;
import org.votingsystem.util.WebSocketMessage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxService {

    private static Logger log = Logger.getLogger(InboxService.class);

    private List<WebSocketMessage> webSocketMessageList = new ArrayList<>();
    private File messagesFile;
    private static final InboxService INSTANCE = new InboxService();
    private Button inboxButton;
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
            }
            else messageArray = (JSONArray) JSONSerializer.toJSON(FileUtils.getStringFromFile(messagesFile));
            for(int i = 0; i < messageArray.size(); i++) {
                webSocketMessageList.add(new WebSocketMessage((net.sf.json.JSONObject) messageArray.get(i)));
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
            consumeMessageTodDevice(ContextVS.getMessage("inboxPinDialogMsg"));
        });
        if(webSocketMessageList.size() > 0) inboxButton.setVisible(true);
        else inboxButton.setVisible(false);
    }


    private void consumeMessageTodDevice(final String pinDialogMessage) {
        PlatformImpl.runLater(() -> {
            if (SessionVSUtils.getCryptoTokenType() != CryptoTokenVS.MOBILE) {
                PasswordDialog passwordDialog = new PasswordDialog();
                String dialogMessage = null;
                if (pinDialogMessage == null) dialogMessage = ContextVS.getMessage("messageToDevicePasswordMsg");
                else dialogMessage = pinDialogMessage;
                passwordDialog.showWithoutPasswordConfirm(dialogMessage);
                String password = passwordDialog.getPassword();
                if (password != null) {
                    try {
                        KeyStore keyStore = ContextVS.getUserKeyStore(password.toCharArray());
                        PrivateKey privateKey = (PrivateKey) keyStore.getKey(ContextVS.KEYSTORE_USER_CERT_ALIAS,
                                password.toCharArray());
                        InboxDialog.show(privateKey);
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("cryptoTokenPasswdErrorMsg"));
                    }
                }
            } else showMessage(new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("messageToDeviceService") +
                    " - " + ContextVS.getMessage("jksRequiredMsg")));
        });
    }

    public void addMessage(WebSocketMessage webSocketMessage) {
        webSocketMessage.setDate(Calendar.getInstance().getTime());
        webSocketMessageList.add(webSocketMessage);
        PlatformImpl.runLater(() -> inboxButton.setVisible(true));
        flush();
        consumeMessageTodDevice(null);
    }


    public void removeMessage(WebSocketMessage webSocketMessage) {
        webSocketMessageList = webSocketMessageList.stream().filter(m -> !m.getUUID().equals(
                webSocketMessage.getUUID())).collect(Collectors.toList());
        if(webSocketMessageList.size() == 0) PlatformImpl.runLater(() -> inboxButton.setVisible(false));
        flush();
    }

    public void processMessage(WebSocketMessage webSocketMessage) {
        switch(webSocketMessage.getOperation()) {
            case COOIN_WALLET_CHANGE:
                PasswordDialog passwordDialog = new PasswordDialog();
                passwordDialog.showWithoutPasswordConfirm(ContextVS.getMessage("walletPinMsg"));
                String password = passwordDialog.getPassword();
                if(password != null) {
                    try {
                        Wallet.saveToWallet(webSocketMessage.getCooinList(), password);
                        NotificationService.getInstance().postToEventBus(
                                webSocketMessage.setState(WebSocketMessage.State.PROCESSED));
                        removeMessage(webSocketMessage);
                    } catch (WalletException wex) {
                        Utils.showWalletNotFoundMessage();
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                }
                break;
            default:

        }
    }

    public List<WebSocketMessage> getMessageList() {
        return new ArrayList<>(webSocketMessageList);
    }

    public void resetMessageList() {
        webSocketMessageList = new ArrayList<>();
    }

    private void flush() {
        log.debug("flush");
        try {
            JSONArray messageArray = new JSONArray();
            for(WebSocketMessage webSocketMessage: webSocketMessageList) {
                messageArray.add(webSocketMessage.getMessageJSON());
            }
            FileUtils.copyStreamToFile(new ByteArrayInputStream(messageArray.toString().getBytes()), messagesFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
