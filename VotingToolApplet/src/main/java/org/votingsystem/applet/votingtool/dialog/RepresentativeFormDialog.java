package org.votingsystem.applet.votingtool.dialog;

import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.applet.callable.RepresentativeDataSender;
import org.votingsystem.signature.dnie.DNIeContentSignerImpl;
import org.votingsystem.applet.votingtool.panel.ImagePreviewPanel;
import org.votingsystem.applet.votingtool.panel.ProgressBarPanel;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.VotingSystemException;
import org.votingsystem.util.FileUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.Future;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeFormDialog extends JDialog {

    private static Logger logger = Logger.getLogger(RepresentativeFormDialog.class);

    private Container container;
    private ProgressBarPanel progressBarPanel;
    private JPanel formsPanel;
    private JPanel messagePanel;
    private JLabel messageLabel;
    private JLabel selectedImageLabel;
    private JButton cancelButton;
    private JButton signAndSendButton;
    private JButton openDocumentButton;
    private OperationVS operation;
    private Future executingTask;

    private File selectedImage = null;
    private Future<ResponseVS> tareaEnEjecucion;


    public RepresentativeFormDialog(Frame parent, boolean modal) {
        super(parent, modal);
        //parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { logger.debug(" - window closed event received"); }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
                sendResponse(ResponseVS.SC_CANCELLED, ContextVS.getInstance().getMessage("operationCancelled"));
            }
        });
        pack();
    }


    public void show(OperationVS operation) {
        this.operation = operation;
        setVisible(true);
    }

    private void initComponents() {
        container = getContentPane();
        //5 rows: progress, message panel, select image panel, open document panel and submit buttons
        container.setLayout(new MigLayout("fill", "", "[][][]20[]"));
        progressBarPanel = new ProgressBarPanel();

        messagePanel = new JPanel();
        messagePanel.setLayout(new MigLayout("fill"));
        messageLabel = new JLabel();
        messageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        messagePanel.add(messageLabel, "growx, wrap");

        Border panelBorder = BorderFactory.createLineBorder(Color.GRAY, 1);

        formsPanel = new JPanel(new MigLayout("fill"));
        container.add(formsPanel, "cell 0 2, growx, wrap");
        
        JPanel selectImagePanel = new JPanel();
        selectImagePanel.setBorder(panelBorder);
        selectImagePanel.setLayout(new MigLayout("fill", "15[grow]15"));
        JButton selectImageButton = new JButton(ContextVS.getMessage("selectImageMsg"));
        selectImageButton.setIcon(ContextVS.getIcon(this, "group"));
        selectImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { selectRepresentativeImage();}
        });
        selectImagePanel.add(selectImageButton, "wrap");
        selectedImageLabel = new JLabel();
        selectImagePanel.add(selectedImageLabel, "wrap");
        formsPanel.add(selectImagePanel, "growx, wrap");

        JPanel openDocumentPanel = new JPanel();
        openDocumentPanel.setBorder(panelBorder);
        openDocumentPanel.setLayout(new MigLayout("fill", "15[grow]15","10[][]10"));

        JLabel formMessageLabel = new JLabel(ContextVS.getMessage("signatureDialogFormMsg"), SwingConstants.CENTER);
        openDocumentPanel.add(formMessageLabel, "growx, wrap");

        openDocumentButton = new JButton(ContextVS.getMessage("openDocumentToSignLbl"));
        openDocumentButton.setIcon(ContextVS.getIcon(this, "open_folder"));
        openDocumentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openDocument();}
        });
        openDocumentPanel.add(openDocumentButton, "wrap");
        formsPanel.add(openDocumentPanel, "growx, wrap");

        signAndSendButton = new JButton(ContextVS.getMessage("signAndSendLbl"));
        signAndSendButton.setIcon(ContextVS.getIcon(this, "document_signature"));
        signAndSendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { signAndSend();}
        });
        container.add(signAndSendButton, "width :150:, cell 0 3, split2, align right");

        cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { cancel();}
        });
        container.add(cancelButton, "width :150:, align right");
    }

    public void showProgressPanel (final boolean visibility, String... messages) {
        if(executingTask == null) logger.debug("showProgressPanel: " + visibility );
        else logger.debug("showProgressPanel: " + visibility + " - executingTask.isDone(): " + executingTask.isDone());
        if(visibility) {
            container.add(progressBarPanel, "cell 0 0, growx, wrap");
            String resultMessage = null;
            for(String message: messages) {
                if(resultMessage == null) resultMessage = message;
                else resultMessage = resultMessage + "<br/>" + message;
            }
            progressBarPanel.setMessage(resultMessage);
            container.remove(formsPanel);
            cancelButton.setText(ContextVS.getMessage("cancelLbl"));
        } else {
            container.add(formsPanel, "cell 0 2, growx, wrap");
            container.remove(progressBarPanel);
            cancelButton.setText(ContextVS.getMessage("closeLbl"));
        }
        signAndSendButton.setVisible(!visibility);
        setMessage(null);
        pack();
    }

    private void sendResponse(int status, String message) {
        if(operation != null) {
            operation.setStatusCode(status);
            operation.setMessage(message);
            ContextVS.getInstance().sendMessageToHost(operation);
        } else {
            logger.debug(" --- operation null --- ");
        }
        dispose();
    }

    private void cancel() {
        if(executingTask == null )  logger.debug("cancel - executingTask null");
        else logger.debug("cancel - executingTask.isDone(): " + executingTask.isDone());

        if(executingTask != null && !executingTask.isDone()) {
            logger.debug(" --- cancelling task ---");
            executingTask.cancel(true);
            showProgressPanel(false);
        } else {
            sendResponse(ResponseVS.SC_CANCELLED, ContextVS.getMessage("operationCancelledMsg"));
            dispose();
        }
    }
    
    private void selectRepresentativeImage() {
        logger.debug(" - selectRepresentativeImage - ");
        try {
            final JFileChooser chooser = new JFileChooser();
            ImagePreviewPanel preview = new ImagePreviewPanel();
            chooser.setAccessory(preview);
            chooser.addPropertyChangeListener(preview);
            int returnVal = chooser.showSaveDialog(new JFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if ((file != null) && file.getName().toLowerCase().endsWith(".jpg") ||
                        file.getName().toLowerCase().endsWith(".jpeg") ||
                        file.getName().toLowerCase().endsWith(".gif") ||
                        file.getName().toLowerCase().endsWith(".png")) {
                    selectedImage = new File(file.getAbsolutePath());
                    byte[] imageFileBytes = FileUtils.getBytesFromFile(selectedImage);
                    logger.debug(" - imageFileBytes.length: " + imageFileBytes.length);
                    if(imageFileBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
                        logger.debug(" - MAX_FILE_SIZE exceeded ");
                        setMessage(ContextVS.getMessage("fileSizeExceeded", ContextVS.IMAGE_MAX_FILE_SIZE_KB));
                        selectedImage = null;
                        selectedImageLabel.setText(ContextVS.getMessage("imageNotSelectedMsg"));
                    } else {
                        selectedImageLabel.setText(file.getAbsolutePath());
                        MessageDigest messageDigest = MessageDigest.getInstance(ContextVS.VOTING_DATA_DIGEST);
                        byte[] resultDigest =  messageDigest.digest(imageFileBytes);
                        String base64ResultDigest = new String(Base64.encode(resultDigest));
                        operation.getDocumentToSignMap().put("base64ImageHash", base64ResultDigest);
                        //String base64RepresentativeEncodedImage = new String(Base64.encode(imageFileBytes));
                        //operation.getContentFirma().put("base64RepresentativeEncodedImage", base64RepresentativeEncodedImage);
                        setMessage(null);
                    }
                } else {
                    selectedImage = null;
                    selectedImageLabel.setText(
                            ContextVS.getInstance().getMessage("imageNotSelectedMsg"));
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }


    private void openDocument() {
        logger.debug("openDocument");
        if (!Desktop.isDesktopSupported()) logger.debug("Desktop Not Supported");
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            logger.debug("Desktop doesn't support file browsing");
        }
        try {
            File tempFile = File.createTempFile(operation.getFileName(), "");
            tempFile.deleteOnExit();
            Map documentToSignMap = operation.getDocumentToSignMap();
            JSONObject documentToSignJSON = (JSONObject) JSONSerializer.toJSON(documentToSignMap);
            FileUtils.copyStreamToFile(new ByteArrayInputStream(documentToSignJSON.toString().getBytes()), tempFile);
            desktop.open(tempFile);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void signAndSend() {
        logger.debug("signAndSend");
        if(selectedImage == null)  setMessage(ContextVS.getMessage("imageMissingMsg"));
        else {
            setMessage(null);
            String password = null;
            PasswordDialog dialogoPassword = new PasswordDialog (new JFrame(), true);
            dialogoPassword.setVisible(true);
            password = dialogoPassword.getPassword();
            if (password == null) return;
            showProgressPanel(true, ContextVS.getMessage("progressLabel"));
            SignerWorker signerWorker = new SignerWorker(password);
            signerWorker.execute();
            tareaEnEjecucion = signerWorker;
            pack();
        }
    }

    private void setMessage (String message) {
        if (message == null) container.remove(messagePanel);
        else {
            container.add(messagePanel, "cell 0 1, growx, wrap");
            messageLabel.setText("<html><div style=\"margin: 5px 0 5px 0;color:#cc1606;\"><b>" +
                    message + "</b></div></html>");
        }
        pack();
    }


    public static void main(String[] args) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ContextVS.initSignatureApplet(null, "log4j.properties", "messages_", "es");
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    RepresentativeFormDialog dialog = new RepresentativeFormDialog(new JFrame(), true);
                    dialog.addWindowListener(new WindowAdapter() {
                        @Override public void windowClosing(WindowEvent e) {
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


    class SignerWorker extends SwingWorker<ResponseVS, Object> {

        private String password = null;

        public SignerWorker(String password) { this.password = password; }

        @Override public ResponseVS doInBackground() throws Exception {
            JSONObject documentToSignJSON = (JSONObject)JSONSerializer.toJSON(operation.getDocumentToSignMap());
            SMIMEMessageWrapper representativeRequestSMIME = DNIeContentSignerImpl.genMimeMessage(null,
                    operation.getNormalizedReceiverName(), documentToSignJSON.toString(), password.toCharArray(),
                    operation.getSignedMessageSubject(), null);
            RepresentativeDataSender dataSender = new RepresentativeDataSender( representativeRequestSMIME, selectedImage,
                    operation.getUrlEnvioDocumento(), ContextVS.getInstance(). getAccessControl().getX509Certificate());
            return dataSender.call();
        }

        @Override protected void done() {
            showProgressPanel(false);
            try {
                ResponseVS responseVS = get();
                logger.debug("SignerWorker.done - response status: " + responseVS.getStatusCode());
                sendResponse(responseVS.getStatusCode(), responseVS.getMessage());
            } catch(Exception ex) {
                String mensajeError = ex.getMessage();
                if(ex.getCause() instanceof VotingSystemException) {
                    mensajeError = ex.getCause().getMessage();
                }
                sendResponse(ResponseVS.SC_ERROR, mensajeError);
            }
        }
    }
 
}
