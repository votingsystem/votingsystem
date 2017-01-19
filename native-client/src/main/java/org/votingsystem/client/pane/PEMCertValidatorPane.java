package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.dialog.AppDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.Messages;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PEMCertValidatorPane extends GridPane {

    private static Logger log = Logger.getLogger(PEMCertValidatorPane.class.getName());

    private TextArea textArea;
    private Button acceptButton;
    private static String certChainPEM;

    public PEMCertValidatorPane() {
        setPadding(new Insets(10, 10 , 10, 10));
        certChainPEM = null;
        Label messageLbl = new Label(Messages.currentInstance().get("certPublicKeyPEMForm") + ":");
        messageLbl.setStyle("");
        add(messageLbl, 0, 0);
        textArea = new TextArea();
        textArea.setPrefHeight(400);
        textArea.setWrapText(true);
        add(textArea, 0, 1);
        acceptButton = new Button(Messages.currentInstance().get("acceptLbl"));
        acceptButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.CHECK)));
        acceptButton.setOnAction(actionEvent -> validatePublicKey());
        HBox buttonsBox = new HBox();
        Button cancelButton = new Button(Messages.currentInstance().get("closeLbl"));
        cancelButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK)));
        cancelButton.setOnAction(actionEvent -> PEMCertValidatorPane.this.getScene().getWindow().hide());
        buttonsBox.getChildren().addAll(acceptButton, Utils.getSpacer(), cancelButton);
        setMargin(buttonsBox, new Insets(20, 20, 0, 20));
        add(buttonsBox, 0, 2);
    }

    private void validatePublicKey() {
        log.info("validatePublicKey");
        Collection<X509Certificate> certs = null;
        try {
            certChainPEM = textArea.getText();
            certs = PEMUtils.fromPEMToX509CertCollection(certChainPEM.getBytes());
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

                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    MainApp.showMessage(ResponseDto.SC_ERROR, ex.getMessage());
                }
            }
            acceptButton.setVisible(false);
        }
    }

    public static String getCertChainPEM () {
        return certChainPEM;
    }

    public static void showDialog() {
        Platform.runLater(() -> {
            PEMCertValidatorPane validatorPane = new PEMCertValidatorPane();
            new AppDialog(validatorPane).show();
        });
    }


}
