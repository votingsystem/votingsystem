package org.votingsystem.client.util;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.scene.control.Button;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.FileUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class NotificationManager {

    private static Logger log = Logger.getLogger(NotificationManager.class);

    private static final String EVENT_BUS_IDENTIFIER = "EVENT_BUS_NotificationManager";
    private static final EventBus eventBus = new EventBus(EVENT_BUS_IDENTIFIER);
    private File notificationsFile;
    private List<Notification> notificationList = new ArrayList<>();
    private static final NotificationManager INSTANCE = new NotificationManager();
    private Button alertButton;

    public void registerToEventBus(Object eventBusListener) {
        eventBus.register(eventBusListener);
    }

    public static NotificationManager getInstance() {return INSTANCE;}

    private NotificationManager() {
        JSONArray notificationsArray = null;
        try {
            notificationsFile = new File(ContextVS.APPDIR + File.separator + ContextVS.NOTIFICATIONS_FILE);
            if(notificationsFile.createNewFile()) {
                notificationsArray = new JSONArray();
                flush();
            }
            else notificationsArray = (JSONArray) JSONSerializer.toJSON(FileUtils.getStringFromFile(notificationsFile));
            for(int i = 0; i < notificationsArray.size(); i++) {
                notificationList.add(new Notification((net.sf.json.JSONObject) notificationsArray.get(i)));
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void setAlertButton(Button alertButton) {
        alertButton.setGraphic(Utils.getImage(FontAwesome.Glyph.INFO_CIRCLE, Utils.COLOR_YELLOW_ALERT));
        this.alertButton = alertButton;
        if(notificationList.size() > 0) alertButton.setVisible(true);
        else alertButton.setVisible(false);
    }

    public Button getAlertButton() {
        return alertButton;
    }

    public void addNotification(Notification notification) {
        notificationList.add(notification.setDate(
                Calendar.getInstance().getTime()).setUUID(UUID.randomUUID().toString()));
        eventBus.post(notification);
        flush();
    }

    public void removeNotification(Notification notification) {
        notificationList = notificationList.stream().filter(n -> !n.getUUID().equals(
                notification.getUUID())).collect(Collectors.toList());
        flush();
    }

    public List<Notification> getNotificationList() {
        return notificationList;
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
