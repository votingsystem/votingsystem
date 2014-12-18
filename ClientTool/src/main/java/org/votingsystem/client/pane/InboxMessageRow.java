package org.votingsystem.client.pane;

import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.InboxDialog;
import org.votingsystem.client.util.CooinStatusChecker;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.client.util.WebSocketMessage;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.DateUtils;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxMessageRow implements CooinStatusChecker.Listener {

    private static Logger log = Logger.getLogger(InboxMessageRow.class);

    @FXML private HBox mainPane;
    @FXML private Label descriptionLbl;
    @FXML private Label dateLbl;
    @FXML private Button messageButton;
    @FXML private Button removeButton;
    private WebSocketMessage socketMsg;

    public  InboxMessageRow(WebSocketMessage socketMsg) throws IOException {
        this.socketMsg = socketMsg;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/InboxMessageRow.fxml"));
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML void initialize() { // This method is called by the FXMLLoader when initialization is complete
        messageButton.setWrapText(true);
        removeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        removeButton.setOnAction((event) ->
                InboxDialog.getInstance().processMessage(socketMsg.setState(WebSocketMessage.State.REMOVED)));
        if(socketMsg.isTimeLimited()) {
            Task task = new Task() {
                @Override protected Object call() throws Exception {
                    AtomicInteger secondsOpened = new AtomicInteger(0);
                    while(secondsOpened.get() < WebSocketMessage.TIME_LIMITED_MESSAGE_LIVE) {
                        PlatformImpl.runLater(() -> dateLbl.setText(
                                ContextVS.getMessage("timeLimitedWebSocketMessage",
                                        WebSocketMessage.TIME_LIMITED_MESSAGE_LIVE - secondsOpened.getAndIncrement())));
                        Thread.sleep(1000);
                    }
                    InboxDialog.getInstance().processMessage(socketMsg.setState(WebSocketMessage.State.REMOVED));
                    return null;
                }
            };
            new Thread(task).start();
        } else dateLbl.setText(DateUtils.getDayWeekDateStr(socketMsg.getDate()));
        switch(socketMsg.getOperation()) {
            case COOIN_WALLET_CHANGE:
                messageButton.setText(ContextVS.getMessage("cooin_wallet_change_button"));
                descriptionLbl.setText(MsgUtils.getCooinChangeWalletMsg(socketMsg));
                new Thread(new CooinStatusChecker(socketMsg.getCooinList(), this)).start();
                break;
            case MESSAGEVS:
                messageButton.setText(ContextVS.getMessage("messagevs_lbl"));
                descriptionLbl.setText(socketMsg.getMessageTruncated());
                break;
            default:
                descriptionLbl.setText(socketMsg.getOperation().toString());
                messageButton.setText(socketMsg.getOperation().toString());
        }

    }

    @Override public void processCooinStatus(Cooin cooin, Integer statusCode) {
        if(ResponseVS.SC_OK != statusCode) {
            log.debug("Cooin '" + cooin.getHashCertVS() + "' - statusCode: " + statusCode);
            InboxDialog.getInstance().processMessage(socketMsg.setState(WebSocketMessage.State.REMOVED));
        }
    }

    public void onClickMessageButton(ActionEvent actionEvent) {
        InboxDialog.getInstance().processMessage(socketMsg);
    }

    public HBox getMainPane() {
        return mainPane;
    }

}