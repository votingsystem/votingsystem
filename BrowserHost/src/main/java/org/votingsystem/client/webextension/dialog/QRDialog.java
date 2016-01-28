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
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.service.EventBusService;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.QRUtils;
import org.votingsystem.util.TypeVS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
                    Platform.runLater(() -> { hide(); });
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

    public static void showImage(QRMessageDto qrDto) {
        try {
            if (INSTANCE == null) INSTANCE = new QRDialog();
            Image qrCodeImage = new Image(new ByteArrayInputStream(QRUtils.encodeAsPNG(
                    JSON.getMapper().writeValueAsString(qrDto), 500, 500)));
            Platform.runLater(() -> {
                INSTANCE.imageView.setImage(qrCodeImage);
                TransactionVSDto dto = qrDto.getData() != null ? (TransactionVSDto) qrDto.getData() : null;
                if(dto != null) {
                    INSTANCE.show(dto.getSubject());
                    INSTANCE.infoLbl.setText(dto.getAmount() + " " + dto.getCurrencyCode() + " - " +
                            MsgUtils.getTagDescription(dto.getTagName()));
                }
            });
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}