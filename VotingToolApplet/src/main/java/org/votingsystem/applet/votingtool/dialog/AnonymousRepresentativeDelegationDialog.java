package org.votingsystem.applet.votingtool.dialog;

import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.applet.votingtool.panel.ProgressBarPanel;
import org.votingsystem.callable.AccessRequestDataSender;
import org.votingsystem.callable.AnonymousDelegationRequestDataSender;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.model.*;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.StringUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AnonymousRepresentativeDelegationDialog extends JDialog {

    private static Logger logger = Logger.getLogger(AnonymousRepresentativeDelegationDialog.class);

    private Container container;
    private ProgressBarPanel progressBarPanel;
    private JPanel formPanel;
    private JLabel messageLabel;
    private JButton cancelButton;
    private JButton signAndSendButton;
    private JButton openDocumentButton;
    private OperationVS operation;
    private Future<ResponseVS> runningTask;
    private String originHashCertVS;
    private String hashCertVSBase64;
    private SMIMEMessageWrapper smimeMessage;
    private Map documentToSignMap;
    private final AtomicBoolean done = new AtomicBoolean(false);

    public AnonymousRepresentativeDelegationDialog(Frame parent, boolean modal) {
        super(parent, modal);
        //parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug("AnonymousRepresentativeDelegationDialog window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug("AnonymousRepresentativeDelegationDialog window closing event received");
                sendResponse(ResponseVS.SC_CANCELLED, ContextVS.getMessage("operationCancelled"));
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    public void show(OperationVS operation) {
        this.operation = operation;
        String representativeName = (String) operation.getDocumentToSignMap().get("representativeName");
        //Bug similar to -> http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6993691
        ParserDelegator workaround = new ParserDelegator();
        messageLabel.setText(ContextVS.getMessage("anonymousRepresentativeMsg", representativeName));
        String caption = operation.getCaption();
        if(caption.length() > 50) caption = caption.substring(0, 50) + "...";
        documentToSignMap = new HashMap();
        documentToSignMap.put("weeksOperationActive", operation.getDocumentToSignMap().get("weeksOperationActive"));
        documentToSignMap.put("UUID", UUID.randomUUID().toString());
        documentToSignMap.put("accessControlURL", ContextVS.getInstance().getAccessControl().getServerURL());
        documentToSignMap.put("operation", TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
        pack();
        setTitle(caption);
        setVisible(true);
    }

    private void sendResponse(int status, String message) {
        done.set(true);
        operation.setStatusCode(status);
        operation.setMessage(message);
        ContextVS.getInstance().sendMessageToHost(operation);
        dispose();
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


    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill", "", "[][]20[]"));
        progressBarPanel = new ProgressBarPanel();

        Border formPanelBorder = BorderFactory.createLineBorder(Color.GRAY, 1);
        formPanel = new JPanel();
        formPanel.setBorder(formPanelBorder);
        formPanel.setLayout(new MigLayout("fill", "15[grow]15","10[][]10"));

        messageLabel = new JLabel(ContextVS.getMessage("signatureDialogFormMsg"), SwingConstants.CENTER);
        formPanel.add(messageLabel, "growx, height 50::, wrap");

        openDocumentButton = new JButton(ContextVS.getMessage("openDocumentToSignLbl"));
        openDocumentButton.setIcon(ContextVS.getIcon(this, "application-certificate"));
        openDocumentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openDocument();}
        });


        JButton infoButton = new JButton(ContextVS.getMessage("anonymousDelegationInfoButtonLbl"));
        infoButton.setIcon(ContextVS.getIcon(this, "information"));
        infoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { openInfoDocument();}
        });

        formPanel.add(openDocumentButton, "wrap");
        container.add(formPanel, "cell 0 1, growx, wrap");

        signAndSendButton = new JButton(ContextVS.getMessage("signAndSendLbl"));
        signAndSendButton.setIcon(ContextVS.getIcon(this, "application-certificate_16"));
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

    private void signAndSend() {
        logger.debug("signAndSend");
        String password = null;
        PasswordDialog dialogoPassword = new PasswordDialog (new JFrame(), true);
        dialogoPassword.setVisible(true);
        password = dialogoPassword.getPassword();
        if (password == null) return;
        showProgressPanel(true, ContextVS.getMessage("progressLabel"));
        DelegationRequestWorker requestWorker = new DelegationRequestWorker(password);
        runningTask = requestWorker;
        requestWorker.execute();
        pack();
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

            JSONObject documentToSignJSON = (JSONObject) JSONSerializer.toJSON(documentToSignMap);
            FileUtils.copyStreamToFile(new ByteArrayInputStream(documentToSignJSON.toString().getBytes()),tempFile);
            desktop.open(tempFile);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void openInfoDocument() {
        logger.debug("openDocument");
    }

    class DelegationRequestWorker extends SwingWorker<ResponseVS, Object> {

        private String password = null;
        
        private DelegationRequestWorker(String password) {
            this.password = password;
        }
       
        @Override public ResponseVS doInBackground() {
            logger.debug("DelegationRequestWorker.doInBackground");
            try {
                String fromUser = ContextVS.getInstance().getMessage("electorLbl");
                String toUser =  ContextVS.getInstance().getAccessControl().getNameNormalized();
                JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(documentToSignMap);
                smimeMessage = DNIeContentSigner.genMimeMessage(fromUser, toUser, jsonObject.toString(),
                        password.toCharArray(), operation.getSignedMessageSubject(), null);
                originHashCertVS = UUID.randomUUID().toString();
                hashCertVSBase64 = CMSUtils.getHashBase64(originHashCertVS, ContextVS.VOTING_DATA_DIGEST);
                String weeksOperationActive = (String)operation.getDocumentToSignMap().get("weeksOperationActive");
                AnonymousDelegationRequestDataSender accessRequestDataSender = new AnonymousDelegationRequestDataSender(
                        smimeMessage, weeksOperationActive, hashCertVSBase64);
                return accessRequestDataSender.call();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

       @Override protected void done() {
           showProgressPanel(false);
            try {
                ResponseVS responseVS = get();
                logger.debug("DelegationRequestWorker.done - statusCode: " +  responseVS.getStatusCode());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    showProgressPanel(true, ContextVS.getMessage("sendingDataToAccessControlMsg"));
                    AnonymousDelegationWorker delegationWorker = new AnonymousDelegationWorker(
                            (CertificationRequestVS) responseVS.getData());
                    runningTask = delegationWorker;
                    delegationWorker.execute();
                } else sendResponse(responseVS.getStatusCode(), responseVS.getMessage());
            } catch(Exception ex) {
                sendResponse(ResponseVS.SC_ERROR, ex.getMessage());
            }
       }
   }
    
    
    class AnonymousDelegationWorker extends SwingWorker<ResponseVS, Object> {

        CertificationRequestVS certificationRequest;
        
        private AnonymousDelegationWorker(CertificationRequestVS certificationRequest) {
            this.certificationRequest = certificationRequest;
        }
       
        @Override public ResponseVS doInBackground() {
            logger.debug("AnonymousDelegationWorker.doInBackground");
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(operation.getDocumentToSignMap());
            String textToSign = jsonObject.toString();
            try {
                String fromUser = hashCertVSBase64;
                String toUser = StringUtils.getNormalized(ContextVS.getInstance().getAccessControl().getName());
                smimeMessage = certificationRequest.genMimeMessage(fromUser, toUser, textToSign,
                        operation.getSignedMessageSubject(), null);
                String anonymousDelegationService = ContextVS.getInstance().getAccessControl().
                        getAnonymousDelegationServiceURL();
                SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage, anonymousDelegationService,
                        ContentTypeVS.SIGNED_AND_ENCRYPTED, certificationRequest.getKeyPair(),
                        ContextVS.getInstance().getAccessControl().getX509Certificate());
                return signedSender.call();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

       @Override protected void done() {
           showProgressPanel(false);
            try {
                ResponseVS responseVS = get();
                logger.debug("AnonymousDelegationWorker.done - StatusCode: " + responseVS.getStatusCode());
                String msg = responseVS.getMessage();
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {  
                    SMIMEMessageWrapper receipt = responseVS.getSmimeMessage();
                    Map receiptDataMap = (JSONObject) JSONSerializer.toJSON(receipt.getSignedContent());
                    responseVS = operation.validateReceiptDataMap(receiptDataMap);
                    Map delegationDataMap = new HashMap();
                    delegationDataMap.put(ContextVS.HASH_CERTVS_KEY, hashCertVSBase64);
                    delegationDataMap.put(ContextVS.ORIGIN_HASH_CERTVS_KEY, originHashCertVS);
                    ResponseVS hashCertVSData = new ResponseVS(ResponseVS.SC_OK);
                    hashCertVSData.setSmimeMessage(receipt);
                    hashCertVSData.setData(delegationDataMap);
                    hashCertVSData.setType(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
                    ContextVS.getInstance().addHashCertVSData(hashCertVSBase64, hashCertVSData);
                }
                sendResponse(responseVS.getStatusCode(), msg);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                sendResponse(ResponseVS.SC_ERROR, ex.getMessage());
            }
       }
   }
}