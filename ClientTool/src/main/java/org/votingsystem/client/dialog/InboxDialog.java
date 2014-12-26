package org.votingsystem.client.dialog;

import com.google.common.eventbus.Subscribe;
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
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.service.NotificationService;
import org.votingsystem.client.util.WebSocketMessage;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.*;

import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxDialog extends DialogVS {

    private static Logger log = Logger.getLogger(InboxDialog.class);

    @FXML private ScrollPane scrollPane;
    @FXML private VBox mainPane;
    @FXML private Label message;
    @FXML private ProgressBar progressBar;
    @FXML private Label closeAdviceMsg;
    @FXML private VBox messageListPanel;
    private static InboxDialog dialog;

    private static final Map<String, HBox> messageMap = new LinkedHashMap<String, HBox>();

    static final Comparator<WebSocketMessage> msgComparator = new Comparator<WebSocketMessage>() {
            public int compare(WebSocketMessage msg1, WebSocketMessage msg2) {
                return msg2.getDate().compareTo(msg1.getDate());
            }
        };

    private InboxDialog() throws IOException {
        super("/fxml/Inbox.fxml", StageStyle.DECORATED);
        getStage().setTitle(ContextVS.getMessage("messageVSInboxCaption"));
        NotificationService.getInstance().registerToEventBus(new EventBusMessageListener());
    }

    private void load(PrivateKey privateKey, WebSocketMessage timeLimitedWebSocketMessage) {
        Task<ObservableList<WebSocketMessage>> task = new WebSocketMessageLoader(privateKey, timeLimitedWebSocketMessage);
        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.visibleProperty().bind(task.runningProperty());
        task.setOnSucceeded(event -> mainPane.getChildren().remove(progressBar));
        new Thread(task).start();
    }

    private void refreshView() {
        PlatformImpl.runLater(() ->  {
            messageListPanel.getChildren().clear();
            if(messageMap.size() > 0) {
                messageListPanel.getChildren().addAll(messageMap.values());
            } else {
                hide();
            }
            message.setText(ContextVS.getMessage("inboxNumMessagesMsg", messageMap.size()));
            scrollPane.getScene().getWindow().sizeToScene();
        });
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
    }

    public static void show(PrivateKey privateKey, WebSocketMessage timeLimitedWebSocketMessage) {
        PlatformImpl.runLater(() -> {
            try {
                if(dialog == null) dialog = new InboxDialog();
                dialog.load(privateKey, timeLimitedWebSocketMessage);
                dialog.show();
            } catch (Exception ex) { log.error(ex.getMessage(), ex); }
        });
    }

    public static InboxDialog getInstance() { return dialog;}

   public void processMessage(WebSocketMessage socketMsg) {
        log.debug("processMessage - operation: " + socketMsg.getOperation());
       switch(socketMsg.getState()) {
           case REMOVED:
               messageMap.remove(socketMsg.getUUID());
               if(getStage().isShowing()) refreshView();
               break;
       }
        InboxService.getInstance().processMessage(socketMsg);
    }

    class EventBusMessageListener {
        @Subscribe public void notificationChanged(WebSocketMessage webSocketMessage) {
            log.debug("EventBusMessageListener - notification: " + webSocketMessage.getOperation() +
                    " - state: " + webSocketMessage.getState());
            switch (webSocketMessage.getState()) {
                case PROCESSED:
                    messageMap.remove(webSocketMessage);
                    refreshView();
                    break;
            }
        }
    }

    public class WebSocketMessageLoader extends Task<ObservableList<WebSocketMessage>> {

        PrivateKey privateKey;
        private WebSocketMessage timeLimitedWebSocketMessage;
        public WebSocketMessageLoader(PrivateKey privateKey, WebSocketMessage timeLimitedWebSocketMessage) {
            this.privateKey = privateKey;
            this.timeLimitedWebSocketMessage = timeLimitedWebSocketMessage;
        }

        @Override protected ObservableList<WebSocketMessage> call() throws Exception {
            List<WebSocketMessage> messageList = null;
            PlatformImpl.runLater(() -> {
                messageListPanel.getChildren().remove(0, messageListPanel.getChildren().size());
            });
            if(timeLimitedWebSocketMessage == null) messageList = InboxService.getInstance().getMessageList();
            else messageList = Arrays.asList(timeLimitedWebSocketMessage);
            Collections.sort(messageList, msgComparator);
            messageMap.clear();
            try {
                int i = 0;
                for(WebSocketMessage socketMsg : messageList) {
                    updateProgress(i++, messageList.size());
                    if(socketMsg.isEncrypted() && privateKey != null) socketMsg.decryptMessage(privateKey);
                    messageMap.put(socketMsg.getUUID(), new InboxMessageRow(socketMsg).getMainPane());
                }
                refreshView();
            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
            return null;
        }
    }

}
