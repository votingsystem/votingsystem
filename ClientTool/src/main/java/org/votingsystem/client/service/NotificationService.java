package org.votingsystem.client.service;

import com.google.common.eventbus.EventBus;
import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.NotificationsDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.util.Notification;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.Wallet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class NotificationService {

    private static Logger log = Logger.getLogger(NotificationService.class);

    private static final String EVENT_BUS_IDENTIFIER = "NotificationManager_EVENT_BUS";
    private static final EventBus eventBus = new EventBus(EVENT_BUS_IDENTIFIER);
    private File notificationsFile;
    private List<Notification> notificationList = new ArrayList<>();
    private static final NotificationService INSTANCE = new NotificationService();
    private Button notificationsButton;

    public void registerToEventBus(Object eventBusListener) {
        eventBus.register(eventBusListener);
    }

    public void postToEventBus(Object eventData) {
        eventBus.post(eventData);
    }

    public static NotificationService getInstance() {return INSTANCE;}

    private NotificationService() {
        JSONArray notificationsArray = null;
        try {
            notificationsFile = new File(ContextVS.APPDIR + File.separator + ContextVS.NOTIFICATIONS_FILE);
            if(notificationsFile.createNewFile()) {
                notificationsArray = new JSONArray();
                flush();
            } else notificationsArray = (JSONArray) JSONSerializer.toJSON(FileUtils.getStringFromFile(notificationsFile));
            for(int i = 0; i < notificationsArray.size(); i++) {
                notificationList.add(new Notification((JSONObject) notificationsArray.get(i)));
            }
            List<Cooin> cooinList = Wallet.getCooinListFromPlainWallet();
            if(cooinList.size() > 0) {
                addNotification(Notification.getPlainWalletNotEmptyNotification(cooinList));
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void setNotificationsButton(Button notificationsButton) {
        notificationsButton.setGraphic(Utils.getImage(FontAwesome.Glyph.INFO_CIRCLE, Utils.COLOR_YELLOW_ALERT));
        this.notificationsButton = notificationsButton;
        notificationsButton.setOnAction((event) -> {
            NotificationsDialog.showDialog();
        });
        if(notificationList.size() > 0) notificationsButton.setVisible(true);
        else notificationsButton.setVisible(false);
    }

    public void showIfPendingNotifications() {
        if(notificationList.size() > 0) NotificationsDialog.showDialog();
    }

    public Button getNotificationsButton() {
        return notificationsButton;
    }

    public void addNotification(Notification notification) {
        notification.setDate(Calendar.getInstance().getTime()).setUUID(UUID.randomUUID().toString());
        if(TypeVS.COOIN_IMPORT == notification.getTypeVS()) {
            notificationList = notificationList.stream().filter(n -> n.getTypeVS() != TypeVS.COOIN_IMPORT).collect(toList());
        }
        notificationList.add(notification);
        eventBus.post(notification);
        if(notificationsButton != null) PlatformImpl.runLater(() -> notificationsButton.setVisible(true));
        flush();
    }

    public void removeNotification(Notification notification) {
        notificationList = notificationList.stream().filter(n -> !n.getUUID().equals(
                notification.getUUID())).collect(toList());
        if(notificationList.size() == 0) PlatformImpl.runLater(() -> notificationsButton.setVisible(false));
        flush();
    }

    public List<Notification> getNotificationList() {
        return notificationList;
    }

    public void consumeNotification(final Notification notification) {
        PlatformImpl.runLater(() -> {
            switch (notification.getTypeVS()) {
                case COOIN_IMPORT:
                    PasswordDialog passwordDialog = new PasswordDialog();
                    passwordDialog.showWithoutPasswordConfirm(ContextVS.getMessage("walletPinMsg"));
                    String password = passwordDialog.getPassword();
                    if(password != null) {
                        try {
                            Wallet.importPlainWallet(password);
                            eventBus.post(notification.setState(Notification.State.PROCESSED));
                        } catch (WalletException wex) {
                            Utils.showWalletNotFoundMessage();
                        } catch (Exception ex) {
                            log.error(ex.getMessage(), ex);
                            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                        }
                    }
                    break;
            }
        });
    }

    private void flush() {
        log.debug("flush");
        try {
            JSONArray messageArray = new JSONArray();
            for(Notification notification: notificationList) {
                messageArray.add(notification.toJSON());
            }
            FileUtils.copyStreamToFile(new ByteArrayInputStream(messageArray.toString().getBytes()), notificationsFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}