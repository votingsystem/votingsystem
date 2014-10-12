package org.votingsystem.client.pane;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.DateUtils;

import java.io.File;
import java.security.cert.X509Certificate;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampCertPane extends GridPane {

    private static Logger log = Logger.getLogger(TimeStampCertPane.class);

    private X509Certificate certificate;

    public TimeStampCertPane(X509Certificate certificate, boolean isSigner) {
        this.certificate = certificate;
        if(isSigner) {
            Label signerLabel = new Label(ContextVS.getMessage("timeStampCertSignerLbl"));
            add(signerLabel, 0, 0);
        }

        Button showPEMcertButton = new Button(ContextVS.getMessage("showCertPemLbl"));
        showPEMcertButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                showPEMCert();
            }
        });
        showPEMcertButton.setGraphic(new ImageView(Utils.getImage(this, "application-certificate")));
        add(showPEMcertButton, 0, 1);

        WebView certInfoWebView = new WebView();
        certInfoWebView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        certInfoWebView.setPrefHeight(400);
        add(certInfoWebView, 0, 2);
    }

    private void showPEMCert() {
        try {
            String message = new String(CertUtils.getPEMEncoded(certificate));
            MessageDialog messageDialog = new MessageDialog();
            messageDialog.showMessage(certificate.getSubjectDN().toString() + " - " + message);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public static String getCertInfo (X509Certificate certificate) {
        return ContextVS.getInstance().getMessage("certInfoFormattedMsg",certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),certificate.getSerialNumber().toString(),
                DateUtils.getDateStr(certificate.getNotBefore(),"dd/MMM/yyyy HH:mm"),
                DateUtils.getDateStr(certificate.getNotAfter(), "dd/MMM/yyyy HH:mm"));
    }

}
