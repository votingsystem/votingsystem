package org.votingsystem.client.dialog;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.util.Callback;
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

    @FXML private VBox mainPane;
    @FXML private Label message;
    @FXML private ProgressBar progressBar;
    @FXML private Label closeAdviceMsg;
    @FXML private ListView<WebSocketMessage> messageListView;
    private static InboxDialog dialog;

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
        task.setOnSucceeded(event -> { if(mainPane.getChildren().contains(progressBar))
            mainPane.getChildren().remove(progressBar);});
        new Thread(task).start();
    }

    static class SocketMessageCell extends ListCell<WebSocketMessage> {

        public SocketMessageCell() { super(); }

        @Override protected void updateItem(WebSocketMessage item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);  // No text in label of super class
            if (empty) {
                setGraphic(null);
            } else {
                try {
                    setGraphic(new InboxMessageRow(item).getMainPane());
                } catch(Exception ex) {log.error(ex.getMessage(), ex);}
            }
        }
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
        messageListView.setCellFactory(new Callback<ListView<WebSocketMessage>, ListCell<WebSocketMessage>>() {
            @Override public ListCell<WebSocketMessage> call(ListView<WebSocketMessage> param) {
                return new SocketMessageCell();
            }
        });
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

    public static InboxDialog getInstance() {
        try {
            if(dialog == null) dialog = new InboxDialog();
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return dialog;
    }

   public void processMessage(WebSocketMessage socketMsg) {
        log.debug("processMessage - operation: " + socketMsg.getOperation() + " - state: " + socketMsg.getState() +
            " - num. messages: " + messageListView.getItems().size());
       switch(socketMsg.getState()) {
           case REMOVED:
               List<WebSocketMessage> removedItems = new ArrayList<>();
               messageListView.getItems().stream().forEach(m -> {
                   if(m.getDate().getTime() == socketMsg.getDate().getTime()) {
                       log.debug("deleted: " + socketMsg.getDate().getTime());
                       removedItems.add(m);
                   }});
               messageListView.getItems().removeAll(removedItems);
               break;
           default: log.debug("message not processed");
       }
        InboxService.getInstance().processMessage(socketMsg);
    }

    class EventBusMessageListener {
        @Subscribe public void notificationChanged(WebSocketMessage socketMsg) {
            log.debug("EventBusMessageListener - notification: " + socketMsg.getOperation() +
                    " - state: " + socketMsg.getState());
            switch (socketMsg.getState()) {
                case PROCESSED:
                    messageListView.getItems().stream().filter(socketMessage -> {if(socketMsg.getDate().getTime()
                            != socketMsg.getDate().getTime()) return true;
                            else return false; });
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
            if(timeLimitedWebSocketMessage == null) messageList = InboxService.getInstance().getMessageList();
            else messageList = Arrays.asList(timeLimitedWebSocketMessage);
            ObservableList<WebSocketMessage> socketMessages = FXCollections.observableArrayList();
            try {
                int i = 0;
                for(WebSocketMessage socketMsg : messageList) {
                    updateProgress(i++, messageList.size());
                    if(socketMsg.isEncrypted() && privateKey != null) {
                        socketMsg.decryptMessage(privateKey);
                    }
                    socketMessages.add(socketMsg);
                }
                Collections.sort(socketMessages, msgComparator);
                PlatformImpl.runLater(() -> messageListView.setItems(socketMessages));
            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
            return null;
        }
    }

}
