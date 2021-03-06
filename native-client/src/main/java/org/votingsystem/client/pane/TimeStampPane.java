package org.votingsystem.client.pane;

import eu.europa.esig.dss.validation.TimestampToken;
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
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.util.CollectionStore;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.dialog.AppDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Messages;

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

        Label timeStampDateLabel = new Label(Messages.currentInstance().get("dateGeneratedLbl") + ":");
        timeStampDateLabel.setStyle("-fx-font-weight: bold;");
        add(timeStampDateLabel, 0, 0);
        setMargin(timeStampDateLabel, new Insets(10, 0 , 10, 0));

        TextField timeStampText = new TextField(DateUtils.getDateStr(tsInfo.getGenTime()));
        timeStampText.setEditable(false);
        add(timeStampText, 1, 0);

        Label serialNumberLabel = new Label(Messages.currentInstance().get("timeStampSerialNumberLbl") + ":");
        serialNumberLabel.setStyle("-fx-font-weight: bold;");
        add(serialNumberLabel, 0, 1);
        setMargin(serialNumberLabel, new Insets(10, 0 , 10, 0));

        TextField serialNumberTextField = new TextField(tsInfo.getSerialNumber().toString());
        serialNumberTextField.setEditable(false);
        add(serialNumberTextField, 1, 1);


        Label certSignerSerialNumberLabel = new Label(Messages.currentInstance().get("certSignerSerialNumberLbl") + ":");
        certSignerSerialNumberLabel.setStyle("-fx-font-weight: bold;");
        add(certSignerSerialNumberLabel, 0, 2);
        setMargin(certSignerSerialNumberLabel, new Insets(10, 0 , 10, 0));

        TextField certSignerSerialNumberTextField =new TextField(signerId.getSerialNumber().toString());
        certSignerSerialNumberTextField.setEditable(false);
        add(certSignerSerialNumberTextField, 1, 2);


        Label certIssuerLabel = new Label(Messages.currentInstance().get("signingCertIssuerLbl") + ":");
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
                                setProvider(Constants.PROVIDER).build(certificateHolder));
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
                    MainApp.showMessage(ResponseDto.SC_ERROR, Messages.currentInstance().get("timeStampWithoutCertErrorMsg"));
                }
                add(certsVBox, 0, 5, 2, 1);
            }
        }
        //AttributeTable  table = timeStampToken.getSignedAttributes();
        HBox buttonsHBox = new HBox();
        Button certValidationButton = new Button(Messages.currentInstance().get("validateLbl"));
        certValidationButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
        certValidationButton.setOnAction(actionEvent -> validateTimeStamp(timeStampToken));
        Button cancelButton = new Button(Messages.currentInstance().get("closeLbl"));
        cancelButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        cancelButton.setOnAction(actionEvent -> TimeStampPane.this.getScene().getWindow().hide());
        buttonsHBox.getChildren().addAll(certValidationButton, Utils.getSpacer(), cancelButton);
        setMargin(buttonsHBox, new Insets(20, 20, 0, 20));
        add(buttonsHBox, 0, 6, 2, 1);
    }

    private void validateTimeStamp(TimeStampToken timeStampToken) {
        try {
            log.info("Validating timeStampToken with cert: " +
                    MainApp.instance().getTimestampServerCert().getSubjectDN().toString());
            timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                    Constants.PROVIDER).build(MainApp.instance().getTimestampServerCert()));
            MainApp.showMessage(ResponseDto.SC_OK, Messages.currentInstance().get("timeStampCertsValidationOKMsg",
                    MainApp.instance().getTimestampServerCert().getSubjectDN().toString()));
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            MainApp.showMessage(ResponseDto.SC_ERROR, ex.getMessage());
        }
    }

    public static void showDialog(final TimeStampToken timeStampToken) {
        Platform.runLater(() -> {
            TimeStampPane timeStampPane = new TimeStampPane(timeStampToken);
            new AppDialog(timeStampPane).setCaption(Messages.currentInstance().get("timeStampInfoDialogCaption")).show();
        });
    }

    public static void showDialog(final TimestampToken timestampToken) {
        Platform.runLater(() -> {
            try {
                TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(timestampToken.getEncoded()));
                TimeStampPane timeStampPane = new TimeStampPane(timeStampToken);
                new AppDialog(timeStampPane).setCaption(Messages.currentInstance().get("timeStampInfoDialogCaption")).show();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                MainApp.showMessage(ResponseDto.SC_ERROR, ex.getMessage());
            }
        });
    }

}