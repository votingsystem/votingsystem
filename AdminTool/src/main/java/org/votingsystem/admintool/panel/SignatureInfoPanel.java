package org.votingsystem.admintool.panel;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.admintool.dialog.TimeStampDialog;
import org.votingsystem.admintool.model.SignedFile;
import org.votingsystem.admintool.util.Formatter;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.DateUtils;

import javax.swing.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignatureInfoPanel extends JPanel {

    private static Logger logger = Logger.getLogger(SignatureInfoPanel.class);

    private UserVS signer;
    private SMIMEMessageWrapper signedMessage;
    private SignedFile signedFile;
    private String signatureAlgorithmValue = null;

    public SignatureInfoPanel(UserVS signer, SMIMEMessageWrapper signedMessage) throws Exception {
        this.signer = signer;
        this.signedMessage = signedMessage;
        signatureAlgorithmValue = signer.getEncryptiontId() + " - " + signer.getDigestId();
        initComponents();
    }

    public SignatureInfoPanel(SignedFile signedFile) throws Exception {
        this.signedFile = signedFile;
        this.signer = signedFile.getPdfDocument().getUserVS();
        /*signatureAlgorithmValue = signedFile.getPdfPKCS7().getDigestEncryptionAlgorithmOid() + " - " +
                signedFile.getPdfPKCS7().getDigestAlgorithm();*/
        signatureAlgorithmValue = signedFile.getPdfPKCS7().getDigestAlgorithm();
        initComponents();
    }

    private JLabel createBoldLabel(String labelContent) {
        return new JLabel("<html><b>" + labelContent + "</b></html>");
    }

    private void initComponents() {
        setLayout(new MigLayout("fill"));

        JLabel signatureAlgorithmLabel = createBoldLabel(ContextVS.getMessage("signatureAlgorithmLbl") + ": ");
        add(signatureAlgorithmLabel);
        JLabel signatureAlgorithmValueLabel = new JLabel(signatureAlgorithmValue);

        if(signer.getTimeStampToken() != null) {
            add(signatureAlgorithmValueLabel);
            JLabel signatureDateLabel = createBoldLabel(ContextVS.getMessage("signatureDateLbl") + ": ");
            add(signatureDateLabel, "gapleft 50");
            JLabel signatureDateValueLabel = new JLabel(DateUtils.getLongDate_Es(
                    signer.getSignatureDate()));
            add(signatureDateValueLabel, "split 2");
            JButton timeStampButton = new JButton(ContextVS.getMessage("timeStampButtonLbl"));
            timeStampButton.setIcon(ContextVS.getIcon(this, "clock"));
            timeStampButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) { openTimeStampInfoDialog();}
            });
            add(timeStampButton, "gapleft 20, wrap");
        } else  add(signatureAlgorithmValueLabel, "wrap");

        JLabel signerLabel = createBoldLabel(ContextVS.getMessage("signerLbl") + ": ");
        add(signerLabel, "wrap");

        add(getJScrollPane(Formatter.getInfoCert(signer.getCertificate())), "span 4, height 150:150:, grow, wrap");

        if(signedMessage != null) {
            JLabel hashBase64Label = createBoldLabel(ContextVS.getMessage("hashBase64Lbl") + ": ");
            add(hashBase64Label, "span 4, wrap");
            JTextField hashBase64Value = new JTextField(signer.getContentDigestBase64());
            add(hashBase64Value, "span 4, width 800:800:800, wrap");

            JLabel hashHexadecimalLabel = createBoldLabel(ContextVS.getMessage("hashHexadecimalLbl") + ": ");
            add(hashHexadecimalLabel, "span 4, wrap");
            JTextField hashHexadecimalValue = new JTextField(signer.getContentDigestHex());
            add(hashHexadecimalValue, "span 4, width 800:800:800, wrap");

            JLabel signatureBase64Label = createBoldLabel(ContextVS.getMessage("signatureBase64lLbl") + ": ");
            add(signatureBase64Label, "span 4, wrap");
            JTextField signatureBase64Value = new JTextField(signer.getSignatureBase64());
            add(signatureBase64Value, "span 4, width 800:800:800, wrap");

            JLabel signatureHexadecimalLabel = createBoldLabel(ContextVS.getMessage("signatureHexadecimalLbl") + ": ");
            add(signatureHexadecimalLabel, "span 4, wrap");
            JTextField signatureHexadecimalValue = new JTextField(signer.getSignatureHex());
            add(signatureHexadecimalValue, "span 4, width 800:800:800, wrap");
        }
    }

    private JScrollPane getJScrollPane(String contentText) {
        JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setBackground(java.awt.Color.white);
        editorPane.setText(contentText);
        scrollPane.setViewportView(editorPane);
        return scrollPane;
    }

    private void openTimeStampInfoDialog() {
        TimeStampToken timeStampToken = signer.getTimeStampToken();
        if(timeStampToken == null) {
            logger.debug("TimeStampToken NULL");
            return;
        }
        TimeStampDialog timeStampDialog = new TimeStampDialog(new JFrame(), true, timeStampToken);
        timeStampDialog.setVisible(true);
    }

}
