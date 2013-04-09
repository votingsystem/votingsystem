package org.sistemavotacion.herramientavalidacion;

import java.awt.Frame;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import javax.swing.JFrame;
import javax.swing.JSeparator;
import net.miginfocom.swing.MigLayout;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.GenTimeAccuracy;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.util.CollectionStore;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class TimeStampDialog extends javax.swing.JDialog {
    
    private static Logger logger = LoggerFactory.getLogger(TimeStampDialog.class);

    TimeStampToken timeStampToken;
            
    
    public TimeStampDialog(java.awt.Frame parent, 
            boolean modal, TimeStampToken timeStampToken) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(null);
        dateTextField.setEditable(false);
        serialNumberTextField.setEditable(false);
        certSignerSerialNumberTextField.setEditable(false);
        certIssuerTextField.setEditable(false);
        this.timeStampToken = timeStampToken;
        TimeStampTokenInfo tsInfo= timeStampToken.getTimeStampInfo();
        serialNumberTextField.setText(tsInfo.getSerialNumber().toString());
        
        logger.debug ("tsInfo.toString(): " + tsInfo.toString());  
        
        SignerId signerId = timeStampToken.getSID();
        BigInteger cert_serial_number = signerId.getSerialNumber();
        dateTextField.setText(DateUtils.
                getSpanishFormattedStringFromDate(tsInfo.getGenTime()));
        certSignerSerialNumberTextField.setText(
                signerId.getSerialNumber().toString());
        certIssuerTextField.setText(signerId.getIssuerAsString());

        logger.debug ("signerId.toString(): " + signerId.toString());        
        
        CollectionStore store = (CollectionStore) timeStampToken.getCertificates();
        Collection<X509CertificateHolder> matches = store.getMatches(null);
        logger.debug ("matches.size(): " + matches.size());
        
        if(matches.size() > 0) {
            boolean validationOk = false;
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
                            setProvider(BouncyCastleProvider.PROVIDER_NAME).build(certificateHolder));
                        logger.debug ("Validation OK");
                        certValidationButton.setVisible(false);
                        validationOk = true;
                        isSigner = true;
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
                try {
                    X509Certificate certificate = new JcaX509CertificateConverter().
                            getCertificate(certificateHolder);
                    TimeStampCertPanel timeStampCertPanel = 
                            new TimeStampCertPanel(certificate, isSigner);
                    certsPanel.add(timeStampCertPanel, "wrap");
                    JSeparator separator = new JSeparator();
                    certsPanel.add(separator, "growx, wrap");
                    logger.debug (" ----- Añadido panel de certificado ----- ");
                } catch (CertificateException ex) {
                    logger.error(ex.getMessage(), ex);
                }
                if(!validationOk) {
                    setMessage(AppletHerramienta.getString(
                            "timeStampWithoutCertErrorMsg"));
                }
            }
        } else {
            certsPanel.setVisible(false);
        }
        //GenTimeAccuracy accuracy = tsInfo.getGenTimeAccuracy();
        //assertEquals(3, accuracy.getSeconds());
        //assertEquals(1, accuracy.getMillis());
        //assertEquals(2, accuracy.getMicros());
        //AttributeTable  table = timeStampToken.getSignedAttributes();
        pack();
    }
    
    private void setMessage(String message) {
        if(message == null || "".equals(message)) {
            messageLabel.setVisible(false);
        } else {
            messageLabel.setText(message);
            messageLabel.setVisible(true);
        }
        pack();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFormattedTextField1 = new javax.swing.JFormattedTextField();
        dateLabel = new javax.swing.JLabel();
        dateTextField = new javax.swing.JTextField();
        serialNumberLabel = new javax.swing.JLabel();
        serialNumberTextField = new javax.swing.JTextField();
        certSignerSerialNumberLabel = new javax.swing.JLabel();
        certSignerSerialNumberTextField = new javax.swing.JTextField();
        certIssuerLabel = new javax.swing.JLabel();
        certIssuerTextField = new javax.swing.JTextField();
        certsPanel = new javax.swing.JPanel();
        closeButton = new javax.swing.JButton();
        messageLabel = new javax.swing.JLabel();
        certValidationButton = new javax.swing.JButton();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/herramientavalidacion/Bundle"); // NOI18N
        jFormattedTextField1.setText(bundle.getString("TimeStampDialog.jFormattedTextField1.text")); // NOI18N

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("TimeStampDialog.title")); // NOI18N

        dateLabel.setBackground(java.awt.Color.white);
        dateLabel.setText(bundle.getString("TimeStampDialog.dateLabel.text")); // NOI18N

        dateTextField.setText(bundle.getString("TimeStampDialog.dateTextField.text")); // NOI18N

        serialNumberLabel.setText(bundle.getString("TimeStampDialog.serialNumberLabel.text")); // NOI18N

        serialNumberTextField.setText(bundle.getString("TimeStampDialog.serialNumberTextField.text")); // NOI18N

        certSignerSerialNumberLabel.setText(bundle.getString("TimeStampDialog.certSignerSerialNumberLabel.text")); // NOI18N

        certSignerSerialNumberTextField.setText(bundle.getString("TimeStampDialog.certSignerSerialNumberTextField.text")); // NOI18N

        certIssuerLabel.setText(bundle.getString("TimeStampDialog.certIssuerLabel.text")); // NOI18N

        certIssuerTextField.setText(bundle.getString("TimeStampDialog.certIssuerTextField.text")); // NOI18N

        certsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("TimeStampDialog.certsPanel.border.title"))); // NOI18N

        javax.swing.GroupLayout certsPanelLayout = new javax.swing.GroupLayout(certsPanel);
        certsPanel.setLayout(certsPanelLayout);
        certsPanelLayout.setHorizontalGroup(
            certsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 711, Short.MAX_VALUE)
        );
        certsPanelLayout.setVerticalGroup(
            certsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 202, Short.MAX_VALUE)
        );

        closeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/cancel_16x16.png"))); // NOI18N
        closeButton.setText(bundle.getString("TimeStampDialog.closeButton.text")); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        messageLabel.setText(bundle.getString("TimeStampDialog.messageLabel.text")); // NOI18N

        certValidationButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/stock_certificate_16x16.png"))); // NOI18N
        certValidationButton.setText(bundle.getString("TimeStampDialog.certValidationButton.text")); // NOI18N
        certValidationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                certValidationButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(certIssuerTextField))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(certValidationButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(closeButton))
                    .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(certsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(certSignerSerialNumberLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(certSignerSerialNumberTextField))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(serialNumberLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 268, Short.MAX_VALUE)
                                    .addComponent(dateLabel))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(dateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addComponent(serialNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addComponent(certIssuerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dateLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serialNumberLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(serialNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(certSignerSerialNumberLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(certSignerSerialNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(certIssuerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(certIssuerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(certsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(messageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(certValidationButton)
                    .addComponent(closeButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void certValidationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_certValidationButtonActionPerformed
        Frame frame;
        Frame[] frames = JFrame.getFrames();
        if(frames.length == 0 || frames[0] == null) frame = new javax.swing.JFrame();
        else frame = frames[0];
        TimeStampCertFormDialog certFormDialog = 
                new TimeStampCertFormDialog(frame, true, timeStampToken);
        certFormDialog.setVisible(true);
    }//GEN-LAST:event_certValidationButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TimeStampDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TimeStampDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TimeStampDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TimeStampDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                TimeStampDialog dialog = new TimeStampDialog(new javax.swing.JFrame(), true, null);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel certIssuerLabel;
    private javax.swing.JTextField certIssuerTextField;
    private javax.swing.JLabel certSignerSerialNumberLabel;
    private javax.swing.JTextField certSignerSerialNumberTextField;
    private javax.swing.JButton certValidationButton;
    private javax.swing.JPanel certsPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JLabel dateLabel;
    private javax.swing.JTextField dateTextField;
    private javax.swing.JFormattedTextField jFormattedTextField1;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JLabel serialNumberLabel;
    private javax.swing.JTextField serialNumberTextField;
    // End of variables declaration//GEN-END:variables
}
