package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import org.apache.log4j.Logger;
import org.votingsystem.client.pane.InboxMessageRow;
import org.votingsystem.client.pane.NotificationRow;
import org.votingsystem.client.util.InboxManager;
import org.votingsystem.client.util.Notification;
import org.votingsystem.client.util.NotificationManager;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.WebSocketMessage;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.List;
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

    private static final Map<Notification, HBox> notificationMap = new HashMap<Notification, HBox>();

    public NotificationsDialog() throws IOException {
        super("/fxml/Inbox.fxml", StageStyle.DECORATED);
        getStage().setTitle(ContextVS.getMessage("notificationsCaption"));
        progressBar.setVisible(false);
        for(Notification notification : NotificationManager.getInstance().getNotificationList()) {
            notificationMap.put(notification, new NotificationRow(notification, this).getMainPane());
        }
        mainPane.getChildren().remove(progressBar);
        refreshView();
    }

    private void refreshView() {
        messageListPanel.getChildren().clear();
        if(notificationMap.size() > 0) {
            messageListPanel.getChildren().addAll(notificationMap.values());
        }
        message.setText(ContextVS.getMessage("numNotificationsMsg", notificationMap.size()));
        scrollPane.getScene().getWindow().sizeToScene();
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
    }

    public void showMessage(Integer statusCode, String message) {
        PlatformImpl.runLater(() -> {
            MessageDialog messageDialog = new MessageDialog();
            messageDialog.showMessage(statusCode, message);
        });
    }

    public static void showDialog() {
        PlatformImpl.runLater(() -> {
            try {
                NotificationsDialog dialog = new NotificationsDialog();
                dialog.show();
            } catch (Exception ex) { log.error(ex.getMessage(), ex); }
        });
    }

    @Override public void removeMessage(Notification notification) {
        log.debug("onMessageButtonClick - operation: " + notification.getTypeVS());
        notificationMap.remove(notification);
        NotificationManager.getInstance().removeNotification(notification);
        PlatformImpl.runLater(() ->  refreshView());
    }

}
