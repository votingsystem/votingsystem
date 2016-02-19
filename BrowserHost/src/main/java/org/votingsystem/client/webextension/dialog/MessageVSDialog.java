package org.votingsystem.client.webextension.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.service.WebSocketService;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageVSDialog extends DialogVS {

    private static Logger log = Logger.getLogger(MessageVSDialog.class.getName());

    private TextArea textArea;
    private Label messageLabel;
    private Label footerMessageLabel;
    private Button acceptButton;
    private SocketMessageDto webSocketMessage;
    private static MessageVSDialog dialog;
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();;

    public MessageVSDialog() {
        super(new VBox(10));
        VBox mainDialog = (VBox) getContentPane();
        mainDialog.setStyle("-fx-max-width: 600px; -fx-padding: 0 20 20 20; -fx-spacing: 20; -fx-alignment: center;" +
                "-fx-font-size: 16; -fx-font-weight: bold; -fx-color: #f9f9f9; -fx-text-fill:#434343;");
        messageLabel = new Label();
        footerMessageLabel = new Label(ContextVS.getMessage("checkingReceptorMsg"));
        messageLabel.setWrapText(true);
        textArea = new TextArea();
        textArea.setPrefHeight(190);
        textArea.setPrefWidth(490);
        textArea.setWrapText(true);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setStyle("-fx-word-wrap:break-word;");

        acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(actionEvent -> {
            if(textArea.getText().trim().equals("")) return;
            try {
                SocketMessageDto messageDto = webSocketMessage.getMessageVSResponse(
                        BrowserSessionService.getInstance().getUserVS(), textArea.getText());
                WebSocketService.getInstance().sendMessage(JSON.getMapper().writeValueAsString(messageDto));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
            hide();
        });
        acceptButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(footerMessageLabel, Utils.getSpacer(), acceptButton);
        mainDialog.getChildren().addAll(messageLabel, textArea, footerButtonsBox);
        Utils.addTextLimiter(textArea, ContextVS.MAX_MSG_LENGTH);
    }

    public void showMessage(SocketMessageDto webSocketMessage) {
        acceptButton.setVisible(false);
        footerMessageLabel.setVisible(true);
        executorService.execute(new CheckReceptorTask(webSocketMessage));
        this.webSocketMessage = webSocketMessage;
        messageLabel.setText(ContextVS.getMessage("messageToLbl", webSocketMessage.getFrom()));
        textArea.setText("");
        show();
    }

    public static void show(SocketMessageDto webSocketMessage) {
        PlatformImpl.runLater(() -> {
            if(dialog == null) dialog = new MessageVSDialog();
            dialog.showMessage(webSocketMessage);
        });
    }

    public  class CheckReceptorTask extends Task<Void> {

        private SocketMessageDto webSocketMessage;

        public CheckReceptorTask(SocketMessageDto webSocketMessage) {
            this.webSocketMessage = webSocketMessage;
        }

        @Override protected Void call() throws Exception {
            WebSocketSession socketSession = ContextVS.getInstance().getWSSession(webSocketMessage.getUUID());
            if(socketSession == null) {
                String serviceURL = ContextVS.getInstance().getCurrencyServer().getDeviceVSByIdServiceURL(webSocketMessage.getDeviceFromId());
                ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, ContentTypeVS.JSON);
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                    BrowserHost.showMessage(ResponseVS.SC_ERROR, responseVS.getMessage());
                    return null;
                }
                DeviceVSDto dto = (DeviceVSDto) responseVS.getMessage(DeviceVSDto.class);
                webSocketMessage.setWebSocketSession(SocketMessageDto.checkWebSocketSession(dto.getDeviceVS(), null, TypeVS.MESSAGEVS));
            }
            footerMessageLabel.setVisible(false);
            acceptButton.setVisible(true);
            return null;
        }
    }

}