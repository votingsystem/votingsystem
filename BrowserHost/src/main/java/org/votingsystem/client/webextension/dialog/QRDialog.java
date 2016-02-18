package org.votingsystem.client.webextension.dialog;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.service.EventBusService;
import org.votingsystem.util.JSON;
import org.votingsystem.util.QRUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRDialog extends DialogVS {

    private static Logger log = Logger.getLogger(QRDialog.class.getName());

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
        super(new VBox(15));
        mainPane = (VBox) getContentPane();
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.getChildren().addAll(infoLbl);
        mainPane.getChildren().addAll(headerBox, imageView);
        EventBusService.getInstance().register(new EventBusSocketMsgListener());
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        mainPane.setStyle("-fx-font-size: 15; -fx-font-weight: bold;-fx-wrap-text: true; -fx-text-fill:#434343;");
    }

    public static void showImage(QRMessageDto qrDto, String msg) {
        try {
            if (INSTANCE == null) INSTANCE = new QRDialog();
            Image qrCodeImage = new Image(new ByteArrayInputStream(QRUtils.encodeAsPNG(
                    JSON.getMapper().writeValueAsString(qrDto), 500, 500)));
            Platform.runLater(() -> {
                INSTANCE.imageView.setImage(qrCodeImage);
                TransactionVSDto dto = qrDto.getData() != null ? (TransactionVSDto) qrDto.getData() : null;
                if(dto != null) {
                    INSTANCE.show(dto.getSubject());
                    INSTANCE.infoLbl.setText(msg);
                }
            });
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}