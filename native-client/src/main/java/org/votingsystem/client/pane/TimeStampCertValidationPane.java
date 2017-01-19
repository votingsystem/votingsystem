package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.dialog.AppDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.Constants;
import org.votingsystem.util.Messages;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampCertValidationPane extends GridPane {

    private static Logger log = Logger.getLogger(TimeStampCertValidationPane.class.getName());

    private TimeStampToken timeStampToken;
    private TextArea textArea;
    private Button validateTimeStampButton;

    public TimeStampCertValidationPane(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
        setPadding(new Insets(10, 10 , 10, 10));
        Label messageLbl = new Label(Messages.currentInstance().get("timeStampValidationWithCertMsg") + ":");
        messageLbl.setStyle("");
        add(messageLbl, 0, 0);
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-size: 10;-fx-pref-height: 400;");
        add(textArea, 0, 1);
        validateTimeStampButton = new Button(Messages.currentInstance().get("validateLbl"));
        validateTimeStampButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.CHECK)));
        validateTimeStampButton.setOnAction(actionEvent -> validateTimeStamp());
        HBox buttonsBox = new HBox();
        Button cancelButton = new Button(Messages.currentInstance().get("closeLbl"));
        cancelButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK)));
        cancelButton.setOnAction(actionEvent ->TimeStampCertValidationPane.this.getScene().getWindow().hide());
        buttonsBox.getChildren().addAll(validateTimeStampButton, Utils.getSpacer(), cancelButton);
        setMargin(buttonsBox, new Insets(20, 20, 0, 20));
        add(buttonsBox, 0, 2);
    }

    private void validateTimeStamp() {
        log.info("validateTimeStamp");
        Collection<X509Certificate> certs = null;
        try {
            String x509CertificatePEM = textArea.getText();
            certs = PEMUtils.fromPEMToX509CertCollection(x509CertificatePEM.getBytes());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            MainApp.showMessage(ResponseDto.SC_ERROR, Messages.currentInstance().get("pemCertsErrorMsg"));
        }
        if(certs.isEmpty()) {
            MainApp.showMessage(ResponseDto.SC_ERROR, Messages.currentInstance().get("certNotFoundErrorMsg"));
        } else {
            for(X509Certificate cert:certs) {
                log.info("Validating timeStampToken with cert: " + cert.getSubjectDN().toString());
                try {
                    timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                            Constants.PROVIDER).build(cert));
                    MainApp.showMessage(ResponseDto.SC_OK, Messages.currentInstance().get("timeStampCertsValidationOKMsg",
                            cert.getSubjectDN().toString()));
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    MainApp.showMessage(ResponseDto.SC_ERROR, ex.getMessage());
                }
            }
            validateTimeStampButton.setVisible(false);
        }
    }

    public static void showDialog(final TimeStampToken timeStampToken) {
        Platform.runLater(() -> {
            TimeStampCertValidationPane timeStampCertValidationPane = new TimeStampCertValidationPane(timeStampToken);
            new AppDialog(timeStampCertValidationPane).setCaption(Messages.currentInstance().get("validateTimeStampDialogCaption")).show();
        });
    }


}
