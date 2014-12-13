package org.votingsystem.client.pane;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.apache.log4j.Logger;
import org.votingsystem.client.util.CooinStatusChecker;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Notification;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.WebSocketMessage;

import java.io.IOException;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class NotificationRow {

    private static Logger log = Logger.getLogger(NotificationRow.class);

    public interface Listener {
        public void onNotificationClicked(Notification notification);
    }

    @FXML private HBox mainPane;
    @FXML private Label descriptionLbl;
    @FXML private Button messageButton;
    private Notification notification;
    private Listener listener;

    public NotificationRow(Notification notification, Listener listener) throws IOException {
        this.notification = notification;
        this.listener = listener;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/InboxMessageRow.fxml"));
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML void initialize() { // This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
        switch(notification.getTypeVS()) {
            case COOIN_IMPORT:
                messageButton.setText(ContextVS.getMessage("importToWalletLbl"));
                descriptionLbl.setText(notification.getMessage());
                break;
            default:
                descriptionLbl.setText(notification.getTypeVS().toString());
                messageButton.setText(notification.getTypeVS().toString());
        }

    }

    public void onClickMessageButton(ActionEvent actionEvent) {
        listener.onNotificationClicked(notification);
    }

    public HBox getMainPane() {
        return mainPane;
    }

}