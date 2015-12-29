package org.votingsystem.client.webextension.dialog;

import com.google.common.eventbus.Subscribe;
import com.google.zxing.WriterException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.pane.DocumentVSBrowserPane;
import org.votingsystem.client.webextension.service.EventBusService;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.OperationVS;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.QRUtils;
import org.votingsystem.util.TypeVS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRDialog extends DialogVS {

    private static Logger log = Logger.getLogger(QRDialog.class.getSimpleName());

    @FXML private Label infoLbl;
    @FXML private VBox mainPane;
    @FXML private ImageView imageView;

    private static QRDialog INSTANCE;

    class EventBusSocketMsgListener {
        @Subscribe public void call(SocketMessageDto socketMsg) {
            switch(socketMsg.getOperation()) {
                case TRANSACTIONVS_RESPONSE:
                    try {
                        if(socketMsg.getWebSocketSession().getData() != null) {
                            QRMessageDto<TransactionVSDto> qrDto = (QRMessageDto<TransactionVSDto>) socketMsg
                                    .getWebSocketSession().getData();
                            TransactionVSDto transactionDto = qrDto.getData();
                            SMIMEMessage smimeMessage = socketMsg.getSMIME();
                            String result = transactionDto.validateReceipt(smimeMessage, true);
                            Button openReceiptButton = new Button();
                            openReceiptButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CERTIFICATE));
                            openReceiptButton.setText(ContextVS.getMessage("openReceiptLbl"));
                            openReceiptButton.setOnAction(event -> {
                                try {
                                    DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(
                                            smimeMessage.getBytes(), null);
                                    Platform.runLater(() -> {
                                        new DialogVS(documentVSBrowserPane, null).setCaption(documentVSBrowserPane.getCaption()).show(); });
                                } catch (Exception ex) {
                                    log.log(Level.SEVERE, ex.getMessage(), ex);
                                }
                            });
                            Platform.runLater(() -> { hide(); });
                            if(qrDto.getTypeVS() == TypeVS.CURRENCY_CHANGE) {
                                Button saveWalletButton = new Button();
                                saveWalletButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.MONEY));
                                saveWalletButton.setText(ContextVS.getMessage("saveToSecureWalletMsg"));
                                saveWalletButton.setOnAction(event -> {
                                    new OperationVS().saveWallet();
                                });
                                VBox buttonsVBox = new VBox(10);
                                buttonsVBox.getChildren().addAll(openReceiptButton, saveWalletButton);
                                BrowserHost.showMessage(result, buttonsVBox, mainPane.getScene().getWindow());
                            } else {
                                BrowserHost.showMessage(result, openReceiptButton, mainPane.getScene().getWindow());
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

    public QRDialog() throws IOException {
        super("/fxml/QRPane.fxml");
        EventBusService.getInstance().register(new EventBusSocketMsgListener());
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        mainPane.setStyle("-fx-font-size: 15; -fx-font-weight: bold;-fx-wrap-text: true; -fx-text-fill:#434343;");
    }

    public static QRDialog getInstance() {
        try {
            if (INSTANCE == null) INSTANCE = new QRDialog();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return INSTANCE;
    }

    public void showImage(QRMessageDto qrDto) throws IOException, WriterException {
        Image qrCodeImage = new Image(new ByteArrayInputStream(QRUtils.encodeAsPNG(
                JSON.getMapper().writeValueAsString(qrDto), 500, 500)));
        Platform.runLater(() -> {
            imageView.setImage(qrCodeImage);
            TransactionVSDto dto = qrDto.getData() != null ? (TransactionVSDto) qrDto.getData() : null;
            if(dto != null) {
                show(dto.getSubject());
                infoLbl.setText(dto.getAmount() + " " + dto.getCurrencyCode() + " - " +
                        MsgUtils.getTagDescription(dto.getTagName()));
            }
        });
    }

}