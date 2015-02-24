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
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.util.CooinStatusChecker;
import org.votingsystem.client.util.InboxMessage;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.StringUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxMessageRow implements CooinStatusChecker.Listener {

    private static Logger log = Logger.getLogger(InboxMessageRow.class);

    public static final int TRUNCATED_MSG_SIZE = 80; //chars

    @FXML private HBox mainPane;
    @FXML private Label descriptionLbl;
    @FXML private Label dateLbl;
    @FXML private Button messageButton;
    @FXML private Button removeButton;
    private InboxMessage inboxMessage;

    public  InboxMessageRow(InboxMessage inboxMessage) throws IOException {
        this.inboxMessage = inboxMessage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/InboxMessageRow.fxml"));
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML void initialize() { // This method is called by the FXMLLoader when initialization is complete
        messageButton.setWrapText(true);
        removeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        removeButton.setOnAction((event) ->
                InboxService.getInstance().processMessage(inboxMessage.setState(InboxMessage.State.REMOVED)));
        if(inboxMessage.isTimeLimited()) {
            Task task = new Task() {
                @Override protected Object call() throws Exception {
                    AtomicInteger secondsOpened = new AtomicInteger(0);
                    while(secondsOpened.get() < InboxService.TIME_LIMITED_MESSAGE_LIVE) {
                        PlatformImpl.runLater(() -> dateLbl.setText(
                                ContextVS.getMessage("timeLimitedWebSocketMessage",
                                        InboxService.TIME_LIMITED_MESSAGE_LIVE - secondsOpened.getAndIncrement())));
                        Thread.sleep(1000);
                    }
                    InboxService.getInstance().processMessage(inboxMessage.setState(InboxMessage.State.REMOVED));
                    return null;
                }
            };
            new Thread(task).start();
        } else dateLbl.setText(DateUtils.getDayWeekDateStr(inboxMessage.getDate()) + " - " + inboxMessage.getFrom());
        switch(inboxMessage.getTypeVS()) {
            case COOIN_WALLET_CHANGE:
                messageButton.setText(ContextVS.getMessage("cooin_wallet_change_button"));
                descriptionLbl.setText(MsgUtils.getCooinChangeWalletMsg(inboxMessage.getWebSocketMessage()));
                new Thread(new CooinStatusChecker(inboxMessage.getWebSocketMessage().getCooinList(), this)).start();
                break;
            case MESSAGEVS:
                messageButton.setText(ContextVS.getMessage("messageLbl"));
                descriptionLbl.setText(StringUtils.truncateMessage(inboxMessage.getMessage(), TRUNCATED_MSG_SIZE));
                break;
            case COOIN_IMPORT:
                messageButton.setText(ContextVS.getMessage("importToWalletLbl"));
                descriptionLbl.setText(inboxMessage.getMessage());
                break;
            case MESSAGEVS_TO_DEVICE:
                dateLbl.setText(DateUtils.getDayWeekDateStr(inboxMessage.getDate()));
                messageButton.setText(ContextVS.getMessage("decryptMsgLbl"));
                descriptionLbl.setVisible(false);
                break;
            default:
                descriptionLbl.setText(inboxMessage.getTypeVS().toString());
                messageButton.setText(inboxMessage.getTypeVS().toString());
        }

    }

    @Override public void processCooinStatus(Cooin cooin, Integer statusCode) {
        if(ResponseVS.SC_OK != statusCode) {
            log.debug("Cooin '" + cooin.getHashCertVS() + "' - statusCode: " + statusCode);
            InboxService.getInstance().processMessage(inboxMessage.setState(InboxMessage.State.REMOVED));
        }
    }

    public void onClickMessageButton(ActionEvent actionEvent) {
        InboxService.getInstance().processMessage(inboxMessage);
    }

    public HBox getMainPane() {
        return mainPane;
    }

}