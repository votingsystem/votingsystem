package org.votingsystem.client.webextension.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.votingsystem.client.webextension.pane.InboxMessageRow;
import org.votingsystem.client.webextension.service.InboxService;
import org.votingsystem.client.webextension.util.InboxMessage;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxDialog extends DialogVS {

    private static Logger log = Logger.getLogger(InboxDialog.class.getName());

    @FXML private VBox mainPane;
    @FXML private ListView<InboxMessage> messageListView;
    private static InboxDialog dialog;

    private InboxDialog() throws IOException {
        super("/fxml/Inbox.fxml", ContextVS.getMessage("messageInboxLbl"));
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.info("initialize");
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
            } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); }
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
        } catch(Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); }
        return dialog;
    }

   public void removeMessage(InboxMessage inboxMessage) {
       List<InboxMessage> removedItems = messageListView.getItems().stream().filter(m ->
               m.getMessageID().equals(inboxMessage.getMessageID())).collect(toList());
       PlatformImpl.runLater(() -> messageListView.getItems().removeAll(removedItems));
    }

    static final Comparator<InboxMessage> msgComparator = new Comparator<InboxMessage>() {
        public int compare(InboxMessage msg1, InboxMessage msg2) {
            return msg2.getDate().compareTo(msg1.getDate());
        }
    };

    public void removeMessagesByType(TypeVS typeToRemove) {
        List<InboxMessage> removedItems = messageListView.getItems().stream().filter(m ->
                m.getTypeVS() == typeToRemove).collect(toList());
        PlatformImpl.runLater(() -> messageListView.getItems().removeAll(removedItems));
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
                } catch(Exception ex) {log.log(Level.SEVERE, ex.getMessage(), ex);}
            }
        }
    }

}
