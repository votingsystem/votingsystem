package org.votingsystem.client.pane;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import org.apache.log4j.Logger;
import org.votingsystem.client.model.SignedFile;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.DateUtils;

import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignatureInfoPane extends GridPane {

    private static Logger logger = Logger.getLogger(SignatureInfoPane.class);

    private UserVS signer;
    private SMIMEMessage signedMessage;
    private SignedFile signedFile;
    private String signatureAlgorithmValue = null;

    public SignatureInfoPane(UserVS signer, SMIMEMessage signedMessage) throws Exception {
        this.signer = signer;
        this.signedMessage = signedMessage;
        signatureAlgorithmValue = signer.getEncryptiontId() + " - " + signer.getDigestId();
        initComponents();
    }

    public SignatureInfoPane(SignedFile signedFile) throws Exception {
        this.signedFile = signedFile;
        this.signer = signedFile.getPdfDocument().getUserVS();
        /*signatureAlgorithmValue = signedFile.getPdfPKCS7().getDigestEncryptionAlgorithmOid() + " - " +
                signedFile.getPdfPKCS7().getDigestAlgorithm();*/
        signatureAlgorithmValue = signedFile.getPdfPKCS7().getDigestAlgorithm();
        initComponents();
    }

    private void initComponents() {
        Label signatureAlgorithmLabel = new Label(ContextVS.getMessage("signatureAlgorithmLbl") + ": ");
        signatureAlgorithmLabel.setStyle("-fx-font-weight: bold;");
        setPadding(new Insets(10, 10 , 10, 10));
        add(signatureAlgorithmLabel, 0, 0);
        Label signatureAlgorithmValueLabel = new Label(signatureAlgorithmValue);
        add(signatureAlgorithmValueLabel, 1, 0);
        if(signer.getTimeStampToken() != null) {
            Label signatureDateLabel = new Label(ContextVS.getMessage("signatureDateLbl") + ": ");
            signatureDateLabel.setStyle("-fx-font-weight: bold;");
            add(signatureDateLabel, 0, 1);
            Label signatureDateValueLabel = new Label(DateUtils.getLongDate_Es(signer.getSignatureDate()));

            add(signatureDateValueLabel, 1, 1);
            Button timeStampButton = new Button(ContextVS.getMessage("timeStampButtonLbl"));
            timeStampButton.setGraphic((new ImageView(Utils.getImage(this, "clock"))));
            timeStampButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent actionEvent) {
                    TimeStampPane.showDialog(signer.getTimeStampToken());
                }
            });
            add(timeStampButton, 2, 1);
            setMargin(timeStampButton, new Insets(10, 0, 10, 0));
        }

        Label signerLabel = new Label(ContextVS.getMessage("signerLbl") + ": ");
        signerLabel.setStyle("-fx-font-weight: bold;");

        WebView webView = new WebView();
        webView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        webView.getEngine().loadContent(Formatter.getInfoCert(signer.getCertificate()));
        webView.setPrefHeight(170);
        setHgrow(webView, Priority.ALWAYS);
        setVgrow(webView, Priority.ALWAYS);
        webView.setStyle("-fx-word-wrap:break-word;");
        add(webView, 0, 2, 3, 1);

        getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints(500), new ColumnConstraints());

        final GridPane signatureInfoGridPane = new GridPane();
        CheckBox signatureInfoCheckBox = new CheckBox(ContextVS.getMessage("signatureInfoCheckBoxLbl"));
        signatureInfoCheckBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                if(SignatureInfoPane.this.getChildren().contains(signatureInfoGridPane)) {
                    SignatureInfoPane.this.getChildren().remove(signatureInfoGridPane);
                } else SignatureInfoPane.this.add(signatureInfoGridPane, 0 , 4);
                SignatureInfoPane.this.getScene().getWindow().sizeToScene();
            }});

        add(signatureInfoCheckBox, 0, 3);
        setMargin(signatureInfoCheckBox, new Insets(10, 0 , 10, 0));

        if(signedMessage != null) {

            signatureInfoGridPane.setMinWidth(1000);

            Label hashBase64Label = new Label(ContextVS.getMessage("hashBase64Lbl") + ": ");
            hashBase64Label.setStyle("-fx-font-weight: bold;");
            signatureInfoGridPane.setMargin(hashBase64Label, new Insets(10, 0 , 10, 0));

            signatureInfoGridPane.add(hashBase64Label, 0, 0);
            TextField hashBase64Value = new TextField(signer.getContentDigestBase64());
            hashBase64Value.setMinWidth(600);
            hashBase64Value.setEditable(false);

            signatureInfoGridPane.add(hashBase64Value, 1, 0);

            Label hashHexadecimalLabel = new Label(ContextVS.getMessage("hashHexadecimalLbl") + ": ");
            signatureInfoGridPane.add(hashHexadecimalLabel, 0, 2);
            setMargin(hashHexadecimalLabel, new Insets(10, 0 , 10, 0));
            TextField hashHexadecimalValue = new TextField(signer.getContentDigestHex());
            hashHexadecimalValue.setEditable(false);
            signatureInfoGridPane.add(hashHexadecimalValue, 1, 2);

            Label signatureBase64Label = new Label(ContextVS.getMessage("signatureBase64lLbl") + ": ");
            signatureInfoGridPane.add(signatureBase64Label, 0, 3);
            setMargin(signatureBase64Label, new Insets(10, 0 , 10, 0));
            TextField signatureBase64Value = new TextField(signer.getSignatureBase64());
            signatureBase64Value.setEditable(false);
            signatureInfoGridPane.add(signatureBase64Value, 1, 3);

            Label signatureHexadecimalLabel = new Label(ContextVS.getMessage("signatureHexadecimalLbl") + ": ");
            signatureInfoGridPane.add(signatureHexadecimalLabel, 0, 4);
            setMargin(signatureHexadecimalLabel, new Insets(10, 0 , 10, 0));
            TextField signatureHexadecimalValue = new TextField(signer.getSignatureHex());
            signatureHexadecimalValue.setEditable(false);
            signatureInfoGridPane.add(signatureHexadecimalValue, 1, 4);
        }
    }


}
