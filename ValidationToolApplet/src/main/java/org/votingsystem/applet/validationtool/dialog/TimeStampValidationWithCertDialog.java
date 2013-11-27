package org.votingsystem.applet.validationtool.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.applet.validationtool.panel.MessagePanel;
import org.votingsystem.applet.validationtool.util.Formatter;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.CertUtil;

import javax.swing.*;
import java.awt.*;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class TimeStampValidationWithCertDialog extends JDialog {

    private static Logger logger = Logger.getLogger(TimeStampValidationWithCertDialog.class);

    private Container container;
    private MessagePanel messagePanel;
    TimeStampToken timeStampToken = null;
    
    public TimeStampValidationWithCertDialog(java.awt.Frame parent, boolean modal, TimeStampToken timeStampToken) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(null);
        validationResultPanel.setVisible(false);
        this.timeStampToken = timeStampToken;
        pack();
    }


    private javax.swing.JButton closeButton;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JTextArea pemCertTextArea;
    private javax.swing.JScrollPane scrollPane1;
    private javax.swing.JButton validationButton;
    private javax.swing.JLabel validationResultIconLabel;
    private javax.swing.JLabel validationResultMsgLabel;
    private javax.swing.JPanel validationResultPanel;

    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill", "", "[][]20[]"));
        JLabel messageLabel = new JLabel(ContextVS.getMessage("timeStampValidationWithCertMsg"));
        container.add(messageLabel, "cell 0 0, grow, wrap");
        JScrollPane pemCertScrollPane = new JScrollPane();
        JEditorPane pemCertPane = new JEditorPane();
        pemCertPane.setBackground(java.awt.Color.white);
        pemCertScrollPane.setViewportView(pemCertPane);
        add(pemCertScrollPane, "cell 0 1, width 400::, grow, wrap");

        JButton validateTimeStampButton = new JButton(ContextVS.getMessage("validateLbl"));
        validateTimeStampButton.setIcon(ContextVS.getIcon(this, "accept"));
        validateTimeStampButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { validateTimeStamp();}
        });

        container.add(validateTimeStampButton, "width :150:");

        JButton cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { dispose();}
        });

        container.add(cancelButton, "width :150:, align right");
    }

    private void validateTimeStamp() {
        X509Certificate validationCert = null;
        Collection<X509Certificate> certs = null;
        try {
            String pemCert = pemCertTextArea.getText();
            certs = CertUtil.fromPEMToX509CertCollection(pemCert.getBytes());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            messagePanel.setMessage(ContextVS.getInstance().getMessage("pemCertsErrorMsg"), ContextVS.getIcon(this, "cancel"));
        }
        for(X509Certificate cert:certs) {
            logger.debug(" ----------- Validating timeStampToken with cert: "  + cert.getSubjectDN().toString());
            try {
                timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                        ContextVS.PROVIDER).build(cert));
                messagePanel.setMessage(ContextVS.getInstance().getMessage("pemCertsValidationOKMsg",
                        validationCert.getSubjectDN().toString()), ContextVS.getIcon(this, "accept"));
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                messagePanel.setMessage(ex.getMessage(), ContextVS.getIcon(this, "cancel"));
            }
        }
    }

}
