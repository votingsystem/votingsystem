package org.votingsystem.applet.validationtool.panel;

import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.dialog.DocumentSignersDialog;
import org.votingsystem.applet.validationtool.model.SignedFile;
import org.votingsystem.applet.validationtool.util.Formatter;
import org.votingsystem.model.ContextVS;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignedFilePanel extends JPanel {

    private static Logger logger = Logger.getLogger(SignedFilePanel.class);

    private SignedFile signedFile;
    private JEditorPane contentPane;
    private JCheckBox contentFormattedCheckBox;

    public SignedFilePanel(SignedFile signedFile) throws Exception {
        logger.debug("SignedFilePanel");
        this.signedFile = signedFile;
        initComponents(signedFile);
    }

    private JLabel createBoldLabel(String labelContent) {
        return new JLabel("<html><b>" + labelContent + "</b></html>");
    }
      
    private void openPDFDocument() {
        if (!Desktop.isDesktopSupported()) logger.debug("Desktop not supported");
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            logger.debug("Desktop can't browse files");
        }
        try {
            logger.info(" openPDFButtonActionPerformed - signedFile: " + signedFile.getName());
            desktop.open(signedFile.getFile());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    private void openDocumentSignersDialog() {
        try {
            Frame frame = Frame.getFrames()[0];
            DocumentSignersDialog signersDialog = new DocumentSignersDialog(frame, true);
            signersDialog.show(signedFile);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void initComponents(SignedFile signedFile) {
        if(signedFile == null) {
            logger.debug("### NULL signedFile");
            return;
        }
        setLayout(new MigLayout("fill", "", "[35:35:35][][15:15:15]"));

        JButton signatureResultButton = new JButton();
        signatureResultButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openDocumentSignersDialog();}
        });

        if(!signedFile.isPDF()) {
            JLabel fileNameLabel = new JLabel(signedFile.getName());
            add(fileNameLabel);
            JScrollPane contentPaneScrollPane = new JScrollPane();
            if (signedFile.isValidSignature()) {
                signatureResultButton.setText("<html><b>" + ContextVS.getMessage("signatureOKLbl") + "</b><html>");
                signatureResultButton.setIcon(ContextVS.getIcon(this, "accept"));
                contentPane = new JEditorPane();
                contentPane.setEditable(false);
                contentPane.setContentType("text/html");
                contentPane.setBackground(java.awt.Color.white);
                contentPane.addHyperlinkListener(new HyperlinkListener() {
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            if(Desktop.isDesktopSupported()) {
                                try {Desktop.getDesktop().browse(e.getURL().toURI());}
                                catch(Exception ex) {logger.error(ex.getMessage(), ex);}
                            }
                        }
                    }
                });

                contentPaneScrollPane.setViewportView(contentPane);
                String contentStr = null;
                try {
                    JSONObject contentJSON = (JSONObject) JSONSerializer.toJSON(
                            signedFile.getSMIMEMessageWraper().getSignedContent());
                    contentStr = Formatter.format(contentJSON.toString(5));
                }  catch(Exception ex) {
                    contentStr = signedFile.getSMIMEMessageWraper().getSignedContent();
                }
                contentPane.setText(contentStr);
            } else {
                signatureResultButton.setText("<html><b>" + ContextVS.getMessage("signatureERRORLbl") + "</b><html>");
                signatureResultButton.setIcon(ContextVS.getIcon(this, "cancel"));
                signatureResultButton.setEnabled(false);
            }
            add(signatureResultButton, "width 200::, align right, wrap");
            add(contentPaneScrollPane, "height 400::,grow, span2, wrap");

            contentFormattedCheckBox = new JCheckBox(ContextVS.getMessage("formattedCheckBoxLbl"));
            contentFormattedCheckBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) { changeContentFormat();}
            });
            add(contentFormattedCheckBox, "wrap");
            contentFormattedCheckBox.setSelected(true);
        } else {
            JButton openPDFButton = new JButton(ContextVS.getMessage("openPDFLbl"));
            openPDFButton.setIcon(ContextVS.getIcon(this, "file_extension_pdf"));
            openPDFButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) { openPDFDocument();}
            });
            add(openPDFButton, "");
            if (signedFile.isValidSignature()) {
                signatureResultButton.setText("<html><b>" + ContextVS.getMessage("signatureOKLbl") + "</b><html>");
                signatureResultButton.setIcon(ContextVS.getIcon(this, "accept"));
            } else {
                signatureResultButton.setText("<html><b>" + ContextVS.getMessage("signatureERRORLbl") + "</b><html>");
                signatureResultButton.setIcon(ContextVS.getIcon(this, "cancel"));
                signatureResultButton.setEnabled(false);
            }
            add(signatureResultButton, "width 200::, align right, wrap");
        }
    }
    public void changeContentFormat() {
        logger.debug("changeContentFormat: " + contentFormattedCheckBox.isSelected());
        if(signedFile.isPDF()) return;
        if (contentFormattedCheckBox.isSelected()) {
            try {
                String formattedText = Formatter.format(signedFile.getSMIMEMessageWraper().getSignedContent());
                contentPane.setText(formattedText);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else contentPane.setText(signedFile.getSMIMEMessageWraper().getSignedContent());
    }



}
