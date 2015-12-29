package org.votingsystem.client.webextension.pane;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.dialog.DialogVS;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ContextVS;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampCertValidationPane extends GridPane {

    private static Logger log = Logger.getLogger(TimeStampCertValidationPane.class.getSimpleName());

    private TimeStampToken timeStampToken;
    private TextArea textArea;
    private Button validateTimeStampButton;

    public TimeStampCertValidationPane(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
        setPadding(new Insets(10, 10 , 10, 10));
        Label messageLbl = new Label(ContextVS.getMessage("timeStampValidationWithCertMsg") + ":");
        messageLbl.setStyle("");
        add(messageLbl, 0, 0);
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-size: 10;-fx-pref-height: 400;");
        add(textArea, 0, 1);
        validateTimeStampButton = new Button(ContextVS.getMessage("validateLbl"));
        validateTimeStampButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.CHECK)));
        validateTimeStampButton.setOnAction(actionEvent -> validateTimeStamp());
        HBox buttonsBox = new HBox();
        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
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
            String pemCert = textArea.getText();
            certs = CertUtils.fromPEMToX509CertCollection(pemCert.getBytes());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("pemCertsErrorMsg"));
        }
        if(certs.isEmpty()) {
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("certNotFoundErrorMsg"));
        } else {
            for(X509Certificate cert:certs) {
                log.info("Validating timeStampToken with cert: " + cert.getSubjectDN().toString());
                try {
                    timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                            ContextVS.PROVIDER).build(cert));
                    BrowserHost.showMessage(ResponseVS.SC_OK, ContextVS.getMessage("timeStampCertsValidationOKMsg",
                            cert.getSubjectDN().toString()));
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                }
            }
            validateTimeStampButton.setVisible(false);
        }
    }

    public static void showDialog(final TimeStampToken timeStampToken) {
        Platform.runLater(() -> {
            TimeStampCertValidationPane timeStampCertValidationPane = new TimeStampCertValidationPane(timeStampToken);
            new DialogVS(timeStampCertValidationPane).setCaption(ContextVS.getMessage("validateTimeStampDialogCaption")).show();
        });
    }


}
