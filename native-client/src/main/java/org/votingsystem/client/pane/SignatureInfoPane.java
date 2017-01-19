package org.votingsystem.client.pane;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.Signature;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Messages;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignatureInfoPane extends GridPane {

    private static Logger log = Logger.getLogger(SignatureInfoPane.class.getName());

    private Signature signature;
    private String signatureAlgorithmValue = null;

    public SignatureInfoPane(Signature signature) throws Exception {
        this.signature = signature;
        signatureAlgorithmValue = signature.getEncryptionAlgorithm() + " - " + signature.getDigestAlgorithm();
        initComponents();
    }

    private void initComponents() {
        Label signatureAlgorithmLabel = new Label(Messages.currentInstance().get("signatureAlgorithmLbl") + ": ");
        signatureAlgorithmLabel.setStyle("-fx-font-weight: bold;");
        setPadding(new Insets(10, 10 , 10, 10));
        add(signatureAlgorithmLabel, 0, 0);
        Label signatureAlgorithmValueLabel = new Label(signatureAlgorithmValue);
        add(signatureAlgorithmValueLabel, 1, 0);
        if(signature.getSignatureDate() != null) {
            Label signatureDateLabel = new Label(Messages.currentInstance().get("signatureDateLbl") + ": ");
            signatureDateLabel.setStyle("-fx-font-weight: bold;");
            add(signatureDateLabel, 0, 1);
            Label signatureDateValueLabel = new Label(DateUtils.getDateStr(signature.getSignatureDate()));
            add(signatureDateValueLabel, 1, 1);
            Button timeStampButton = new Button(Messages.currentInstance().get("timeStampButtonLbl"));
            timeStampButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.CLOCK_ALT)));
            timeStampButton.setOnAction(actionEvent -> TimeStampPane.showDialog(signature.getTimestampToken()));
            add(timeStampButton, 2, 1);
            setMargin(timeStampButton, new Insets(10, 0, 10, 0));
        }
        Label signerLabel = new Label(Messages.currentInstance().get("signerLbl") + ": ");
        signerLabel.setStyle("-fx-font-weight: bold;");
        WebView webView = new WebView();
        String finalHTML = "<html style='font-size:0.9em;background: #f9f9f9;'>" +
                Formatter.getInfoCert(signature.getSigningCert()) + "</html>";
        webView.getEngine().loadContent(finalHTML);
        webView.setPrefHeight(150);
        setHgrow(webView, Priority.ALWAYS);
        setVgrow(webView, Priority.ALWAYS);
        webView.setStyle("-fx-word-wrap:break-word;-fx-font-size: 10;");
        add(webView, 0, 2, 3, 1);
        getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints(500), new ColumnConstraints());
    }

}
