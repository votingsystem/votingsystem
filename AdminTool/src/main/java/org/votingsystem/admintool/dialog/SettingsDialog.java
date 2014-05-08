package org.votingsystem.admintool.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SettingsDialog extends JDialog {

    private static Logger logger = Logger.getLogger(SettingsDialog.class);

    private Container container;
    private JLabel keyStoreLabel;
    private JCheckBox signWithKeystoreCheckBox;
    private JPanel keyStorePanel;
    private KeyStore userKeyStore;

    public SettingsDialog(Frame parentFrame, boolean modal) {
        super(parentFrame, modal);
        initComponents();
        setTitle(ContextVS.getInstance().getMessage("settingsCaption"));
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        Boolean withKeystore = ContextVS.getInstance().getBoolProperty(ContextVS.WITH_KEYSTORE_PROPERTY, false);
        container = getContentPane();
        //3 rows: cert checkbox, keystore panel and panel buttons
        container.setLayout(new MigLayout("fill, insets 20 30 10 30", "", "[]10[]15[]"));

        signWithKeystoreCheckBox = new JCheckBox(ContextVS.getMessage("setKeyStoreInsteadDNIMsg"));
        signWithKeystoreCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { changeSignatureMode();}
        });
        signWithKeystoreCheckBox.setSelected(withKeystore);

        add(signWithKeystoreCheckBox, "wrap");

        Border panelBorder = BorderFactory.createLineBorder(Color.GRAY, 1);

        keyStorePanel = new JPanel();
        keyStorePanel.setBorder(panelBorder);
        keyStorePanel.setLayout(new MigLayout("fill", "15[grow]15"));
        JButton selectImageButton = new JButton(ContextVS.getMessage("setKeyStoreLbl"));
        selectImageButton.setIcon(ContextVS.getIcon(this, "fa-key"));
        selectImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectKeystoreFile();
            }
        });
        keyStorePanel.add(selectImageButton, "wrap");
        keyStoreLabel = new JLabel();
        keyStorePanel.add(keyStoreLabel, "wrap");
        if(withKeystore) {
            container.add(keyStorePanel, "growx, wrap");
        }

        JButton acceptButton = new JButton(ContextVS.getMessage("acceptLbl"));
        acceptButton.setIcon(ContextVS.getIcon(this, "accept"));
        acceptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { validateSettings();}
        });
        container.add(acceptButton, "width :150:, cell 0 3, split2, align right");

        JButton cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { cancel();}
        });
        container.add(cancelButton, "width :150:, align right");
    }

    private void selectKeystoreFile() {
        logger.debug("selectKeystoreFile");
        try {
            final JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showSaveDialog(new JFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if ((file != null)) {
                    File selectedKeystore = new File(file.getAbsolutePath());
                    byte[] keystoreBytes = FileUtils.getBytesFromFile(selectedKeystore);
                    try {
                        userKeyStore = KeyStoreUtil.getKeyStoreFromBytes(keystoreBytes, null);
                    } catch(Exception ex) {
                        MessageDialog messageDialog = new MessageDialog(new JFrame(), true);
                        messageDialog.showMessage(ContextVS.getMessage("keyStoreNotValidErrorMsg"),
                                ContextVS.getMessage("errorLbl"));
                    }
                    //PrivateKey privateKeySigner = (PrivateKey)userKeyStore.getKey("UserTestKeysStore", null);
                    X509Certificate certSigner = (X509Certificate) userKeyStore.getCertificate("UserTestKeysStore");
                    keyStoreLabel.setText(certSigner.getSubjectDN().toString());
                } else {
                    keyStoreLabel.setText(ContextVS.getMessage("selectKeyStoreLbl"));
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        pack();
    }

    private void changeSignatureMode() {
        logger.debug("changeSignatureMode");
        if (signWithKeystoreCheckBox.isSelected()) {
            container.add(keyStorePanel, "cell 0 2, growx, wrap");
        } else {
            container.remove(keyStorePanel);
            userKeyStore = null;
        }
        pack();
    }

    private void validateSettings() {
        logger.debug("validateSettings");
        Boolean withKeystoreSettings = ContextVS.getInstance().getBoolProperty(ContextVS.WITH_KEYSTORE_PROPERTY, false);
        if(signWithKeystoreCheckBox.isSelected() != withKeystoreSettings) {
            try {
                if(signWithKeystoreCheckBox.isSelected()) {
                    if(userKeyStore == null) {
                        MessageDialog messageDialog = new MessageDialog(new JFrame(), true);
                        messageDialog.showMessage(ContextVS.getMessage("keyStoreNotSelectedErrorLbl"),
                                ContextVS.getMessage("errorLbl"));
                    }
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            } finally {
                if(!signWithKeystoreCheckBox.isSelected())
                    ContextVS.getInstance().setProperty(ContextVS.WITH_KEYSTORE_PROPERTY,
                            String.valueOf(signWithKeystoreCheckBox.isSelected()));
            }
        }
        if(userKeyStore != null) {
            try {
                PasswordDialog passwordDialog = new PasswordDialog (new JFrame(), true);
                passwordDialog.setMainMessage(ContextVS.getMessage("newKeyStorePasswordMsg"),
                        ContextVS.getMessage("setKeyStoreLbl"));
                passwordDialog.setVisible(true);
                String password = passwordDialog.getPassword();
                ContextVS.saveUserKeyStore(userKeyStore, password);
                ContextVS.getInstance().setProperty(ContextVS.WITH_KEYSTORE_PROPERTY,
                        String.valueOf(signWithKeystoreCheckBox.isSelected()));
            } catch(Exception ex) {
                MessageDialog messageDialog = new MessageDialog(new JFrame(), true);
                messageDialog.showMessage(ContextVS.getMessage("errorStoringKeyStoreMsg"),
                        ContextVS.getMessage("errorLbl"));
            }
        }
        dispose();
    }

    private void cancel() {
        logger.debug("cancel");
        dispose();
    }

}
