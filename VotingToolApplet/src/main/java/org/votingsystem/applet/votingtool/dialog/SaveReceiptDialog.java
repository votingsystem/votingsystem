package org.votingsystem.applet.votingtool.dialog;

import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.*;
import org.votingsystem.util.FileUtils;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SaveReceiptDialog extends javax.swing.JDialog {
    
    private static Logger logger = Logger.getLogger(SaveReceiptDialog.class);

    private Container container;

    public SaveReceiptDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        pack();
        setLocationRelativeTo(null);
    }
    
    public void show(final String hashCertVSBase64) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() { saveReceipt(hashCertVSBase64); }
        });   
        setVisible(true);
    }
    
    public void saveReceipt(String hashCertVSBase64) {
        logger.debug(" - saveReceipt - ");
        ResponseVS responseVS = ContextVS.getInstance().getHashCertVSData(hashCertVSBase64);
        File fileToSave = null;
        if(responseVS == null || responseVS.getType() == null || responseVS.getData() == null) {
            logger.error("Missing receipt data");
            sendResponse(ResponseVS.SC_ERROR, ContextVS.getMessage("receiptNotFoundMsg", hashCertVSBase64));
        } else {
            try {
                switch(responseVS.getType()) {
                    case VOTEVS:
                        VoteVS voteVS = (VoteVS)responseVS.getData();
                        fileToSave = FileUtils.getFileFromBytes(voteVS.getReceipt().getBytes());
                        break;
                    case ANONYMOUS_REPRESENTATIVE_SELECTION:
                        fileToSave = getAnonymousRepresentativeSelectionCancelFile(responseVS);
                        break;
                }
                if(fileToSave != null) {
                    fileToSave.deleteOnExit();
                    final JFileChooser chooser = new JFileChooser();
                    chooser.setSelectedFile(fileToSave);
                    int returnVal = chooser.showSaveDialog(new JFrame());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();
                        if (file != null) {
                            fileToSave.renameTo(file);
                            sendResponse(ResponseVS.SC_OK,file.getAbsolutePath());
                            return;
                        }
                    }
                }
                sendResponse(ResponseVS.SC_CANCELLED, null);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                sendResponse(ResponseVS.SC_ERROR, ContextVS.getMessage("operationErrorMsg"));
            }
        }
    }

    private File getAnonymousRepresentativeSelectionCancelFile(ResponseVS responseVS) throws Exception {
        Map delegationDataMap = (Map) responseVS.getData();
        JSONObject messageJSON = (JSONObject) JSONSerializer.toJSON(delegationDataMap);
        java.util.List<File> fileList = new ArrayList<File>();
        File smimeTempFile = File.createTempFile(ContextVS.RECEIPT_FILE_NAME, ".p7s");
        smimeTempFile.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(responseVS.getSmimeMessage().getBytes()), smimeTempFile);
        File certVSDataFile = File.createTempFile(ContextVS.CANCEL_DATA_FILE_NAME, "");
        certVSDataFile.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(messageJSON.toString().getBytes("UTF-8")), certVSDataFile);
        fileList.add(certVSDataFile);
        fileList.add(smimeTempFile);
        File outputZip = File.createTempFile(ContextVS.CANCEL_BUNDLE_FILE_NAME, ".zip");
        outputZip.deleteOnExit();
        FileUtils.packZip(outputZip, fileList);
        return outputZip;
    }

    private void sendResponse(int status, String message) {
        OperationVS operation = new OperationVS();
        operation.setStatusCode(status);
        operation.setMessage(message);
        ContextVS.getInstance().sendMessageToHost(operation);
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