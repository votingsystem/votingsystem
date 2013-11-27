package org.votingsystem.applet.validationtool.panel;

import java.awt.Desktop;
import java.awt.Frame;
import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.util.Formatter;
import org.votingsystem.model.ContextVS;
import javax.swing.*;
import org.votingsystem.applet.validationtool.dialog.DocumentSignersDialog;
import org.votingsystem.applet.validationtool.model.SignedFile;

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
        initComponents(signedFile);
    }

    private JLabel createBoldLabel(String labelContent) {
        return new JLabel("<html><b>" + labelContent + "</b></html>");
    }
    
    public void changeContentFormat() {
        logger.debug("changeContentFormat: " + contentFormattedCheckBox.isSelected());
        if(signedFile.isPDF()) return;
        if (contentFormattedCheckBox.isSelected()) {
            try {
                String formattedText = Formatter.procesar(signedFile.
                        getSMIMEMessageWraper().getSignedContent()); 
                contentPane.setText(formattedText);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else contentPane.setText(signedFile.getSMIMEMessageWraper().getSignedContent());        
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
            /*JFrame frame = null;
            Component component = SwingUtilities.getRoot(this);
            if(component instanceof JFrame) {
                frame = (JFrame)component;
            }
            while(frame == null) {
                component = component.getParent();
                if(component instanceof JFrame) frame = (JFrame)component;
            }*/
            Frame frame = Frame.getFrames()[0];
            DocumentSignersDialog signersDialog = new DocumentSignersDialog(frame, true);
            signersDialog.show(signedFile.getSMIMEMessageWraper());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void initComponents(SignedFile signedFile) {
        this.signedFile = signedFile;
        if(signedFile == null) {
            logger.debug("### NULL signedFile");
            return;
        }
        setLayout(new MigLayout("fill"));

        if(!signedFile.isPDF()) {
            JButton signatureResultButton = new JButton();
            signatureResultButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) { openDocumentSignersDialog();}
            });

            JLabel fileNameLabel = new JLabel(signedFile.getName());
            add(fileNameLabel);
            JScrollPane contentPaneScrollPane = new JScrollPane();
            if (signedFile.isValidSignature()) {
                signatureResultButton.setText("<html><b>" + ContextVS.getMessage("signatureOKLbl") + "</b><html>");
                signatureResultButton.setIcon(ContextVS.getIcon(this, "accept"));
                JEditorPane contentPane = new JEditorPane();
                contentPane.setEditable(false);
                contentPane.setContentType("text/html");
                contentPane.setBackground(java.awt.Color.white);
                contentPaneScrollPane.setViewportView(contentPane);
                String conntentStr = null;
                try {
                    JSONObject contentJSON = (JSONObject) JSONSerializer.toJSON(
                            signedFile.getSMIMEMessageWraper().getSignedContent());
                    conntentStr = contentJSON.toString(5);
                }  catch(Exception ex) {
                    conntentStr = signedFile.getSMIMEMessageWraper().getSignedContent();
                }
                contentPane.setText(conntentStr);
            } else {
                signatureResultButton.setText("<html><b>" + ContextVS.getMessage("signatureERRORLbl") + "</b><html>");
                signatureResultButton.setIcon(ContextVS.getIcon(this, "cancel"));
                signatureResultButton.setEnabled(false);
            }
            add(signatureResultButton, "width 200::, gapleft 30, wrap");
            add(contentPaneScrollPane, "width 400::, height 400::, span2, wrap");
        } else {
            JButton openPDFButton = new JButton(ContextVS.getMessage("openPDFLbl"));
            openPDFButton.setIcon(ContextVS.getIcon(this, "file_extension_pdf"));
            openPDFButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) { openPDFDocument();}
            });
            add(openPDFButton, "");
        }

        if(!signedFile.isPDF()) {
            contentFormattedCheckBox = new JCheckBox(ContextVS.getMessage("formattedCheckBoxLbl"));
            contentFormattedCheckBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) { changeContentFormat();}
            });
            add(contentFormattedCheckBox, "span 2, wrap");
        }
    }

}
