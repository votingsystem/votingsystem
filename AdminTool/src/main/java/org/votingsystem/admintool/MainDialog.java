package org.votingsystem.admintool;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.admintool.dialog.DecompressFileDialog;
import org.votingsystem.admintool.dialog.SettingsDialog;
import org.votingsystem.admintool.dialog.SignedDocumentsBrowser;
import org.votingsystem.admintool.util.BrowserVS;
import org.votingsystem.admintool.util.VicketUserGroupAdminListener;
import org.votingsystem.model.AppHostVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.security.Security;
import java.util.UUID;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MainDialog extends JDialog implements DecompressFileDialog.Listener, AppHostVS {
        
    private static Logger logger = Logger.getLogger(MainDialog.class);

    private Container container;
    private BrowserVS browserVS;
    public static String locale = "es";

    public MainDialog(java.awt.Frame parentFrame, boolean modal) {
        super(parentFrame, modal);
        ContextVS.initAdminTool(MainDialog.this, "log4jAdminTool.properties",
                "adminToolMessages.properties", locale);
        initComponents();
        setTitle(ContextVS.getInstance().getMessage("mainDialogCaption"));
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill, insets 20 30 10 30", "", ""));

        JButton openSignedFileButton = new JButton(ContextVS.getMessage("openSignedFileButtonLbl"));
        openSignedFileButton.setIcon(ContextVS.getIcon(this, "application-certificate"));
        openSignedFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openSignedFile();}
        });
        container.add(openSignedFileButton, "cell 0 0, width :400:, wrap 10");

        JButton openBackupButton = new JButton(ContextVS.getMessage("openBackupButtonLbl"));
        openBackupButton.setIcon(ContextVS.getIcon(this, "fa-archive"));
        openBackupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openBackup();}
        });
        container.add(openBackupButton, "cell 0 1, width :400:, wrap 10");

        JButton groupAdminButton = new JButton(ContextVS.getMessage("groupAdminButtonLbl"));
        groupAdminButton.setIcon(ContextVS.getIcon(this, "group"));
        groupAdminButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openGroupAdmin();}
        });
        container.add(groupAdminButton, "cell 0 2, width :400:, wrap 20");


        JButton toolsButton = new JButton(ContextVS.getMessage("settingsLbl"));
        toolsButton.setIcon(ContextVS.getIcon(this, "fa-wrench"));
        toolsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openSettings();}
        });
        container.add(toolsButton, "width :150:, cell 0 3, split2, align right");

        JButton cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel_16"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { cancel();}
        });
        container.add(cancelButton, "width :150:, cell 0 3, gapleft 100, align right");
    }

    private void openGroupAdmin() {
        logger.debug("openGroupAdmin");
        if(browserVS == null) browserVS = new BrowserVS();
        browserVS.setBrowserVSOperator(new VicketUserGroupAdminListener(browserVS));
        browserVS.loadURL("http://vickets/Vickets/groupVS/newGroup");
    }

    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(new JFrame(), true);
        dialog.setVisible(true);
    }

    private void openBackup() {
        logger.debug("openBackup");
        try {
            final JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileFilter() {
                @Override public boolean accept(File f) {
                    return f.getName().toLowerCase().endsWith(".zip") || f.isDirectory();
                }

                public String getDescription() {
                    return "ZIP Files";
                }
            });
            int returnVal = chooser.showOpenDialog(new JFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File result = chooser.getSelectedFile();
                String outputFolder = ContextVS.APPTEMPDIR + File.separator + UUID.randomUUID();
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
        logger.debug("cancel");
        ContextVS.getInstance().sendMessageToHost(new OperationVS(ResponseVS.SC_CANCELLED));
        dispose();
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    logger.debug("init");
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

    @Override public void sendMessageToHost(OperationVS operation) {
        logger.debug("### sendMessageToHost");
    }
}
