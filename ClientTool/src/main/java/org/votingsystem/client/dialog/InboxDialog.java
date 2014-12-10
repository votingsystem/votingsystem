package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.votingsystem.client.util.MessageToDeviceInbox;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.WebSocketMessage;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxDialog {

    private static Logger log = Logger.getLogger(InboxDialog.class);

    @FXML private Label message;
    @FXML private Label closeAdviceMsg;
    @FXML private Button closeButton;
    private List<WebSocketMessage> messageList;

    public InboxDialog(PrivateKey privateKey) {
        messageList = MessageToDeviceInbox.getInstance().getMessageList();
        try {
            for(WebSocketMessage webSocketMessage : messageList) {
                webSocketMessage.decryptMessage(privateKey);
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
        }
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
        closeButton.setText(ContextVS.getMessage("closeLbl"));
        closeAdviceMsg.setText(ContextVS.getMessage("closeAdviceMsg"));
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
                    Stage stage = new Stage();
                    stage.centerOnScreen();
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Inbox.fxml"));
                    fxmlLoader.setController(dialog);
                    stage.setScene(new Scene(fxmlLoader.load()));
                    stage.setTitle(ContextVS.getMessage("inboxDialogCaption"));
                    stage.initModality(Modality.WINDOW_MODAL);
                    //stage.initOwner(((Node)event.getSource()).getScene().getWindow() );
                    stage.show();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
    }
}
