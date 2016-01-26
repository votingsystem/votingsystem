package org.votingsystem.client.webextension.dialog;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.control.CurrencyCodeChoiceBox;
import org.votingsystem.client.webextension.control.NumberTextField;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRTransactionFormDialog extends DialogVS implements AddTagVSDialog.Listener {

    private static Logger log = Logger.getLogger(QRTransactionFormDialog.class.getSimpleName());

    @FXML private Label tagLbl;
    @FXML private Label userNameLbl;
    @FXML private Label userIBANLbl;
    @FXML private Label subjectContentLbl;
    @FXML private CheckBox timeLimitedCheckBox;
    @FXML private NumberTextField amounTxt;
    @FXML private TextField subjectTxt;
    @FXML private Button addTagButton;
    @FXML private Button acceptButton;
    @FXML private HBox tagHBox;
    @FXML private VBox mainPane;
    @FXML private VBox formVBox;
    @FXML private CurrencyCodeChoiceBox currencyChoiceBox;

    private String selectedTag = null;

    private static QRTransactionFormDialog INSTANCE;


    public QRTransactionFormDialog() throws IOException {
        super("/fxml/QRTransactionFormPane.fxml");
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        userNameLbl.setText(BrowserSessionService.getInstance().getUserVS().getName());
        userIBANLbl.setText(BrowserSessionService.getInstance().getConnectedDevice().getIBAN());
        subjectTxt.setPromptText(ContextVS.getMessage("subjectLbl"));
        addTagButton.setText(ContextVS.getMessage("addTagVSLbl"));
        addTagButton.setOnAction(actionEvent -> {
            if (selectedTag == null) {
                AddTagVSDialog.show(ContextVS.getMessage("addTagVSLbl"), this);
            } else {
                selectedTag = null;
                formVBox.getChildren().remove(timeLimitedCheckBox);
                addTagButton.setText(ContextVS.getMessage("addTagVSLbl"));
                tagLbl.setText(ContextVS.getMessage("addTagMsg"));
            }
        });
        addTagButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TAG));
        timeLimitedCheckBox.setText(ContextVS.getMessage("timeLimitedCheckBox"));
        tagLbl.setText(ContextVS.getMessage("addTagMsg"));
        acceptButton.setText(ContextVS.getMessage("acceptLbl"));
        acceptButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
        acceptButton.setOnAction(actionEvent -> {
            try {
                if (subjectTxt.getText().trim().equals("")) {
                    BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("emptyFieldErrorMsg",
                            ContextVS.getMessage("subjectLbl")));
                    return;
                }
                TransactionVSDto dto = TransactionVSDto.PAYMENT_REQUEST(
                        BrowserSessionService.getInstance().getUserVS().getName(), UserVS.Type.USER,
                        new BigDecimal(amounTxt.getText()), currencyChoiceBox.getSelected(),
                        BrowserSessionService.getInstance().getConnectedDevice().getIBAN(), subjectTxt.getText(),
                        (selectedTag == null ? TagVS.WILDTAG : selectedTag));
                dto.setPaymentOptions(Arrays.asList(TransactionVS.Type.FROM_USERVS,
                        TransactionVS.Type.CURRENCY_SEND, TransactionVS.Type.CURRENCY_CHANGE));
                if ((dto.getAmount().compareTo(BigDecimal.ONE) < 0)) {
                    BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("amountErrorMsg"));
                    return;
                }
                dto.setTimeLimited(timeLimitedCheckBox.isSelected());
                QRMessageDto qrDto = new QRMessageDto(BrowserSessionService.getInstance().getConnectedDevice(),
                        TypeVS.TRANSACTIONVS_INFO);
                qrDto.setData(dto);
                BrowserHost.getInstance().putQRMessage(qrDto);
                QRDialog.getInstance().showImage(qrDto);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        tagHBox.setStyle("-fx-border-color: #6c0404; -fx-border-width: 1;-fx-wrap-text: true;");
        mainPane.setStyle("-fx-font-size: 15; -fx-font-weight: bold;-fx-wrap-text: true; -fx-text-fill:#434343;");
        tagLbl.setWrapText(true);
        formVBox.getChildren().remove(timeLimitedCheckBox);
    }

    public static void showDialog() {
        Platform.runLater(() -> {
            CryptoTokenVS  tokenType = CryptoTokenVS.valueOf(ContextVS.getInstance().getProperty(
                    ContextVS.CRYPTO_TOKEN, CryptoTokenVS.JKS_KEYSTORE.toString()));
            if(tokenType != CryptoTokenVS.JKS_KEYSTORE) {
                BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("improperTokenMsg"));
                return;
            }
            if(!Utils.checkConnection()) return;
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
        log.info("addTagVS: " + tagName);
        this.selectedTag = tagName;
        addTagButton.setText(ContextVS.getMessage("removeTagLbl"));
        tagLbl.setText(tagName);
        formVBox.getChildren().add(3, timeLimitedCheckBox);
        formVBox.getScene().getWindow().sizeToScene();
    }

}