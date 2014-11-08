package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.util.CollectionStore;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.DateUtils;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampPane extends GridPane {

    private static Logger log = Logger.getLogger(TimeStampPane.class);

    public TimeStampPane(final TimeStampToken timeStampToken) {
        setPadding(new Insets(10, 10 , 10, 10));

        TimeStampTokenInfo tsInfo= timeStampToken.getTimeStampInfo();
        log.debug ("timeStampToken.getAttributeCertificates().toString(): " +
                timeStampToken.getAttributeCertificates().getMatches(null).size());

        SignerId signerId = timeStampToken.getSID();
        log.debug ("signerId.toString(): " + signerId.toString());
        BigInteger cert_serial_number = signerId.getSerialNumber();

        Label timeStampDateLabel = new Label(ContextVS.getMessage("dateGeneratedLbl") + ":");
        timeStampDateLabel.setStyle("-fx-font-weight: bold;");
        add(timeStampDateLabel, 0, 0);
        setMargin(timeStampDateLabel, new Insets(10, 0 , 10, 0));


        TextField timeStampText =new TextField(DateUtils.getDateStr(tsInfo.getGenTime(),"dd/MMM/yyyy HH:mm"));
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
        TextField certIssuerTextField = new TextField(signerId.getIssuerAsString());
        certIssuerTextField.setEditable(false);
        add(certIssuerTextField, 0, 4, 2, 1);

        CollectionStore store = (CollectionStore) timeStampToken.getCertificates();
        Collection<X509CertificateHolder> matches = store.getMatches(null);
        log.debug ("matches.size(): " + matches.size());

        if(matches.size() > 0) {
            boolean validationOk = false;
            for(X509CertificateHolder certificateHolder : matches) {
                boolean isSigner = false;
                log.debug ("cert_serial_number: '" + cert_serial_number +
                        "' - serial number: '" + certificateHolder.getSerialNumber() + "'");
                VBox certsVBox = new VBox();
                if(certificateHolder.getSerialNumber().compareTo(cert_serial_number) == 0) {
                    try {
                        log.debug ("certificateHolder.getSubject(): "
                                + certificateHolder.getSubject() +
                                " - serial number" + certificateHolder.getSerialNumber());
                        timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().
                                setProvider(ContextVS.PROVIDER).build(certificateHolder));
                        log.debug ("Validation OK");
                        validationOk = true;
                        isSigner = true;
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
                try {
                    X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);
                    TimeStampCertPane timeStampCertPanel = new TimeStampCertPane(certificate, isSigner);
                    certsVBox.getChildren().add(timeStampCertPanel);
                } catch (CertificateException ex) {
                    log.error(ex.getMessage(), ex);
                }
                if(!validationOk) {
                    showMessage(ContextVS.getInstance().getMessage("timeStampWithoutCertErrorMsg"));
                }
                add(certsVBox, 0, 5, 2, 1);
            }
        }
        //GenTimeAccuracy accuracy = tsInfo.getGenTimeAccuracy();
        //assertEquals(3, accuracy.getSeconds());
        //assertEquals(1, accuracy.getMillis());
        //assertEquals(2, accuracy.getMicros());
        //AttributeTable  table = timeStampToken.getSignedAttributes();
        HBox buttonsHBox = new HBox();


        Button certValidationButton = new Button(ContextVS.getMessage("validateLbl"));
        certValidationButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));
        certValidationButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                TimeStampCertValidationPane.showDialog(timeStampToken);
            }
        });

        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TimeStampPane.this.getScene().getWindow().hide();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buttonsHBox.getChildren().addAll(certValidationButton, spacer, cancelButton);
        setMargin(buttonsHBox, new Insets(20, 20, 0, 20));
        add(buttonsHBox, 0, 6, 2, 1);
    }

    private void showMessage(String message) {
        MessageDialog messageDialog = new MessageDialog();
        messageDialog.showMessage(null, message);
    }

    public static void showDialog(final TimeStampToken timeStampToken) {
        log.debug("showDialog");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                Stage stage = new Stage();
                stage.initModality(Modality.WINDOW_MODAL);
                //stage.initOwner(window);
                stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
                    @Override public void handle(WindowEvent window) { }
                });
                TimeStampPane timeStampPane = new TimeStampPane(timeStampToken);
                stage.setScene(new Scene(timeStampPane));
                stage.setTitle(ContextVS.getMessage("timeStampInfoDialogCaption"));
                stage.centerOnScreen();
                stage.show();
            }
        });
    }


}