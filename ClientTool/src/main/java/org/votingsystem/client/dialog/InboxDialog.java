package org.votingsystem.client.dialog;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.apache.log4j.Logger;
import org.votingsystem.client.pane.InboxMessageRow;
import org.votingsystem.client.service.EventBusService;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.util.InboxMessage;
import org.votingsystem.model.ContextVS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxDialog extends DialogVS {

    private static Logger log = Logger.getLogger(InboxDialog.class);

    static final Comparator<InboxMessage> msgComparator = new Comparator<InboxMessage>() {
        public int compare(InboxMessage msg1, InboxMessage msg2) {
            return msg2.getDate().compareTo(msg1.getDate());
        }
    };

    @FXML private VBox mainPane;
    @FXML private ListView<InboxMessage> messageListView;
    private static InboxDialog dialog;


    private InboxDialog() throws IOException {
        super("/fxml/Inbox.fxml", ContextVS.getMessage("messageVSInboxCaption"));
        EventBusService.getInstance().register(new EventBusMessageListener());
    }

    static class MessageCell extends ListCell<InboxMessage> {

        public MessageCell() { super(); }

        @Override protected void updateItem(InboxMessage item, boolean empty) {
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
        messageListView.setCellFactory(new Callback<ListView<InboxMessage>, ListCell<InboxMessage>>() {
            @Override public ListCell<InboxMessage> call(ListView<InboxMessage> param) {
                return new MessageCell();
            }
        });
    }

    public static void showDialog() {
        PlatformImpl.runLater(() -> {
            try {
                if(dialog == null) dialog = new InboxDialog();
                dialog.showMessages();
            } catch (Exception ex) { log.error(ex.getMessage(), ex); }
        });
    }

    private void showMessages() {
        ObservableList<InboxMessage> messages = FXCollections.observableArrayList(
                InboxService.getInstance().getMessageList());
        Collections.sort(messages, msgComparator);
        messageListView.setItems(null);//to force refresh
        messageListView.setItems(messages);
        dialog.show();
    }

    public static InboxDialog getInstance() {
        try {
            if(dialog == null) dialog = new InboxDialog();
        } catch(Exception ex) { log.error(ex.getMessage(), ex); }
        return dialog;
    }

   public void processMessage(InboxMessage inboxMessage) {
        log.debug("processMessage - type: " + inboxMessage.getTypeVS() + " - state: " + inboxMessage.getState() +
            " - num. messages: " + messageListView.getItems().size());
       switch(inboxMessage.getState()) {
           case REMOVED:
               List<InboxMessage> removedItems = messageListView.getItems().stream().filter(m ->
                       m.getMessageID().equals(inboxMessage.getMessageID())).collect(toList());
               PlatformImpl.runLater(() -> messageListView.getItems().removeAll(removedItems));
               break;
           default: log.debug("message not processed");
       }
       InboxService.getInstance().processMessage(inboxMessage);
    }

    class EventBusMessageListener {
        @Subscribe public void notificationChanged(InboxMessage inboxMessage) {
            log.debug("EventBusMessageListener - notification: " + inboxMessage.getTypeVS() +
                    " - state: " + inboxMessage.getState());
            switch (inboxMessage.getState()) {
                case PROCESSED:
                    messageListView.getItems().stream().filter(socketMessage ->
                        inboxMessage.getDate().getTime() != inboxMessage.getDate().getTime());
                    break;
            }
        }
    }


}
