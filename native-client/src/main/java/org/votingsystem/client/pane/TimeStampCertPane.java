package org.votingsystem.client.pane;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Messages;

import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampCertPane extends GridPane {

    private static Logger log = Logger.getLogger(TimeStampCertPane.class.getName());

    private X509Certificate certificate;

    public TimeStampCertPane(X509Certificate certificate, boolean isSigner) {
        this.certificate = certificate;
        if(isSigner) {
            Label signerLabel = new Label(Messages.currentInstance().get("timeStampCertSignerLbl"));
            add(signerLabel, 0, 0);
        }
        Button showPEMcertButton = new Button(Messages.currentInstance().get("showCertPemLbl"));
        showPEMcertButton.setOnAction(actionEvent -> showPEMCert());
        showPEMcertButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CERTIFICATE));
        add(showPEMcertButton, 0, 1);
        WebView certInfoWebView = new WebView();
        certInfoWebView.setPrefHeight(400);
        add(certInfoWebView, 0, 2);
    }

    private void showPEMCert() {
        try {
            String message = new String(PEMUtils.getPEMEncoded(certificate));
            MessageDialog messageDialog = new MessageDialog(getScene().getWindow());
            messageDialog.showMessage(null, certificate.getSubjectDN().toString() + " - " + message);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static String getCertInfo (X509Certificate certificate) {
        return Messages.currentInstance().get("certInfoFormattedMsg",certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),certificate.getSerialNumber().toString(),
                DateUtils.getDateStr(certificate.getNotBefore()),
                DateUtils.getDateStr(certificate.getNotAfter()));
    }

}
