package org.votingsystem.client.pane;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import org.apache.log4j.Logger;
import org.votingsystem.client.model.SignedFile;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.DateUtils;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SignatureInfoPane extends GridPane {

    private static Logger logger = Logger.getLogger(SignatureInfoPane.class);

    private UserVS signer;
    private SMIMEMessageWrapper signedMessage;
    private SignedFile signedFile;
    private String signatureAlgorithmValue = null;

    public SignatureInfoPane(UserVS signer, SMIMEMessageWrapper signedMessage) throws Exception {
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
        }

        Label signerLabel = new Label(ContextVS.getMessage("signerLbl") + ": ");
        signerLabel.setStyle("-fx-font-weight: bold;");

        WebView webView = new WebView();
        webView.getEngine().loadContent(Formatter.getInfoCert(signer.getCertificate()));
        webView.setPrefHeight(150);
        add(webView, 0 ,2, 3, 1);

        getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints(400), new ColumnConstraints());

        if(signedMessage != null) {
            Label hashBase64Label = new Label(ContextVS.getMessage("hashBase64Lbl") + ": ");
            hashBase64Label.setStyle("-fx-font-weight: bold;");

            add(hashBase64Label, 0, 3);
            TextField hashBase64Value = new TextField(signer.getContentDigestBase64());
            hashBase64Value.setEditable(false);
            add(hashBase64Value, 1, 3);

            Label hashHexadecimalLabel = new Label(ContextVS.getMessage("hashHexadecimalLbl") + ": ");
            add(hashHexadecimalLabel, 0, 4);
            TextField hashHexadecimalValue = new TextField(signer.getContentDigestHex());
            hashHexadecimalValue.setEditable(false);
            add(hashHexadecimalValue, 1, 4);

            Label signatureBase64Label = new Label(ContextVS.getMessage("signatureBase64lLbl") + ": ");
            add(signatureBase64Label, 0, 5);
            TextField signatureBase64Value = new TextField(signer.getSignatureBase64());
            signatureBase64Value.setEditable(false);
            add(signatureBase64Value, 1, 5);

            Label signatureHexadecimalLabel = new Label(ContextVS.getMessage("signatureHexadecimalLbl") + ": ");
            add(signatureHexadecimalLabel, 0, 6);
            TextField signatureHexadecimalValue = new TextField(signer.getSignatureHex());
            signatureHexadecimalValue.setEditable(false);
            add(signatureHexadecimalValue, 1, 6);
        }
    }


}
