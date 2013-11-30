package org.votingsystem.applet.votingtool.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.util.FileUtils;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SaveReceiptDialog extends javax.swing.JDialog {
    
    private static Logger logger = Logger.getLogger(SaveReceiptDialog.class);

    private Container container;

    public SaveReceiptDialog(Frame parent, boolean modal) {
        super(parent, modal);
        setLocationRelativeTo(null);
        initComponents();
        parent.setLocationRelativeTo(null);
    }
    
    public void show(final String hashCertVoteBase64) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() { saveReceipt(hashCertVoteBase64); }
        });   
        setVisible(true);
    }
    
    public void saveReceipt(String hashCertVoteBase64) {
        logger.debug(" - saveReceipt - ");
        VoteVS voteVS = ContextVS.getInstance().getVote(hashCertVoteBase64);
        OperationVS operationVS = new OperationVS(ResponseVS.SC_CANCELLED);
        if(voteVS != null) {
            String result = ContextVS.getInstance().getMessage("operationCancelled");
            try {
                final JFileChooser chooser = new JFileChooser();
                File voteVSFile = FileUtils.getFileFromBytes(voteVS.getReceipt().getBytes());
                chooser.setSelectedFile(voteVSFile);
                int returnVal = chooser.showSaveDialog(new JFrame());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if (file.getName().indexOf(".") == -1) {
                        String fileName = file.getAbsolutePath();
                        file = new File(fileName);
                    }
                    if (file != null) {
                        voteVSFile.renameTo(file);
                        result = file.getAbsolutePath();
                        operationVS.setStatusCode(ResponseVS.SC_OK);
                    }
                }
                voteVSFile.delete();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            operationVS.setMessage(result);
            logger.debug("- saveReceipt - result: " + result);
        } else {
            logger.debug(" - Receipt Null - ");
            operationVS.setStatusCode(ResponseVS.SC_ERROR_REQUEST);
            operationVS.setMessage(ContextVS.getInstance().getMessage("receiptNotFoundMsg", hashCertVoteBase64));
        }
        ContextVS.getInstance().sendMessageToHost(operationVS);
        dispose();
    }

    private void initComponents() {
        logger.debug("initComponents");
        container = getContentPane();
        container.setLayout(new MigLayout("fill"));
        JLabel messageLabel = new JLabel(ContextVS.getMessage("saveReceiptLbl"));
        messageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        add(messageLabel, "growx, wrap");
    }

    public static void main(String args[]) {

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    ContextVS.initSignatureApplet(null, "log4j.properties", "messages_", "es");
                    final SaveReceiptDialog dialog = new SaveReceiptDialog(new javax.swing.JFrame(), true);
                    dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            dialog.dispose();
                        }
                    });
                    dialog.show("Message");
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }
}