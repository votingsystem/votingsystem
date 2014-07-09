package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.CertUtil;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampCertValidationPane extends GridPane {

    private static Logger logger = Logger.getLogger(TimeStampCertValidationPane.class);

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
        textArea.setPrefHeight(400);
        add(textArea, 0, 1);

        validateTimeStampButton = new Button(ContextVS.getMessage("validateLbl"));
        validateTimeStampButton.setGraphic((new ImageView(Utils.getImage(this, "accept"))));
        validateTimeStampButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                validateTimeStamp();
            }
        });

        HBox buttonsBox = new HBox();
        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setGraphic((new ImageView(Utils.getImage(this, "cancel"))));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                TimeStampCertValidationPane.this.getScene().getWindow().hide();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonsBox.getChildren().addAll(validateTimeStampButton, spacer, cancelButton);
        setMargin(buttonsBox, new Insets(20, 20, 0, 20));
        add(buttonsBox, 0, 2);
    }

    private void showMessage(String message) {
        MessageDialog messageDialog = new MessageDialog();
        messageDialog.showMessage(message);
    }

    private void validateTimeStamp() {
        logger.debug("validateTimeStamp");
        Collection<X509Certificate> certs = null;
        try {
            String pemCert = textArea.getText();
            certs = CertUtil.fromPEMToX509CertCollection(pemCert.getBytes());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            showMessage(ContextVS.getInstance().getMessage("pemCertsErrorMsg"));
        }
        if(certs.isEmpty()) {
            showMessage("ERROR - " + ContextVS.getMessage("certNotFoundErrorMsg"));
        } else {
            for(X509Certificate cert:certs) {
                logger.debug("Validating timeStampToken with cert: "  + cert.getSubjectDN().toString());
                try {
                    timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                            ContextVS.PROVIDER).build(cert));
                    showMessage(ContextVS.getMessage("timeStampCertsValidationOKMsg",
                            cert.getSubjectDN().toString()));
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    showMessage("ERROR - " + ex.getMessage());
                }
            }
            validateTimeStampButton.setVisible(false);
        }
    }

    public static void showDialog(final TimeStampToken timeStampToken) {
        logger.debug("showDialog");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                Stage stage = new Stage();
                stage.initModality(Modality.WINDOW_MODAL);
                //stage.initOwner(window);

                stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
                    @Override public void handle(WindowEvent window) {
                    }
                });
                TimeStampCertValidationPane timeStampCertValidationPane = new TimeStampCertValidationPane(timeStampToken);
                stage.setScene(new Scene(timeStampCertValidationPane, javafx.scene.paint.Color.TRANSPARENT));
                stage.setTitle(ContextVS.getMessage("validateTimeStampDialogCaption"));
                stage.centerOnScreen();
                stage.show();
            }
        });
    }


}
