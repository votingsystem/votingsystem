package org.votingsystem.client.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.client.model.SignedFile;
import org.votingsystem.client.panel.SignatureInfoPanel;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Set;
import java.util.UUID;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class DocumentSignersDialog extends JDialog {

    private static Logger logger = Logger.getLogger(DocumentSignersDialog.class);

    private Container container;
    private JTabbedPane tabbedPane;

    public DocumentSignersDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug(" - window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
            }
        });
        setTitle(ContextVS.getMessage("signersLbl"));
        pack();
        setLocationRelativeTo(null);
    }


    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill"));

        tabbedPane = new JTabbedPane();
        container.add(tabbedPane, "grow, wrap 20");

        JButton cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel_16"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel();
            }
        });
        container.add(cancelButton, "height 35:35:35, width :150:, align right");
    }

    public void show (SignedFile signedFile) throws Exception {
        if(signedFile.isPDF()) {
            SignatureInfoPanel signerVSPanel = new SignatureInfoPanel(signedFile);
            String tabName = ContextVS.getMessage("signerLbl");
            if(signedFile.getPdfDocument().getUserVS() != null)
                tabName = signedFile.getPdfDocument().getUserVS().getNif();
            tabbedPane.addTab(tabName, signerVSPanel);
        } else {
            Set<UserVS> signersVS = signedFile.getSMIMEMessageWraper().getSigners();
            logger.debug("Num. signers: " + signersVS.size());
            for (UserVS signerVS:signersVS) {
                SignatureInfoPanel signerVSPanel = new SignatureInfoPanel(signerVS, signedFile.getSMIMEMessageWraper());
                String tabName = ContextVS.getMessage("signerLbl");
                if(signerVS.getNif() != null) tabName = signerVS.getNif();
                tabbedPane.addTab(tabName, signerVSPanel);
            }
        }
        pack();
        setVisible(true);
    }

    private void cancel() {
        logger.debug("cancel");
        dispose();
    }

    public static void main(String args[]) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ContextVS.init(null, "log4jClientTool.properties", "clientToolMessages_", "es");
                    String zipFile = "./representative_00000001R.zip";
                    String outputFolder = ContextVS.APPTEMPDIR +  File.separator + UUID.randomUUID();
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    DocumentSignersDialog dialog = new DocumentSignersDialog(new JFrame(), true);
                    dialog.setVisible(true);
                    //dialog.unZipBackup(zipFile, outputFolder);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }

}