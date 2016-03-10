package org.votingsystem.client.webextension.dialog;

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
import org.votingsystem.client.webextension.MainApp;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.crypto.CertUtils;

import java.io.IOException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyDialog extends DialogVS {

    private static Logger log = Logger.getLogger(CurrencyDialog.class.getName());

    private Currency currency;
    private CurrencyServer currencyServer;

    @FXML private Label serverLbl;
    @FXML private VBox mainPane;
    @FXML private TextField currencyHashText;
    @FXML private Label currencyValueLbl;
    @FXML private Label currencyTagLbl;
    @FXML private Label validFromLbl;
    @FXML private Label validToLbl;
    @FXML private Label currencyLbl;


    private Runnable statusChecker = new Runnable() {
        @Override public void run() {
            try {
                ResponseVS responseVS = ContextVS.getInstance().checkServer(currency.getCurrencyServerURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    currencyServer = (CurrencyServer) responseVS.getData();
                    responseVS = HttpHelper.getInstance().getData(
                            currencyServer.getCurrencyStateServiceURL(currency.getHashCertVS()), null);
                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                        currency.setState(Currency.State.ERROR);
                        currency.setReason(responseVS.getReason());
                        update();
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
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
            currencyStatusLbl.setText(ContextVS.getMessage("invalidCurrency"));
            currencyStatusLbl.getStyleClass().add("currency-error-msg");
            mainPane.getChildren().add(1, currencyStatusLbl);
            getStage().sizeToScene();
            MainApp.showMessage(ResponseVS.SC_ERROR, currency.getReason());
        }
    }

    public void showDialog(Currency currency) {
        this.currency = currency;
        mainPane.getStyleClass().remove("currency-error");
        setCaption(currency.getCurrencyServerURL().split("//")[1]);
        currencyHashText.setText(currency.getHashCertVS());
        currencyValueLbl.setText(currency.getAmount().toPlainString());
        currencyLbl.setText(currency.getCurrencyCode());
        currencyTagLbl.setText(MsgUtils.getTagDescription(currency.getTagVS().getName()));
        validFromLbl.setText(ContextVS.getMessage("issuedLbl") + ": " +
                DateUtils.getDateStr(currency.getValidFrom(), "dd MMM yyyy' 'HH:mm"));
        validToLbl.setText(ContextVS.getMessage("expiresLbl") + ": " +
                DateUtils.getDateStr(currency.getValidTo(), "dd MMM yyyy' 'HH:mm"));
        if(currency.getTimeLimited()) validToLbl.setStyle("-fx-text-fill:rgba(186,0,17, 0.45);");
        try {
            PKIXCertPathValidatorResult validatorResult = CertUtils.verifyCertificate(
                    ContextVS.getInstance().getCurrencyServer().getTrustAnchors(), false, Arrays.asList(
                            currency.getCertificationRequest().getCertificate()));
            X509Certificate certCaResult = validatorResult.getTrustAnchor().getTrustedCert();
            PlatformImpl.runLater(statusChecker);
            log.info("currency issuer: " + certCaResult.getSubjectDN().toString());
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            X509Certificate x509Cert = currency.getX509AnonymousCert();
            String msg = null;
            if(x509Cert == null) msg = ContextVS.getMessage("currencyWithoutCertErrorMsg");
            else {
                String errorMsg =  null;
                if(new Date().after(x509Cert.getNotAfter())) {
                    errorMsg =  ContextVS.getMessage("currencyLapsedErrorLbl");
                } else errorMsg =  ContextVS.getMessage("currencyErrorLbl");
                String amountStr = currency.getAmount() + " " + currency.getCurrencyCode() + " " +
                        Utils.getTagForDescription(currency.getTagVS().getName());
                msg = ContextVS.getMessage("currencyInfoErroMsg", errorMsg, amountStr, x509Cert.getIssuerDN().toString(),
                        DateUtils.getDateStr(currency.getValidFrom(), "dd MMM yyyy' 'HH:mm"),
                        DateUtils.getDateStr(currency.getValidTo(), "dd MMM yyyy' 'HH:mm"));
            }
            MainApp.showMessage(msg, ContextVS.getMessage("errorLbl"));
        }
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