package org.votingsystem.admintool.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.admintool.panel.MessagePanel;
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
    private JEditorPane pemCertPane;
    private TimeStampToken timeStampToken = null;
    private JButton validateTimeStampButton = null;
    
    public TimeStampValidationWithCertDialog(java.awt.Frame parent, boolean modal, TimeStampToken timeStampToken) {
        super(parent, modal);
        setTitle(ContextVS.getMessage("validateTimeStampDialogCaption"));
        initComponents();
        this.timeStampToken = timeStampToken;
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill", "", ""));


        container.add(new JLabel("<html><b>" + ContextVS.getMessage("timeStampValidationWithCertMsg") + ": </b></html>"),
                "cell 0 1, height 35:35:35, span2, grow, wrap");

        JScrollPane pemCertScrollPane = new JScrollPane();
        pemCertPane = new JEditorPane();
        pemCertPane.setBackground(java.awt.Color.white);
        pemCertScrollPane.setViewportView(pemCertPane);
        add(pemCertScrollPane, "span 2, height 300::, width 500::, grow, wrap 20");

        validateTimeStampButton = new JButton(ContextVS.getMessage("validateLbl"));
        validateTimeStampButton.setIcon(ContextVS.getIcon(this, "accept"));
        validateTimeStampButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { validateTimeStamp();}
        });

        container.add(validateTimeStampButton, "height 35:35:35, width :150:");

        JButton cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { dispose();}
        });

        container.add(cancelButton, "height 35:35:35, width :150:, align right");
    }

    private void validateTimeStamp() {
        logger.debug("validateTimeStamp");
        Collection<X509Certificate> certs = null;
        try {
            String pemCert = pemCertPane.getText();
            certs = CertUtil.fromPEMToX509CertCollection(pemCert.getBytes());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            messagePanel.setMessage(ContextVS.getInstance().getMessage("pemCertsErrorMsg"),
                    ContextVS.getIcon(this, "cancel"));
        }

        messagePanel = new MessagePanel();
        container.add(messagePanel, "cell 0 0, span2, grow, wrap");
        for(X509Certificate cert:certs) {
            logger.debug("Validating timeStampToken with cert: "  + cert.getSubjectDN().toString());
            try {
                timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                        ContextVS.PROVIDER).build(cert));
                messagePanel.setMessage(ContextVS.getMessage("timeStampCertsValidationOKMsg",
                        cert.getSubjectDN().toString()), ContextVS.getIcon(this, "accept"));
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                messagePanel.setMessage(ex.getMessage(), ContextVS.getIcon(this, "cancel"));
            }
        }
        validateTimeStampButton.setVisible(false);
        pack();
    }

}
