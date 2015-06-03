package org.votingsystem.client.dialog;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.control.CurrencyCodeChoiceBox;
import org.votingsystem.client.control.NumberTextField;
import org.votingsystem.client.service.BrowserSessionService;
import org.votingsystem.client.service.WebSocketAuthenticatedService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.QRUtils;
import org.votingsystem.util.TypeVS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRTransactionFormDialog extends DialogVS implements AddTagVSDialog.Listener {

    private static Logger log = Logger.getLogger(QRTransactionFormDialog.class.getSimpleName());

    @FXML private Label tagLbl;
    @FXML private CheckBox timeLimitedCheckBox;
    @FXML private NumberTextField amounTxt;
    @FXML private Button addTagButton;
    @FXML private Button acceptButton;
    @FXML private HBox tagHBox;
    @FXML private HBox imageHBox;
    @FXML private VBox mainPane;
    @FXML private VBox formVBox;
    @FXML private ImageView imageView;
    @FXML private CurrencyCodeChoiceBox currencyChoiceBox;
    private Image qrCodeImage;

    private String selectedTag = null;

    private static QRTransactionFormDialog INSTANCE;

    public QRTransactionFormDialog() throws IOException {
        super("/fxml/QRTransactionFormPane.fxml");
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        addTagButton.setText(ContextVS.getMessage("addTagVSLbl"));
        addTagButton.setWrapText(true);
        addTagButton.setOnAction(actionEvent -> {
            if (selectedTag == null) {
                AddTagVSDialog.show(ContextVS.getMessage("addTagVSLbl"), this);
            } else {
                selectedTag = null;
                addTagButton.setText(ContextVS.getMessage("addTagVSLbl"));
                tagLbl.setText(ContextVS.getMessage("addTagMsg"));
            }
        });
        addTagButton.setGraphic(Utils.getIcon(FontAwesomeIcons.TAG));
        timeLimitedCheckBox.setText(ContextVS.getMessage("timeLimitedCheckBox"));
        tagLbl.setText(ContextVS.getMessage("addTagMsg"));
        acceptButton.setText(ContextVS.getMessage("acceptLbl"));
        acceptButton.setGraphic(Utils.getIcon(FontAwesomeIcons.CHECK));
        acceptButton.setOnAction(actionEvent -> {
            Platform.runLater(() -> {
                try {
                    if (qrCodeImage == null) {
                        TransactionVSDto dto = new TransactionVSDto();
                        dto.setAmount(new BigDecimal(amounTxt.getText()));
                        if((dto.getAmount().compareTo(BigDecimal.ONE) < 0)) {
                            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("amountErrorMsg"));
                            return;
                        }
                        dto.setTimeLimited(timeLimitedCheckBox.isSelected());
                        if(selectedTag == null) dto.setTag(new TagVS(TagVS.WILDTAG));
                        else dto.setTag(new TagVS(selectedTag));
                        QRMessageDto qrDto = new QRMessageDto(BrowserSessionService.getInstance().getConnectedDevice(),
                                TypeVS.TRANSACTIONVS_INFO);
                        qrDto.setData(dto);
                        qrCodeImage = new Image(new ByteArrayInputStream(QRUtils.encodeAsPNG(
                                JSON.getMapper().writeValueAsString(qrDto), 500, 500)));
                        imageView.setImage(qrCodeImage);
                        VotingSystemApp.getInstance().putQRMessage(qrDto);
                        acceptButton.setText(ContextVS.getMessage("newLbl"));
                        if (!imageHBox.getChildren().contains(imageView)) imageHBox.getChildren().add(imageView);
                        if (mainPane.getChildren().contains(formVBox)) mainPane.getChildren().remove(formVBox);
                    } else {
                        qrCodeImage = null;
                        if (imageHBox.getChildren().contains(imageView)) imageHBox.getChildren().remove(imageView);
                        if (!mainPane.getChildren().contains(formVBox)) mainPane.getChildren().add(0, formVBox);
                        acceptButton.setText(ContextVS.getMessage("acceptLbl"));
                    }
                    mainPane.getScene().getWindow().sizeToScene();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });
        });
        tagLbl.setWrapText(true);
        tagHBox.setStyle("-fx-border-color: #6c0404; -fx-border-width: 1;");
        mainPane.setStyle("-fx-font-size: 15; -fx-font-weight: bold;");
        if(imageHBox.getChildren().contains(imageView)) imageHBox.getChildren().remove(imageView);
    }

    public static void showDialog() {
        Platform.runLater(() -> {
            CryptoTokenVS  tokenType = CryptoTokenVS.valueOf(ContextVS.getInstance().getProperty(
                    ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString()));
            if(tokenType != CryptoTokenVS.JKS_KEYSTORE) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("improperTokenMsg"));
                return;
            }
            if(!BrowserSessionService.getInstance().isConnected()) {
                Button connectionButton = new Button();
                connectionButton.setGraphic(Utils.getIcon(FontAwesomeIcons.CLOUD_UPLOAD));
                connectionButton.setText(ContextVS.getMessage("connectLbl"));
                connectionButton.setOnAction(event -> {
                    WebSocketAuthenticatedService.getInstance().setConnectionEnabled(true);
                });
                showMessage(ContextVS.getMessage("authenticatedWebSocketConnectionRequiredMsg"), connectionButton);
                return;
            }
            try {
                if (INSTANCE == null) {
                    INSTANCE = new QRTransactionFormDialog();
                }
                INSTANCE.selectedTag = null;
                INSTANCE.show(ContextVS.getMessage("createQRLbl"));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

    @Override public void addTagVS(String tagName) {
        this.selectedTag = tagName;
        addTagButton.setText(ContextVS.getMessage("removeTagLbl"));
        tagLbl.setText(tagName);
    }

}