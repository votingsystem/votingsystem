package org.votingsystem.applet.validationtool.panel;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.applet.validationtool.util.Formatter;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.DateUtils;

import javax.swing.*;
import org.votingsystem.applet.validationtool.dialog.TimeStampDialog;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignatureInfoPanel extends JPanel {

    private static Logger logger = Logger.getLogger(SignatureInfoPanel.class);

    private UserVS signer;
    private SMIMEMessageWrapper signedMessage;

    private JCheckBox contentFormattedCheckBox;
    private JEditorPane signatureContentPane;


    public SignatureInfoPanel(UserVS signer, SMIMEMessageWrapper signedMessage) throws Exception {
        this.signer = signer;
        this.signedMessage = signedMessage;
        initComponents();
    }


    private JLabel createBoldLabel(String labelContent) {
        return new JLabel("<html><b>" + labelContent + "</b></html>");
    }

    private void initComponents() {
        setLayout(new MigLayout("fill"));

        this.signedMessage = signedMessage;

        JLabel signatureAlgorithmLabel = createBoldLabel(ContextVS.getMessage("signatureAlgorithmLbl") + ": ");
        add(signatureAlgorithmLabel, "width 200::");
        String algorithValue = signer.getEncryptiontId() + " - " + signer.getDigestId();
        JLabel signatureAlgorithmValueLabel = new JLabel(algorithValue);
        add(signatureAlgorithmValueLabel, "width 200::");

        JLabel signatureDateLabel = createBoldLabel(ContextVS.getMessage("signatureDateLbl") + ": ");
        add(signatureDateLabel, "width 200::, gapleft 30");
        if(signer.getSignatureDate() != null) {
            JLabel signatureDateValueLabel = new JLabel(DateUtils.getSpanishFormattedStringFromDate(
                    signer.getSignatureDate()));
            add(signatureDateValueLabel, "width 200::, wrap");
        }

        JLabel signerLabel = createBoldLabel(ContextVS.getMessage("Firmante") + ": ");
        add(signerLabel, "width 200::, span 2");


        if(signer.getTimeStampToken() == null) {
            JButton timeStampButton = new JButton(ContextVS.getMessage("timeStampButtonLbl"));
            timeStampButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) { openTimeStampInfoDialog();}
            });
            add(timeStampButton, "span 2, wrap");
        } else {
            JPanel dummyPanel = new JPanel();
            add(dummyPanel, "span 2, wrap");
        }

        JScrollPane signerInfoScrollPane = new JScrollPane();
        JEditorPane signerInfoPane = new JEditorPane();
        signerInfoPane.setEditable(false);
        signerInfoPane.setContentType("text/html");
        signerInfoPane.setBackground(java.awt.Color.white);
        signerInfoPane.setText(Formatter.getInfoCert(signer.getCertificate()));
        signerInfoScrollPane.setViewportView(signerInfoPane);
        add(signerInfoScrollPane, "span 4, wrap");

        JLabel signatureContentLabel = createBoldLabel(ContextVS.getMessage("signatureContentLbl") + ": ");
        add(signatureContentLabel, "span 2");

        contentFormattedCheckBox = new JCheckBox(ContextVS.getMessage("formattedCheckBoxLbl"));
        contentFormattedCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { contentFormattedCheckBoxChanged();}
        });
        add(contentFormattedCheckBox, "span 2, wrap");

        JScrollPane signatureContentScrollPane = new JScrollPane();
        signatureContentPane = new JEditorPane();
        signatureContentPane.setEditable(false);
        signatureContentPane.setContentType("text/html");
        signatureContentPane.setBackground(java.awt.Color.white);
        signatureContentPane.setText(signedMessage.getSignedContent());
        signatureContentScrollPane.setViewportView(signatureContentPane);
        add(signatureContentScrollPane, "span 4, wrap");

        JLabel hashBase64Label = createBoldLabel(ContextVS.getMessage("hashBase64Lbl") + ": ");
        add(hashBase64Label, "span 4, wrap");
        JTextField hashBase64Value = new JTextField(signer.getContentDigestBase64());
        add(hashBase64Value, "span 4, wrap");

        JLabel hashHexadecimalLabel = createBoldLabel(ContextVS.getMessage("hashHexadecimalLbl") + ": ");
        add(hashHexadecimalLabel, "span 4, wrap");
        JTextField hashHexadecimalValue = new JTextField(signer.getContentDigestHex());
        add(hashHexadecimalValue, "span 4, wrap");

        JLabel signatureBase64lLabel = createBoldLabel(ContextVS.getMessage("signatureBase64lLbl") + ": ");
        add(signatureBase64lLabel, "span 4, wrap");
        JTextField signatureBase64Value = new JTextField(signer.getSignatureBase64());
        add(signatureBase64Value, "span 4, wrap");

        JLabel signatureHexadecimalLabel = createBoldLabel(ContextVS.getMessage("signatureHexadecimalLbl") + ": ");
        add(signatureHexadecimalLabel, "span 4, wrap");
        JTextField signatureHexadecimalValue = new JTextField(signer.getSignatureHex());
        add(signatureHexadecimalValue, "span 4, wrap");
    }

    private void openTimeStampInfoDialog() {
        TimeStampToken timeStampToken = signer.getTimeStampToken();
        if(timeStampToken == null) {
            logger.debug("TimeStampToken NULL");
            return;
        }
        TimeStampDialog timeStampDialog = new TimeStampDialog(
                new JFrame(), true, timeStampToken);
        timeStampDialog.setVisible(true);
    }


    public void contentFormattedCheckBoxChanged() {
        logger.debug("contentFormattedCheckBoxChanged");
        String formattedText = null;
        if (!contentFormattedCheckBox.isSelected()) {
            try {
                formattedText = Formatter.procesar( signedMessage.getSignedContent());
                signatureContentPane.setText(formattedText);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else signatureContentPane.setText(signedMessage.getSignedContent());
    }
}
