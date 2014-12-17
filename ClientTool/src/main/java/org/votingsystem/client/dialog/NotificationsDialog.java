package org.votingsystem.client.dialog;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import org.apache.log4j.Logger;
import org.votingsystem.client.pane.NotificationRow;
import org.votingsystem.client.service.NotificationService;
import org.votingsystem.client.util.Notification;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class NotificationsDialog extends DialogVS implements NotificationRow.Listener {

    private static Logger log = Logger.getLogger(NotificationsDialog.class);

    @FXML private ScrollPane scrollPane;
    @FXML private VBox mainPane;
    @FXML private Label message;
    @FXML private ProgressBar progressBar;
    @FXML private Label closeAdviceMsg;
    @FXML private VBox messageListPanel;
    private static NotificationsDialog dialog;

    private static final Map<Notification, HBox> notificationMap = new HashMap<Notification, HBox>();

    private NotificationsDialog() throws IOException {
        super("/fxml/Inbox.fxml", StageStyle.DECORATED);
        getStage().setTitle(ContextVS.getMessage("notificationsCaption"));
        progressBar.setVisible(false);
        for(Notification notification : NotificationService.getInstance().getNotificationList()) {
            notificationMap.put(notification, new NotificationRow(notification, this).getMainPane());
        }
        mainPane.getChildren().remove(progressBar);
        NotificationService.getInstance().registerToEventBus(new EventBusNotificationListener());
        refreshView();
    }

    private void refreshView() {
        PlatformImpl.runLater(() ->  {
            messageListPanel.getChildren().clear();
            if(notificationMap.size() > 0) {
                messageListPanel.getChildren().addAll(notificationMap.values());
                message.setText(ContextVS.getMessage("numNotificationsMsg", notificationMap.size()));
                scrollPane.getScene().getWindow().sizeToScene();
            } else hide();
        });
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
    }

    public static void showDialog() {
        PlatformImpl.runLater(() -> {
            try {
                if(dialog == null) dialog = new NotificationsDialog();
                dialog.show();
            } catch (Exception ex) { log.error(ex.getMessage(), ex); }
        });
    }

    class EventBusNotificationListener {
        @Subscribe public void notificationChanged(Notification notification) {
            log.debug("EventBusNotificationListener - notification: " + notification.getTypeVS() +
                    " - state: " + notification.getState());
            switch (notification.getState()) {
                case PROCESSED:
                    notificationMap.remove(notification);
                    refreshView();
                    break;
            }
        }
    }

    @Override public void onNotificationClicked(Notification notification) {
        log.debug("onNotificationClicked - operation: " + notification.getTypeVS());
        NotificationService.getInstance().consumeNotification(notification);
    }

}
