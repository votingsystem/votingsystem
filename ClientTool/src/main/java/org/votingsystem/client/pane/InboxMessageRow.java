package org.votingsystem.client.pane;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.util.CooinStatusChecker;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.WebSocketMessage;

import java.io.IOException;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxMessageRow implements CooinStatusChecker.Listener {

    private static Logger log = Logger.getLogger(InboxMessageRow.class);

    public interface Listener {
        public void removeMessage(WebSocketMessage webSocketMessage);
    }

    @FXML private HBox mainPane;
    @FXML private Label descriptionLbl;
    @FXML private Label dateLbl;
    @FXML private Button messageButton;
    @FXML private Button removeButton;
    private WebSocketMessage webSocketMessage;
    private Listener listener;

    public InboxMessageRow(WebSocketMessage webSocketMessage, Listener listener) throws IOException {
        this.webSocketMessage = webSocketMessage;
        this.listener = listener;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/InboxMessageRow.fxml"));
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML void initialize() { // This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
        removeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        removeButton.setOnAction((event) -> listener.removeMessage(webSocketMessage));
        switch(webSocketMessage.getOperation()) {
            case COOIN_WALLET_CHANGE:
                dateLbl.setText(DateUtils.getDayWeekDateStr(webSocketMessage.getDate()));
                messageButton.setWrapText(true);
                messageButton.setText(ContextVS.getMessage("cooin_wallet_change_button"));
                descriptionLbl.setText(MsgUtils.getCooinChangeWalletMsg(webSocketMessage));
                new Thread(new CooinStatusChecker(webSocketMessage.getCooinList(), this)).start();
                break;
            default:
                descriptionLbl.setText(webSocketMessage.getOperation().toString());
                messageButton.setText(webSocketMessage.getOperation().toString());
        }

    }

    @Override public void processCooinStatus(Cooin cooin, Integer statusCode) {
        if(ResponseVS.SC_OK != statusCode) {
            log.debug("Cooin '" + cooin.getHashCertVS() + "' - statusCode: " + statusCode);
            listener.removeMessage(webSocketMessage);
        }
    }

    public void onClickMessageButton(ActionEvent actionEvent) {
        InboxService.getInstance().processMessage(webSocketMessage);
    }

    public HBox getMainPane() {
        return mainPane;
    }

}