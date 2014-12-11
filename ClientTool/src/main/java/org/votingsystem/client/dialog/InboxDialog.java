package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.pane.InboxMessageRow;
import org.votingsystem.client.util.MessageToDeviceInbox;
import org.votingsystem.client.util.Utils;
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
public class InboxDialog extends DialogVS implements InboxMessageRow.Listener {

    private static Logger log = Logger.getLogger(InboxDialog.class);

    @FXML private Label message;
    @FXML private Label closeAdviceMsg;
    @FXML private Button closeButton;
    @FXML private VBox messageListPanel;
    private static final Map<WebSocketMessage, InboxMessageRow> messageMap =
            new HashMap<WebSocketMessage, InboxMessageRow>();

    public InboxDialog(PrivateKey privateKey) throws IOException {
        super("/fxml/Inbox.fxml");
        List<WebSocketMessage> messageList = MessageToDeviceInbox.getInstance().getMessageList();
        try {
            for(WebSocketMessage webSocketMessage : messageList) {
                webSocketMessage.decryptMessage(privateKey);
                messageMap.put(webSocketMessage, new InboxMessageRow(webSocketMessage, this));
            }
            refreshView();
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
        }
    }

    private void refreshView() {
        messageListPanel.getChildren().clear();
        if(messageMap.size() > 0) {
            messageListPanel.getChildren().addAll(messageMap.values());
        }
        message.setText(ContextVS.getMessage("inboxNumMessagesMsg", messageMap.size()));
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
        closeButton.setText(ContextVS.getMessage("closeLbl"));
        closeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) { hide(); }});
    }

    public void showMessage(Integer statusCode, String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                MessageDialog messageDialog = new MessageDialog();
                messageDialog.showMessage(statusCode, message);
            }
        });
    }

    public static void show(PrivateKey privateKey) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    InboxDialog dialog = new InboxDialog(privateKey);
                    dialog.show();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
    }

    @Override public void onMessageButtonClick(WebSocketMessage webSocketMessage) {
        log.debug("onMessageButtonClick");
        messageMap.remove(webSocketMessage);
        refreshView();
        switch (webSocketMessage.getOperation()) {

        }
    }
}
