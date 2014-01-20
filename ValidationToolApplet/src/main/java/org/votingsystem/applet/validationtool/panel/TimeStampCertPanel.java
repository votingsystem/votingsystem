package org.votingsystem.applet.validationtool.panel;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.dialog.MessageDialog;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.DateUtils;

import javax.swing.*;
import java.security.cert.X509Certificate;

/**
 * Created by jgzornoza on 23/11/13.
 */
public class TimeStampCertPanel extends JPanel {

    private static Logger logger = Logger.getLogger(TimeStampCertPanel.class);

    private X509Certificate certificate;

    public TimeStampCertPanel(X509Certificate certificate, boolean isSigner) {
        this.certificate = certificate;
        initComponents(isSigner);
    }

    private void initComponents(boolean isSigner) {
        logger.debug("initComponents");
        setLayout(new MigLayout("fill", "[][]"));
        if(isSigner) {
            JLabel signerLabel = new JLabel(ContextVS.getMessage("timeStampCertSignerLbl"));
            signerLabel.setFont(new java.awt.Font("DejaVu Sans", 3, 15));
            signerLabel.setForeground(new java.awt.Color(35, 113, 30));
            add(signerLabel);
        }

        JButton showPEMcertButton = new JButton(ContextVS.getMessage("showCertPemLbl"));
        showPEMcertButton.setIcon(ContextVS.getIcon(this, "application-certificate"));
        showPEMcertButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { showPEMCert();}
        });
        add(showPEMcertButton, "width :150:, gapleft 30, align right, wrap");



        JScrollPane scrollPane = new JScrollPane();
        JEditorPane certEditorPane = new JEditorPane();
        certEditorPane.setEditable(false);
        certEditorPane.setContentType("text/html");
        certEditorPane.setBackground(java.awt.Color.white);
        scrollPane.setViewportView(certEditorPane);
        certEditorPane.setText(getCertInfo(certificate));
        add(scrollPane, "grow, wrap");
    }

    private void showPEMCert() {
        try {
            String message = new String(CertUtil.getPEMEncoded(certificate));
            MessageDialog messageDialog = new MessageDialog(new JFrame(), true);
            messageDialog.showMessage(message, certificate.getSubjectDN().toString());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public static String getCertInfo (X509Certificate certificate) {
        return ContextVS.getInstance().getMessage("certInfoFormattedMsg",certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),certificate.getSerialNumber().toString(),
                DateUtils.getLongDate_Es(certificate.getNotBefore()),
                DateUtils.getLongDate_Es(certificate.getNotAfter()));
    }

}