package org.votingsystem.client.webextension.pane;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.util.Formatter;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignatureInfoPane extends GridPane {

    private static Logger log = Logger.getLogger(SignatureInfoPane.class.getName());

    private UserVS signer;
    private String signatureAlgorithmValue = null;

    public SignatureInfoPane(UserVS signer, CMSSignedMessage signedMessage) throws Exception {
        this.signer = signer;
        signatureAlgorithmValue = signer.getEncryptiontId() + " - " + signer.getDigestId();
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
            Label signatureDateValueLabel = new Label(DateUtils.getDateStr(signer.getSignatureDate(), "dd/MMM/yyyy HH:mm"));
            add(signatureDateValueLabel, 1, 1);
            Button timeStampButton = new Button(ContextVS.getMessage("timeStampButtonLbl"));
            timeStampButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.CLOCK_ALT)));
            timeStampButton.setOnAction(actionEvent -> TimeStampPane.showDialog(signer.getTimeStampToken()));
            add(timeStampButton, 2, 1);
            setMargin(timeStampButton, new Insets(10, 0, 10, 0));
        }
        Label signerLabel = new Label(ContextVS.getMessage("signerLbl") + ": ");
        signerLabel.setStyle("-fx-font-weight: bold;");
        WebView webView = new WebView();
        String finalHTML = "<html style='font-size:0.9em;background: #f9f9f9;'>" +
                Formatter.getInfoCert(signer.getCertificate()) + "</html>";
        webView.getEngine().loadContent(finalHTML);
        webView.setPrefHeight(150);
        setHgrow(webView, Priority.ALWAYS);
        setVgrow(webView, Priority.ALWAYS);
        webView.setStyle("-fx-word-wrap:break-word;-fx-font-size: 10;");
        add(webView, 0, 2, 3, 1);
        getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints(500), new ColumnConstraints());
    }

}
