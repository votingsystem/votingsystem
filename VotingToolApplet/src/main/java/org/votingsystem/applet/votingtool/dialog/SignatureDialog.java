package org.votingsystem.applet.votingtool.dialog;

import com.itextpdf.text.pdf.PdfReader;
import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Future;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.Border;
import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.callable.PDFSignedSender;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.util.PdfFormHelper;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.applet.votingtool.panel.ProgressBarPanel;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.VotingSystemException;
import org.votingsystem.util.FileUtils;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignatureDialog extends JDialog {
    
    private static Logger logger = Logger.getLogger(SignatureDialog.class);
    
    private byte[] pdfDocumentBytes;
    private String eventId;
    
    private Container container;
    private ProgressBarPanel progressBarPanel;
    private JPanel formPanel;
    private JButton cancelButton;
    private JButton signAndSendButton;
    private JButton openDocumentButton;
    private OperationVS operation;
    private Future runningTask;

        
    public SignatureDialog(Frame parent, boolean modal) {
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
    
    private void initComponents() {
        container = getContentPane();   
        container.setLayout(new MigLayout("fill", "", "[][]20[]"));
        progressBarPanel = new ProgressBarPanel();

        Border formPanelBorder = BorderFactory.createLineBorder(Color.GRAY, 1);
        formPanel = new JPanel();
        formPanel.setBorder(formPanelBorder);
        formPanel.setLayout(new MigLayout("fill", "15[grow]15","10[][]10"));

        JLabel formMessageLabel = new JLabel(ContextVS.getMessage("signatureDialogFormMsg"), SwingConstants.CENTER);
        formPanel.add(formMessageLabel, "growx, wrap"); 
        
        openDocumentButton = new JButton(ContextVS.getMessage("openDocumentToSignLbl"));
        openDocumentButton.setIcon(ContextVS.getIcon(this, "open_folder_16"));
        openDocumentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openDocument();}
        });
        formPanel.add(openDocumentButton, "wrap");
        container.add(formPanel, "cell 0 1, growx, wrap");
        
        signAndSendButton = new JButton(ContextVS.getMessage("signAndSendLbl"));
        signAndSendButton.setIcon(ContextVS.getIcon(this, "document_signature_16"));
        signAndSendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { signAndSend();}
        });
        container.add(signAndSendButton, "width :150:, cell 0 2, split2, align right");
        
        cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel_16"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { cancel();}
        });
        container.add(cancelButton, "width :150:, align right");
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
        if(runningTask == null )  logger.debug("cancel - runningTask null");
        else logger.debug("cancel - runningTask.isDone(): " + runningTask.isDone());
        
        if(runningTask != null && !runningTask.isDone()) {
            logger.debug(" --- cancelling task ---"); 
            runningTask.cancel(true);
            showProgressPanel(false);
            return;
        } else {
            sendResponse(ResponseVS.SC_CANCELLED, ContextVS.getMessage("operationCancelledMsg"));
        }
        dispose();
    }
    
    private void openDocument() {
        logger.debug("openDocument");
        if (!Desktop.isDesktopSupported()) {
            logger.debug("Desktop Not Supported");
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            logger.debug("Desktop doesn't support file browsing");
        }
        try {
            File tempFile = File.createTempFile(operation.getFileName(), "");
            tempFile.deleteOnExit();
            if(pdfDocumentBytes == null) {
                JSONObject documentToSignJSON = (JSONObject)JSONSerializer.toJSON(operation.getDocumentToSignMap());
                FileUtils.copyStreamToFile(new ByteArrayInputStream(documentToSignJSON.toString().getBytes()),tempFile);
            }
            else FileUtils.copyStreamToFile(new ByteArrayInputStream(pdfDocumentBytes), tempFile);
            desktop.open(tempFile);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
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
    
    public void show(OperationVS operation) {
        logger.debug("show - operationType: " + operation.getType());
        this.operation = operation;
        setTitle(operation.getCaption());
        switch(operation.getType()) {
            case MANIFEST_SIGN:
            case MANIFEST_PUBLISHING:
                openDocumentButton.setIcon(ContextVS.getIcon(this, "file_extension_pdf"));
                progressBarPanel.setMessage(ContextVS.getMessage("downloadingDocument"));
                showProgressPanel(true);
                PdfGetterWorker pdfGetterWorker = new PdfGetterWorker(operation);
                pdfGetterWorker.execute();
                break;
            case BACKUP_REQUEST:
                openDocumentButton.setIcon(ContextVS.getIcon(this, "file_extension_pdf"));
                try {
                    pdfDocumentBytes = PdfFormHelper.getBackupRequest(operation.getEventVS().getId().toString(),
                        operation.getEventVS().getSubject(), operation.getEmail());
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
                break;
            case REPRESENTATIVE_SELECTION:
                if(operation.getDocumentToSignMap().get("representativeName") != null) {
                    progressBarPanel.setMessage(ContextVS.getMessage("selectedRepresentativeMsg",                            
                            (String) operation.getDocumentToSignMap().get("representativeName")));
                    progressBarPanel.setVisible(true);
                }
                break;
            default: break;
        }
        pack();
        setVisible(true);
    }
    
        
    class PdfGetterWorker extends SwingWorker<ResponseVS, Object> {

        OperationVS operation = null;

        public PdfGetterWorker(OperationVS operation) {
            this.operation = operation;
        }

        @Override public ResponseVS doInBackground() {
            logger.debug("PdfGetterWorker.doInBackground");
            ResponseVS responseVS = null;
            try {
                switch(operation.getType()) {
                    case MANIFEST_SIGN:
                        responseVS = HttpHelper.getInstance().getData(operation.getUrlDocumento(), ContentTypeVS.PDF.getName());
                        return responseVS;
                    case MANIFEST_PUBLISHING:
                        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(operation.getDocumentToSignMap());
                        responseVS = HttpHelper.getInstance().sendData(jsonObject.toString().getBytes(),
                                null, operation.getUrlEnvioDocumento(), "eventId");
                        return responseVS;
                }  
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
            return responseVS;
        }

       @Override protected void done() {
           logger.debug("PdfGetterWorker.done");
            try {
                ResponseVS responseVS = get();
                switch(operation.getType()) {
                    case MANIFEST_SIGN:
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) pdfDocumentBytes = responseVS.getMessageBytes();
                        else sendResponse(responseVS.getStatusCode(), ContextVS.getInstance().getMessage(
                                    "errorDownloadingDocument") + " - " + responseVS.getMessage());
                        break;
                    case MANIFEST_PUBLISHING:
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            pdfDocumentBytes = responseVS.getMessageBytes();
                            eventId = ((List<String>)responseVS.getData()).iterator().next();
                            String receiverSignServiceURL = operation.getUrlEnvioDocumento() +  "/" + eventId;
                            operation.setUrlEnvioDocumento(receiverSignServiceURL);
                        } else {
                            sendResponse(responseVS.getStatusCode(), ContextVS.getInstance().getMessage(
                                    "errorDownloadingDocument") + " - " + responseVS.getMessage());
                        }
                        break;
                }   
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                sendResponse(ResponseVS.SC_ERROR, ex.getMessage());
            }
            showProgressPanel(false);
       }
   }
    
    public static void main(String[] args) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ContextVS.initSignatureApplet(null, "log4j.properties", "messages_", "es");
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    SignatureDialog dialog = new SignatureDialog(new JFrame(), true);
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
    
    class SignedSenderWorker extends SwingWorker<ResponseVS, Object> {
        
        private String password = null;
        
        public SignedSenderWorker(String password) {
            this.password = password;
        }
        
        @Override public ResponseVS doInBackground() throws Exception {
            logger.debug("SignedSenderWorker.doInBackground - operation: "  + operation.getType().toString());
            switch(operation.getType()) {
                case CLAIM_PUBLISHING:
                case VOTING_PUBLISHING:
                    return runSMIMEOperation("eventURL");
                case BACKUP_REQUEST:
                case MANIFEST_SIGN:
                case MANIFEST_PUBLISHING:
                    PdfReader readerManifesto = new PdfReader(pdfDocumentBytes);
                    String reason = null;
                    String location = null;
                    PDFSignedSender pdfSignedSender = new PDFSignedSender(operation.getUrlEnvioDocumento(),
                            reason, location, password.toCharArray(), readerManifesto, null, null,
                            ContextVS.getInstance().getAccessControl().getX509Certificate());
                    return pdfSignedSender.call();
                default:
                    logger.debug("Operation without headers");
                    return runSMIMEOperation(null);
            }
       }

        private ResponseVS runSMIMEOperation(String header) throws Exception {
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
                    if(operation.isRespuestaConRecibo()) {
                        try {
                            logger.debug("SignedSenderWorker.done - isRespuestaConRecibo");
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
                                String eventURL = ((List<String>)responseVS.getData()).iterator().next();
                                result.setUrlDocumento(eventURL);
                                msg = eventURL;
                            }
                            sendResponse(responseVS.getStatusCode(), msg);
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                            sendResponse(ResponseVS.SC_ERROR, ex.getMessage());
                        }
                    } else sendResponse(responseVS.getStatusCode(), 
                            responseVS.getMessage());   
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
 
}
