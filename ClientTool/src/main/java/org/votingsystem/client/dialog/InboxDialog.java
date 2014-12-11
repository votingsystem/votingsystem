package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.pane.InboxMessageRow;
import org.votingsystem.client.util.MessageToDeviceInbox;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.WebSocketMessage;
import java.io.IOException;
import java.security.PrivateKey;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxDialog extends DialogVS implements InboxMessageRow.Listener {

    private static Logger log = Logger.getLogger(InboxDialog.class);

    @FXML private ScrollPane scrollPane;
    @FXML private VBox mainPane;
    @FXML private Label message;
    @FXML private ProgressBar progressBar;
    @FXML private Label closeAdviceMsg;
    @FXML private Button closeButton;
    @FXML private VBox messageListPanel;

    private static final Map<WebSocketMessage, HBox> messageMap = new HashMap<WebSocketMessage, HBox>();

    public InboxDialog(PrivateKey privateKey) throws IOException {
        super("/fxml/Inbox.fxml", StageStyle.DECORATED);
        getStage().setTitle(ContextVS.getMessage("messageVSInboxCaption"));
        Task<ObservableList<WebSocketMessage>> task = new DecryptMessageTask(privateKey);
        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.visibleProperty().bind(task.runningProperty());
        new Thread(task).start();
    }

    private void refreshView() {
        messageListPanel.getChildren().clear();
        if(messageMap.size() > 0) {
            messageListPanel.getChildren().addAll(messageMap.values());
        }
        message.setText(ContextVS.getMessage("inboxNumMessagesMsg", messageMap.size()));
        scrollPane.getScene().getWindow().sizeToScene();
        closeButton.setVisible(true);
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
        closeButton.setText(ContextVS.getMessage("closeLbl"));
        closeButton.setVisible(false);
        closeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                hide();
            }
        });
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

    public class DecryptMessageTask extends Task<ObservableList<WebSocketMessage>> {

        PrivateKey privateKey;
        public DecryptMessageTask(PrivateKey privateKey) {
            this.privateKey = privateKey;
        }

        @Override protected ObservableList<WebSocketMessage> call() throws Exception {
            List<WebSocketMessage> messageList = MessageToDeviceInbox.getInstance().getMessageList();
            try {
                int i = 0;
                for(WebSocketMessage webSocketMessage : messageList) {
                    updateProgress(i++, messageList.size());
                    webSocketMessage.decryptMessage(privateKey);
                    messageMap.put(webSocketMessage, new InboxMessageRow(webSocketMessage, InboxDialog.this).getMainPane());
                }
                PlatformImpl.runLater(new Runnable(){ @Override public void run() { refreshView();}
                });

            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
            return null;
        }
    }
}
