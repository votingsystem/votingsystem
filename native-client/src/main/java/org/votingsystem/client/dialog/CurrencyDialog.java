package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.util.Utils;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.KeyDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Messages;

import java.io.IOException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyDialog extends AppDialog {

    private static Logger log = Logger.getLogger(CurrencyDialog.class.getName());

    private Currency currency;

    @FXML private Label serverLbl;
    @FXML private VBox mainPane;
    @FXML private TextField currencyHashText;
    @FXML private Label currencyValueLbl;
    @FXML private Label validFromLbl;
    @FXML private Label validToLbl;
    @FXML private Label currencyLbl;


    private Runnable statusChecker = new Runnable() {
        @Override public void run() {
            try {
                MetadataDto currencyServer = MainApp.instance().getSystemEntity(currency.getCurrencyEntity(), true);
                ResponseDto response = HttpConn.getInstance().doPostRequest(currency.getRevocationHash().getBytes(),
                        null, CurrencyOperation.GET_CURRENCY_STATUS.getUrl(currency.getCurrencyEntity()));
                if(ResponseDto.SC_OK != response.getStatusCode()) {
                    currency.setState(Currency.State.ERROR).setReason(response.getMessage());
                    update();
                }
                Set<KeyDto> currencyIssuerKeys = currencyServer.getKeys(KeyDto.Use.CURRENCY_ISSUER);
                Set<TrustAnchor> trustAnchors = new HashSet<>();
                for (KeyDto keyDto : currencyIssuerKeys) {
                    trustAnchors.add(new TrustAnchor(keyDto.getX509Certificate(), null));
                }
                PKIXCertPathValidatorResult validatorResult = CertificateUtils.verifyCertificate(trustAnchors, false, Arrays.asList(
                        currency.getCertificationRequest().getCertificate()));
                X509Certificate certCaResult = validatorResult.getTrustAnchor().getTrustedCert();
                PlatformImpl.runLater(statusChecker);
                log.info("currency issuer: " + certCaResult.getSubjectDN().toString());
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                X509Certificate x509Cert = currency.getCurrencyCertificate();
                String msg = null;
                if(x509Cert == null) msg = Messages.currentInstance().get("currencyWithoutCertErrorMsg");
                else {
                    String errorMsg =  null;
                    if(new Date().after(x509Cert.getNotAfter())) {
                        errorMsg =  Messages.currentInstance().get("currencyLapsedErrorLbl");
                    } else errorMsg =  Messages.currentInstance().get("currencyErrorLbl");
                    String amountStr = currency.getAmount() + " " + currency.getCurrencyCode();
                    msg = Messages.currentInstance().get("currencyInfoErroMsg", errorMsg, amountStr, x509Cert.getIssuerDN().toString(),
                            DateUtils.getDateStr(currency.getValidFrom(), "dd MMM yyyy' 'HH:mm"),
                            DateUtils.getDateStr(currency.getValidTo(), "dd MMM yyyy' 'HH:mm"));
                }
                MainApp.showMessage(msg, Messages.currentInstance().get("errorLbl"));
            }
        }
    };

    public CurrencyDialog() throws IOException {
        super("/fxml/Currency.fxml");
        MenuButton menuButton = new MenuButton();
        menuButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.BARS));
        addMenuButton(menuButton);
    }

    private void update() {
        if(currency.getState() == Currency.State.ERROR) {
            mainPane.getStyleClass().add("currency-error");
            Label currencyStatusLbl = new Label();
            currencyStatusLbl.setText(Messages.currentInstance().get("invalidCurrency"));
            currencyStatusLbl.getStyleClass().add("currency-error-msg");
            mainPane.getChildren().add(1, currencyStatusLbl);
            getStage().sizeToScene();
            MainApp.showMessage(ResponseDto.SC_ERROR, currency.getReason());
        }
    }

    public void showDialog(Currency currency) {
        this.currency = currency;
        mainPane.getStyleClass().remove("currency-error");
        setCaption(currency.getCurrencyEntity().split("//")[1]);
        currencyHashText.setText(currency.getRevocationHash());
        currencyValueLbl.setText(currency.getAmount().toPlainString());
        currencyLbl.setText(currency.getCurrencyCode().toString());
        validFromLbl.setText(Messages.currentInstance().get("issuedLbl") + ": " +
                DateUtils.getDateStr(currency.getValidFrom(), "dd MMM yyyy' 'HH:mm"));
        validToLbl.setText(Messages.currentInstance().get("expiresLbl") + ": " +
                DateUtils.getDateStr(currency.getValidTo(), "dd MMM yyyy' 'HH:mm"));
        show();
    }

    public Currency getCurrency() {
        return currency;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.info("initialize");
    }

    public static void show(final Currency currency, final Window parentWindow, EventHandler closeListener) {
        Platform.runLater(() -> {
            try {
                CurrencyDialog currencyDialog = new CurrencyDialog();
                currencyDialog.initOwner(parentWindow);
                currencyDialog.showDialog(currency);
                currencyDialog.addCloseListener(closeListener);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

}