package org.votingsystem.client.pane;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.WebSocketMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxMessageRow extends VBox {

    private static Logger log = Logger.getLogger(InboxMessageRow.class);

    public interface Listener {
        public void onMessageButtonClick(WebSocketMessage webSocketMessage);
    }

    @FXML private Label descriptionLbl;
    @FXML private Button messageButton;
    private WebSocketMessage webSocketMessage;
    private Listener listener;

    public InboxMessageRow(WebSocketMessage webSocketMessage, Listener listener) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/InboxMessageRow.fxml"));
        fxmlLoader.setController(this);
        this.webSocketMessage = webSocketMessage;
        this.listener = listener;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
        descriptionLbl.setText(webSocketMessage.getOperation().toString());
        messageButton.setText("Procesar mensaje");
        messageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                listener.onMessageButtonClick(webSocketMessage);
            }
        });
    }
}
