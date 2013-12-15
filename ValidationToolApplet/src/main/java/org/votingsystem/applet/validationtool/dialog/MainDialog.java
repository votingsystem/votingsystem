package org.votingsystem.applet.validationtool.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.UUID;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MainDialog extends JDialog implements DecompressFileDialog.Listener {
        
    private static Logger logger = Logger.getLogger(MainDialog.class);

    private Container container;

    public MainDialog(java.awt.Frame parentFrame, boolean modal) {
        super(parentFrame, modal);
        initComponents();
        setLocationRelativeTo(null);   
        setTitle(ContextVS.getInstance().getMessage("mainDialogCaption"));
        pack();
    }

    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill, insets 20 30 10 30", "", ""));

        JButton openSignedFileButton = new JButton(ContextVS.getMessage("openSignedFileButtonLbl"));
        openSignedFileButton.setIcon(ContextVS.getIcon(this, "document_signature"));
        openSignedFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openSignedFile();}
        });
        container.add(openSignedFileButton, "cell 0 0, width :400:, wrap 10");

        JButton openBackupButton = new JButton(ContextVS.getMessage("openBackupButtonLbl"));
        openBackupButton.setIcon(ContextVS.getIcon(this, "file_extension_zip"));
        openBackupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openBackup();}
        });
        container.add(openBackupButton, "cell 0 1, width :400:, wrap 20");

        JButton cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel_16"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { cancel();}
        });
        container.add(cancelButton, "cell 0 2, width :150:, align right");

    }

    private void openBackup() {
        logger.debug("openBackup");
        try {
            final JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().toLowerCase().endsWith(".zip") || f.isDirectory();
                }

                public String getDescription() {
                    return "ZIP Files";
                }
            });
            int returnVal = chooser.showOpenDialog(new JFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File result = chooser.getSelectedFile();
                String outputFolder = ContextVS.APPTEMPDIR +
                    File.separator + UUID.randomUUID();
                DecompressFileDialog dialog = new DecompressFileDialog(new JFrame(), true);
                dialog.unZipBackup(this,result.getAbsolutePath(), outputFolder); 
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void openSignedFile() {
        logger.debug("openSignedFile");
        SignedDocumentsBrowser documentBrowser =  new SignedDocumentsBrowser(new JFrame(), false);
        documentBrowser.openSignedFile();
    }                                                    

    private void cancel() {
        logger.debug("close");
        ContextVS.getInstance().sendMessageToHost(new OperationVS(ResponseVS.SC_CANCELLED));
        dispose();
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ContextVS.init(null, "log4jValidationTool.properties", "validationToolMessages_", "es");
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    MainDialog dialog = new MainDialog(new JFrame(), true);
                    dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override public void windowClosing(java.awt.event.WindowEvent e) {
                            System.exit(0);
                        }
                    });
                    dialog.setVisible(true);
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }

    @Override public void processDecompressedFile(ResponseVS response) {
        logger.debug("processDecompressedFile - statsusCode:" + response.getStatusCode());
        if(ResponseVS.SC_OK == response.getStatusCode()) {
            SignedDocumentsBrowser documentBrowser = new SignedDocumentsBrowser(new JFrame(), false);
            documentBrowser.setVisible((String) response.getData());
        }
    }

}
