package org.votingsystem.client.pane;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.apache.log4j.Logger;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CooinServer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.WebSocketMessage;

import java.io.IOException;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxMessageRow {

    private static Logger log = Logger.getLogger(InboxMessageRow.class);

    public interface Listener {
        public void removeMessage(WebSocketMessage webSocketMessage);
    }

    private class CooinStatusChecker implements Runnable{
        @Override  public void run() {
            try {
                ResponseVS responseVS = Utils.checkServer(webSocketMessage.getCooinList().iterator().next().
                        getCooinServerURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    CooinServer cooinServer = (CooinServer) responseVS.getData();
                    for(Cooin cooin: webSocketMessage.getCooinList()) {
                        responseVS = HttpHelper.getInstance().getData(
                                cooinServer.getCooinStatusServiceURL(cooin.getHashCertVS()), null);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        } else {
                            log.error("ERROR: " + responseVS.getMessage());
                            listener.removeMessage(webSocketMessage);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML private HBox mainPane;
    @FXML private Label descriptionLbl;
    @FXML private Button messageButton;
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
        switch(webSocketMessage.getOperation()) {
            case COOIN_WALLET_CHANGE:
                messageButton.setText(ContextVS.getMessage("cooin_wallet_change_button"));
                descriptionLbl.setText(MsgUtils.getCooinChangeWalletMsg(webSocketMessage));
                new Thread(new CooinStatusChecker()).start();
                break;
            default:
                descriptionLbl.setText(webSocketMessage.getOperation().toString());
                messageButton.setText(webSocketMessage.getOperation().toString());
        }

    }

    public void onClickMessageButton(ActionEvent actionEvent) {
        switch(webSocketMessage.getOperation()) {
            case COOIN_WALLET_CHANGE:
                descriptionLbl.setText(ContextVS.getMessage("cooin_wallet_change_button"));
                messageButton.setText(ContextVS.getMessage("cooin_wallet_change_button"));
                break;
            default:

        }
    }

    public HBox getMainPane() {
        return mainPane;
    }


}