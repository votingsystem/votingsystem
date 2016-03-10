package org.votingsystem.client.webextension.pane;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.util.CollectionStore;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.MainApp;
import org.votingsystem.client.webextension.dialog.DialogVS;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampPane extends GridPane {

    private static Logger log = Logger.getLogger(TimeStampPane.class.getName());

    public TimeStampPane(final TimeStampToken timeStampToken) {
        setPadding(new Insets(10, 10 , 10, 10));

        TimeStampTokenInfo tsInfo= timeStampToken.getTimeStampInfo();
        log.info("timeStampToken.getAttributeCertificates().toString(): " +
                timeStampToken.getAttributeCertificates().getMatches(null).size());

        SignerId signerId = timeStampToken.getSID();
        log.info("signerId.toString(): " + signerId.toString());
        BigInteger cert_serial_number = signerId.getSerialNumber();

        Label timeStampDateLabel = new Label(ContextVS.getMessage("dateGeneratedLbl") + ":");
        timeStampDateLabel.setStyle("-fx-font-weight: bold;");
        add(timeStampDateLabel, 0, 0);
        setMargin(timeStampDateLabel, new Insets(10, 0 , 10, 0));


        TextField timeStampText =new TextField(DateUtils.getDateStr(tsInfo.getGenTime(),"dd/MMM/yyyy HH:mm:ss"));
        timeStampText.setEditable(false);
        add(timeStampText, 1, 0);

        Label serialNumberLabel = new Label(ContextVS.getMessage("timeStampSerialNumberLbl") + ":");
        serialNumberLabel.setStyle("-fx-font-weight: bold;");
        add(serialNumberLabel, 0, 1);
        setMargin(serialNumberLabel, new Insets(10, 0 , 10, 0));

        TextField serialNumberTextField = new TextField(tsInfo.getSerialNumber().toString());
        serialNumberTextField.setEditable(false);
        add(serialNumberTextField, 1, 1);


        Label certSignerSerialNumberLabel = new Label(ContextVS.getMessage("certSignerSerialNumberLbl") + ":");
        certSignerSerialNumberLabel.setStyle("-fx-font-weight: bold;");
        add(certSignerSerialNumberLabel, 0, 2);
        setMargin(certSignerSerialNumberLabel, new Insets(10, 0 , 10, 0));

        TextField certSignerSerialNumberTextField =new TextField(signerId.getSerialNumber().toString());
        certSignerSerialNumberTextField.setEditable(false);
        add(certSignerSerialNumberTextField, 1, 2);


        Label certIssuerLabel = new Label(ContextVS.getMessage("signingCertIssuerLbl") + ":");
        certIssuerLabel.setStyle("-fx-font-weight: bold;");
        add(certIssuerLabel, 0, 3);
        setMargin(certIssuerLabel, new Insets(10, 0 , 0, 0));
        TextField certIssuerTextField = new TextField(signerId.getIssuer().toString());
        certIssuerTextField.setEditable(false);
        add(certIssuerTextField, 0, 4, 2, 1);

        CollectionStore store = (CollectionStore) timeStampToken.getCertificates();
        Collection<X509CertificateHolder> matches = store.getMatches(null);
        log.info("matches.size(): " + matches.size());

        if(matches.size() > 0) {
            boolean validationOk = false;
            for(X509CertificateHolder certificateHolder : matches) {
                boolean isSigner = false;
                log.info("cert_serial_number: '" + cert_serial_number +
                        "' - serial number: '" + certificateHolder.getSerialNumber() + "'");
                VBox certsVBox = new VBox();
                if(certificateHolder.getSerialNumber().compareTo(cert_serial_number) == 0) {
                    try {
                        log.info("certificateHolder.getSubject(): "
                                + certificateHolder.getSubject() +
                                " - serial number" + certificateHolder.getSerialNumber());
                        timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().
                                setProvider(ContextVS.PROVIDER).build(certificateHolder));
                        log.info("Validation OK");
                        validationOk = true;
                        isSigner = true;
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                }
                try {
                    X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);
                    TimeStampCertPane timeStampCertPanel = new TimeStampCertPane(certificate, isSigner);
                    certsVBox.getChildren().add(timeStampCertPanel);
                } catch (CertificateException ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
                if(!validationOk) {
                    MainApp.showMessage(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("timeStampWithoutCertErrorMsg"));
                }
                add(certsVBox, 0, 5, 2, 1);
            }
        }
        //AttributeTable  table = timeStampToken.getSignedAttributes();
        HBox buttonsHBox = new HBox();
        Button certValidationButton = new Button(ContextVS.getMessage("validateLbl"));
        certValidationButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
        certValidationButton.setOnAction(actionEvent -> validateTimeStamp(timeStampToken));
        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        cancelButton.setOnAction(actionEvent -> TimeStampPane.this.getScene().getWindow().hide());
        buttonsHBox.getChildren().addAll(certValidationButton, Utils.getSpacer(), cancelButton);
        setMargin(buttonsHBox, new Insets(20, 20, 0, 20));
        add(buttonsHBox, 0, 6, 2, 1);
    }

    private void validateTimeStamp(TimeStampToken timeStampToken) {
        try {
            log.info("Validating timeStampToken with cert: " +
                    ContextVS.getInstance().getDefaultServer().getTimeStampCert().getSubjectDN().toString());
            timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                    ContextVS.PROVIDER).build(ContextVS.getInstance().getDefaultServer().getTimeStampCert()));
            MainApp.showMessage(ResponseVS.SC_OK, ContextVS.getMessage("timeStampCertsValidationOKMsg",
                    ContextVS.getInstance().getDefaultServer().getTimeStampCert().getSubjectDN().toString()));
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            MainApp.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
        }
    }

    public static void showDialog(final TimeStampToken timeStampToken) {
        Platform.runLater(() -> {
            TimeStampPane timeStampPane = new TimeStampPane(timeStampToken);
            new DialogVS(timeStampPane).setCaption(ContextVS.getMessage("timeStampInfoDialogCaption")).show();
        });
    }

}