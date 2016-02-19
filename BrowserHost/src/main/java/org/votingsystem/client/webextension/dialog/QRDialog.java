package org.votingsystem.client.webextension.dialog;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    private Label infoLbl;
    private VBox mainPane;
    private ImageView imageView;

    private static QRDialog INSTANCE;

    class EventBusSocketMsgListener {
        @Subscribe public void call(SocketMessageDto socketMsg) {
            switch(socketMsg.getOperation()) {
                case INIT_REMOTE_SIGNED_SESSION:
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
        imageView = new ImageView();
        infoLbl = new Label();
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.getChildren().addAll(infoLbl);
        mainPane.getChildren().addAll(headerBox, imageView);
        EventBusService.getInstance().register(new EventBusSocketMsgListener());
        mainPane.setStyle("-fx-font-size: 15; -fx-font-weight: bold;-fx-wrap-text: true; -fx-text-fill:#434343;");
    }

    public static void showDialog(QRMessageDto qrDto, String caption, String msg) {
        try {
            Platform.runLater(() -> {
                try {
                    if (INSTANCE == null) INSTANCE = new QRDialog();
                    Image qrCodeImage = new Image(new ByteArrayInputStream(QRUtils.encodeAsPNG(
                            JSON.getMapper().writeValueAsString(qrDto), 500, 500)));
                    INSTANCE.imageView.setImage(qrCodeImage);
                    INSTANCE.show(caption);
                    INSTANCE.infoLbl.setText(msg);
                } catch (Exception ex)  {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }

            });
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}