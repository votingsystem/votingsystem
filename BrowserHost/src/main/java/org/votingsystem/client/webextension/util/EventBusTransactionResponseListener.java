package org.votingsystem.client.webextension.util;

import com.google.common.eventbus.Subscribe;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.dialog.DocumentBrowserDialog;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventBusTransactionResponseListener {

    private static Logger log = Logger.getLogger(EventBusTransactionResponseListener.class.getName());

    @Subscribe
    public void call(SocketMessageDto socketMsg) {
        switch(socketMsg.getOperation()) {
            case TRANSACTIONVS_RESPONSE:
                try {
                    if(socketMsg.getWebSocketSession().getData() != null) {
                        QRMessageDto<TransactionVSDto> qrDto = (QRMessageDto<TransactionVSDto>) socketMsg
                                .getWebSocketSession().getData();
                        TransactionVSDto transactionDto = qrDto.getData();
                        SMIMEMessage smimeMessage = socketMsg.getSMIME();
                        String result = transactionDto.validateReceipt(smimeMessage, true);
                        Button openReceiptButton = new Button(ContextVS.getMessage("openReceiptLbl"),
                                Utils.getIcon(FontAwesome.Glyph.CERTIFICATE));
                        openReceiptButton.setOnAction(event -> DocumentBrowserDialog.showDialog(smimeMessage, null));
                        if(qrDto.getTypeVS() == TypeVS.CURRENCY_CHANGE) {
                            Button saveWalletButton = new Button(ContextVS.getMessage("saveToSecureWalletMsg"),
                                    Utils.getIcon(FontAwesome.Glyph.MONEY));
                            saveWalletButton.setOnAction(event -> new OperationVS().saveWallet());
                            VBox buttonsVBox = new VBox(10);
                            buttonsVBox.getChildren().addAll(openReceiptButton, saveWalletButton);
                            BrowserHost.showMessage(ContextVS.getMessage("currencyChangeSubject"), result, buttonsVBox, null);
                        } else {
                            BrowserHost.showMessage(socketMsg.getOperation().toString().toLowerCase(), result,
                                    openReceiptButton, null);
                        }
                    }
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
                break;
            default:log.info("EventBusSocketMsgListener - unprocessed operation: " + socketMsg.getOperation());
        }
    }
}
