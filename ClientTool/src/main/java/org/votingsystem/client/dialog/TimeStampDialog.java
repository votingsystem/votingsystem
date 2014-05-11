package org.votingsystem.client.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.util.CollectionStore;
import org.votingsystem.client.panel.MessagePanel;
import org.votingsystem.client.panel.TimeStampCertPanel;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.DateUtils;

import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class TimeStampDialog extends JDialog {
    
    private static Logger logger = Logger.getLogger(TimeStampDialog.class);

    private Container container;
    TimeStampToken timeStampToken;
            
    
    public TimeStampDialog(java.awt.Frame parent, boolean modal, TimeStampToken timeStampToken) {
        super(parent, modal);
        setTitle(ContextVS.getMessage("timeStampInfoDialogCaption"));
        this.timeStampToken = timeStampToken;
        initComponents();
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill"));

        TimeStampTokenInfo tsInfo= timeStampToken.getTimeStampInfo();
        logger.debug ("timeStampToken.getAttributeCertificates().toString(): " +
                timeStampToken.getAttributeCertificates().getMatches(null).size());

        SignerId signerId = timeStampToken.getSID();
        logger.debug ("signerId.toString(): " + signerId.toString());
        BigInteger cert_serial_number = signerId.getSerialNumber();

        JLabel timeStampDateLabel = createBoldLabel(ContextVS.getMessage("dateGeneratedLbl"));
        container.add(timeStampDateLabel, "width 250::");
        JTextField timeStampTextField =new JTextField(DateUtils.getLongDate_Es(tsInfo.getGenTime()));
        timeStampTextField.setEditable(false);
        container.add(timeStampTextField, "width 250::, wrap");

        JLabel serialNumberLabel = createBoldLabel(ContextVS.getMessage("timeStampSerialNumberLbl"));
        container.add(serialNumberLabel, "width 250::");
        JTextField serialNumberTextField = new JTextField(tsInfo.getSerialNumber().toString());
        serialNumberTextField.setEditable(false);
        container.add(serialNumberTextField, "width 250::, wrap");

        JLabel certSignerSerialNumberLabel = createBoldLabel(ContextVS.getMessage("certSignerSerialNumberLbl"));
        container.add(certSignerSerialNumberLabel, "width 250::");
        JTextField certSignerSerialNumberTextField =new JTextField(signerId.getSerialNumber().toString());
        certSignerSerialNumberTextField.setEditable(false);
        container.add(certSignerSerialNumberTextField, "width 250::, wrap");

        JLabel certIssuerLabel = createBoldLabel(ContextVS.getMessage("signingCertIssuerLbl"));
        container.add(certIssuerLabel, "wrap");
        JTextField certIssuerTextField = new JTextField(signerId.getIssuerAsString());
        certIssuerTextField.setEditable(false);
        container.add(certIssuerTextField, "wrap 20, span 2, growx");

        CollectionStore store = (CollectionStore) timeStampToken.getCertificates();
        Collection<X509CertificateHolder> matches = store.getMatches(null);
        logger.debug ("matches.size(): " + matches.size());

        if(matches.size() > 0) {
            boolean validationOk = false;
            JPanel certsPanel = new JPanel();
            certsPanel.setLayout(new MigLayout());
            for(X509CertificateHolder certificateHolder : matches) {
                boolean isSigner = false;
                logger.debug ("cert_serial_number: '" + cert_serial_number +
                        "' - serial number: '" + certificateHolder.getSerialNumber() + "'");
                if(certificateHolder.getSerialNumber().compareTo(cert_serial_number) == 0) {
                    try {
                        logger.debug ("certificateHolder.getSubject(): "
                                + certificateHolder.getSubject() +
                                " - serial number" + certificateHolder.getSerialNumber());
                        timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().
                                setProvider(ContextVS.PROVIDER).build(certificateHolder));
                        logger.debug ("Validation OK");
                        validationOk = true;
                        isSigner = true;
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
                try {
                    X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);
                    TimeStampCertPanel timeStampCertPanel = new TimeStampCertPanel(certificate, isSigner);
                    certsPanel.add(timeStampCertPanel, "wrap");
                    JSeparator separator = new JSeparator();
                    certsPanel.add(separator, "growx, wrap");
                } catch (CertificateException ex) {
                    logger.error(ex.getMessage(), ex);
                }
                if(!validationOk) {
                    MessagePanel messagePanel = new MessagePanel();
                    messagePanel.setMessage(ContextVS.getInstance().getMessage("timeStampWithoutCertErrorMsg"), null);
                    container.add(messagePanel, "grow, wrap");

                }
            }
        }
        //GenTimeAccuracy accuracy = tsInfo.getGenTimeAccuracy();
        //assertEquals(3, accuracy.getSeconds());
        //assertEquals(1, accuracy.getMillis());
        //assertEquals(2, accuracy.getMicros());
        //AttributeTable  table = timeStampToken.getSignedAttributes();
        JButton certValidationButton = new JButton(ContextVS.getMessage("validateLbl"));
        certValidationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                certValidation();
            }
        });
        container.add(certValidationButton, "width :150:");

        JButton cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel_16"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dispose();
            }
        });
        container.add(cancelButton, "width :150:, align right");
    }

    private JLabel createBoldLabel(String labelContent) {
        return new JLabel("<html><b>" + labelContent + "</b></html>");
    }

    private void certValidation() {
        TimeStampValidationWithCertDialog certFormDialog = new TimeStampValidationWithCertDialog(
                new JFrame(), true, timeStampToken);
        certFormDialog.setVisible(true);
    }

}
