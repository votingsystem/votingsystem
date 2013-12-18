package org.votingsystem.applet.votingtool.dialog;


import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.applet.votingtool.panel.ProgressBarPanel;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.model.*;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.VotingSystemException;
import org.votingsystem.util.FileUtils;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;
import java.util.concurrent.Future;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class CancelRepresentationDialog extends JDialog {

    private static Logger logger = Logger.getLogger(CancelRepresentationDialog.class);

    private Container container;
    private ProgressBarPanel progressBarPanel;
    private Future<ResponseVS> runningTask;
    private JButton cancelButton;
    private JButton signAndSendButton;
    private JPanel formPanel;
    private File cancellationData;

    public CancelRepresentationDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        pack();
        setLocationRelativeTo(null);
    }

    @Override public void show() {
        openDataFile();
        setVisible(true);
    }

    public void showProgressPanel (final boolean visibility, String... messages) {
        if(runningTask == null) logger.debug("showProgressPanel: " + visibility );
        else logger.debug("showProgressPanel: " + visibility + " - runningTask.isDone(): " + runningTask.isDone());
        if(visibility) {
            container.add(progressBarPanel, "cell 0 0, growx, wrap");
            String resultMessage = null;
            for(String message: messages) {
                if(resultMessage == null) resultMessage = message;
                else resultMessage = resultMessage + "<br/>" + message;
            }
            progressBarPanel.setMessage(resultMessage);
            container.remove(formPanel);
            cancelButton.setText(ContextVS.getMessage("cancelLbl"));
        } else {
            container.add(formPanel, "cell 0 1, growx, wrap");
            container.remove(progressBarPanel);
            cancelButton.setText(ContextVS.getMessage("closeLbl"));
        }
        signAndSendButton.setVisible(!visibility);
        pack();
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
        progressBarPanel = new ProgressBarPanel();
        container.setLayout(new MigLayout("fill", "", "[][]20[]"));
        JLabel messageLabel = new JLabel(ContextVS.getMessage("saveReceiptLbl"));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(messageLabel, "growx, wrap");

        Border formPanelBorder = BorderFactory.createLineBorder(Color.GRAY, 1);
        formPanel = new JPanel();
        formPanel.setBorder(formPanelBorder);
        formPanel.setLayout(new MigLayout("fill", "15[grow]15",""));

        JButton selectCancelationDataFile = new JButton(ContextVS.getMessage("selectCancelationDataButtonLbl"));
        selectCancelationDataFile.setIcon(ContextVS.getIcon(this, "open_folder"));
        selectCancelationDataFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openDataFile();}
        });
        selectCancelationDataFile.add(signAndSendButton, "width :150:, cell 0 2, split2, align right");


        signAndSendButton = new JButton(ContextVS.getMessage("signAndSendLbl"));
        signAndSendButton.setIcon(ContextVS.getIcon(this, "document_signature_16"));
        signAndSendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { signAndSend();}
        });
        container.add(signAndSendButton, "width :150:, cell 0 2, split2, align right");
        signAndSendButton.setVisible(false);

        cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel_16"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { cancel();}
        });
        container.add(cancelButton, "width :150:, align right");
    }

    private void openDataFile() {
        try {
            final JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showSaveDialog(new JFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File cancellationData = chooser.getSelectedFile();
                if (cancellationData != null) {
                    String outputFolder = ContextVS.APPTEMPDIR + File.separator + UUID.randomUUID();
                    FileUtils.unpackZip(cancellationData, new File(outputFolder));
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        if(cancellationData != null) signAndSendButton.setVisible(true);
    }

    private void signAndSend() {
        logger.debug("signAndSend");
        String password = null;
        PasswordDialog dialogoPassword = new PasswordDialog (new JFrame(), true);
        dialogoPassword.setVisible(true);
        password = dialogoPassword.getPassword();
        if (password == null) return;
        showProgressPanel(true, ContextVS.getMessage("progressLabel"));
        SignedSenderWorker signedSenderWorker = new SignedSenderWorker(password);
        runningTask = signedSenderWorker;
        signedSenderWorker.execute();
        pack();
    }


    private void cancel() {
        if(runningTask == null )  logger.debug("cancel - runningTask null");
        else logger.debug("cancel - runningTask.isDone(): " + runningTask.isDone());
        if(runningTask != null && !runningTask.isDone()) {
            logger.debug(" --- cancelling task ---");
            runningTask.cancel(true);
            showProgressPanel(false);
            return;
        } else sendResponse(ResponseVS.SC_CANCELLED, ContextVS.getMessage("operationCancelledMsg"));
        dispose();
    }

    class SignedSenderWorker extends SwingWorker<ResponseVS, Object> {

        private String password = null;

        public SignedSenderWorker(String password) {
            this.password = password;
        }

        @Override public ResponseVS doInBackground() throws Exception {
            logger.debug("SignedSenderWorker.doInBackground");
            JSONObject documentToSignJSON = (JSONObject)JSONSerializer.toJSON(operation.getDocumentToSignMap());
            SMIMEMessageWrapper smimeMessage = DNIeContentSigner.genMimeMessage(null,
                    operation.getNormalizedReceiverName(), documentToSignJSON.toString(),
                    password.toCharArray(), operation.getSignedMessageSubject(), null);
            SMIMESignedSender senderWorker = new SMIMESignedSender(smimeMessage, operation.getUrlEnvioDocumento(),
                    ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED, null, ContextVS.getInstance().getAccessControl().
                    getX509Certificate(), header);
            return senderWorker.call();
        }


        @Override protected void done() {
            showProgressPanel(false);
            try {
                ResponseVS responseVS = get();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    logger.debug("SignedSenderWorker.done - statusCode:" + responseVS.getStatusCode());
                    if(responseVS.getContentType() != null && responseVS.getContentType().isSigned()) {
                        try {
                            SMIMEMessageWrapper smimeMessageResp = responseVS.getSmimeMessage();
                            if(smimeMessageResp == null) {
                                ByteArrayInputStream bais = new ByteArrayInputStream(responseVS.getMessageBytes());
                                smimeMessageResp = new SMIMEMessageWrapper(bais);
                            }
                            JSONObject jsonObject = (JSONObject)JSONSerializer.toJSON(smimeMessageResp.getSignedContent());
                            OperationVS result = OperationVS.populate(jsonObject);
                            String msg = result.getMessage();
                            if(TypeVS.VOTING_PUBLISHING == operation.getType() ||
                                    TypeVS.CLAIM_PUBLISHING == operation.getType()) {
                                String eventURL = ((java.util.List<String>)responseVS.getData()).iterator().next();
                                result.setUrlDocumento(eventURL);
                                msg = eventURL;
                            }
                            sendResponse(responseVS.getStatusCode(), msg);
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                            sendResponse(ResponseVS.SC_ERROR, ex.getMessage());
                        }
                    } else sendResponse(responseVS.getStatusCode(), responseVS.getMessage());
                } else {
                    sendResponse(responseVS.getStatusCode(), responseVS.getMessage());
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                String mensajeError = ContextVS.getInstance().getMessage("signDocumentErrorMsg");
                if(ex.getCause() instanceof VotingSystemException) {
                    mensajeError = ex.getCause().getMessage();
                }
                sendResponse(ResponseVS.SC_ERROR, mensajeError);
            }
            dispose();
        }
    }

    public static void main(String args[]) {  }

}