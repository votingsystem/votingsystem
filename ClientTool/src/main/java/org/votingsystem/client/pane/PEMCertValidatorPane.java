package org.votingsystem.client.pane;

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
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtils;

import java.security.cert.X509Certificate;
import java.util.Collection;

import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PEMCertValidatorPane extends GridPane {

    private static Logger log = Logger.getLogger(PEMCertValidatorPane.class);

    private TextArea textArea;
    private Button acceptButton;
    private static String certChainPEM;

    public PEMCertValidatorPane() {
        setPadding(new Insets(10, 10 , 10, 10));
        certChainPEM = null;
        Label messageLbl = new Label(ContextVS.getMessage("certPublicKeyPEMForm") + ":");
        messageLbl.setStyle("");
        add(messageLbl, 0, 0);
        textArea = new TextArea();
        textArea.setPrefHeight(400);
        textArea.setWrapText(true);
        add(textArea, 0, 1);
        acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setGraphic((Utils.getImage(FontAwesome.Glyph.CHECK)));
        acceptButton.setOnAction(actionEvent -> validatePublicKey());
        HBox buttonsBox = new HBox();
        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setGraphic((Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK)));
        cancelButton.setOnAction(actionEvent -> PEMCertValidatorPane.this.getScene().getWindow().hide());
        buttonsBox.getChildren().addAll(acceptButton, Utils.getSpacer(), cancelButton);
        setMargin(buttonsBox, new Insets(20, 20, 0, 20));
        add(buttonsBox, 0, 2);
    }

    private void validatePublicKey() {
        log.debug("validatePublicKey");
        Collection<X509Certificate> certs = null;
        try {
            certChainPEM = textArea.getText();
            certs = CertUtils.fromPEMToX509CertCollection(certChainPEM.getBytes());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("pemCertsErrorMsg"));
        }
        if(certs.isEmpty()) {
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("certNotFoundErrorMsg"));
        } else {
            for(X509Certificate cert:certs) {
                log.debug("Validating timeStampToken with cert: "  + cert.getSubjectDN().toString());
                try {

                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                }
            }
            acceptButton.setVisible(false);
        }
    }

    public static String getCertChainPEM () {
        return certChainPEM;
    }

    public static void showDialog() {
        log.debug("validateBackup");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                Stage stage = new Stage();
                stage.centerOnScreen();
                stage.initModality(Modality.WINDOW_MODAL);
                //stage.initOwner(window);
                stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
                PEMCertValidatorPane validatorPane = new PEMCertValidatorPane();
                stage.setScene(new Scene(validatorPane, javafx.scene.paint.Color.TRANSPARENT));
                stage.setTitle(ContextVS.getMessage("validateTimeStampDialogCaption"));
                stage.showAndWait();
            }
        });
    }


}
