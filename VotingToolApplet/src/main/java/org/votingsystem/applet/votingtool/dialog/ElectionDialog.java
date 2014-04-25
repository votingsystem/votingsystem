package org.votingsystem.applet.votingtool.dialog;

import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.callable.AccessRequestDataSender;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.applet.votingtool.panel.ProgressBarPanel;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
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
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ElectionDialog extends JDialog {

    private static Logger logger = Logger.getLogger(ElectionDialog.class);

    private Container container;
    private ProgressBarPanel progressBarPanel;
    private JPanel formPanel;
    private JLabel messageLabel;
    private JButton cancelButton;
    private JButton signAndSendButton;
    private JButton openDocumentButton;
    private OperationVS operation;
    private Future<ResponseVS> runningTask;
    private EventVS eventVS;
    private SMIMEMessageWrapper smimeMessage;
    private final AtomicBoolean done = new AtomicBoolean(false);

    public ElectionDialog(Frame parent, boolean modal) {
        super(parent, modal);
        //parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug("VotacionDialog window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug("VotacionDialog window closing event received");
                sendResponse(ResponseVS.SC_CANCELLED, ContextVS.getInstance().getMessage("operationCancelled"));
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    public void show(OperationVS operation) {
        this.operation = operation;
        eventVS = operation.getEventVS();
        try {
            eventVS.getVoteVS().genVote();
        } catch (NoSuchAlgorithmException ex) {
            logger.error(ex.getMessage(), ex);
        }
        //Bug similar to -> http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6993691
        ParserDelegator workaround = new ParserDelegator();
        messageLabel.setText(ContextVS.getInstance().getMessage("electionDialogMsg", eventVS.getSubject(),
                eventVS.getVoteVS().getOptionSelected().getContent()));
        String caption = eventVS.getSubject();
        if(caption.length() > 50) caption = caption.substring(0, 50) + "...";
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
            sendResponse(ResponseVS.SC_CANCELLED, ContextVS.getMessage("operationCancelled"));
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
        AccesRequestWorker accesRequestWorker = new AccesRequestWorker(password);
        runningTask = accesRequestWorker;
        accesRequestWorker.execute();
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
            JSONObject documentToSignJSON = (JSONObject) JSONSerializer.toJSON(eventVS.getVoteVS().getAccessRequestDataMap());
            FileUtils.copyStreamToFile(new ByteArrayInputStream(documentToSignJSON.toString().getBytes()),tempFile);
            desktop.open(tempFile);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

 
    class AccesRequestWorker extends SwingWorker<ResponseVS, Object> {

        private String password = null;
        
        private AccesRequestWorker(String password) {
            this.password = password;
        }
       
        @Override public ResponseVS doInBackground() {
            logger.debug("AccesRequestWorker.doInBackground");
            try {
                String fromUser = ContextVS.getInstance().getMessage("electorLbl");
                String toUser =  eventVS.getAccessControlVS().getNameNormalized();
                String msgSubject = ContextVS.getInstance().getMessage("accessRequestMsgSubject")  + eventVS.getId();
                JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(eventVS.getVoteVS().getAccessRequestDataMap());
                smimeMessage = DNIeContentSigner.genMimeMessage(fromUser, toUser, jsonObject.toString(),
                        password.toCharArray(), msgSubject, null);
                //No se hace la comprobación antes porque no hay usuario en contexto
                //hasta que no se firma al menos una vez
                eventVS.setUserVS(ContextVS.getInstance().getSessionUser());
                AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(
                        smimeMessage, eventVS.getVoteVS());
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
                logger.debug("AccesRequestWorker.done - statusCode: " +  responseVS.getStatusCode());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    showProgressPanel(true, ContextVS.getInstance().getMessage("sendingDataToControlCenterMsg"));
                    VoteSenderWorker voteSenderWorker = new VoteSenderWorker(
                            (CertificationRequestVS) responseVS.getData(), eventVS);
                    runningTask = voteSenderWorker;
                    voteSenderWorker.execute();
                } else {
                    sendResponse(responseVS.getStatusCode(), responseVS.getMessage());
                }
            } catch(Exception ex) {
                sendResponse(ResponseVS.SC_ERROR, ex.getMessage());
            }
       }
   }
    
    
    class VoteSenderWorker extends SwingWorker<ResponseVS, Object> {

        CertificationRequestVS certificationRequest;
        EventVS eventVS;
        
        private VoteSenderWorker(CertificationRequestVS certificationRequest, EventVS eventVS) {
            this.certificationRequest = certificationRequest;
            this.eventVS = eventVS;
        }
       
        @Override public ResponseVS doInBackground() {
            logger.debug("VoteSenderWorker.doInBackground");
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(eventVS.getVoteVS().getVoteDataMap());
            String textToSign = jsonObject.toString();
            try {
                String fromUser = eventVS.getVoteVS().getHashCertVSBase64();
                String toUser = StringUtils.getNormalized(eventVS.getControlCenterVS().getName());
                String msgSubject = ContextVS.getInstance().getMessage("voteVSSubject");
                smimeMessage = certificationRequest.genMimeMessage(fromUser, toUser, textToSign, msgSubject, null);
                String urlVoteService = ((ControlCenterVS)ContextVS.getInstance().getControlCenter()).getVoteServiceURL();
                SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage, urlVoteService,
                        ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                        ContentTypeVS.VOTE, certificationRequest.getKeyPair(), ContextVS.getInstance().getControlCenter().
                        getX509Certificate(), "voteURL");
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
                logger.debug("VoteSenderWorker.done - StatusCode: " + responseVS.getStatusCode());
                String msg = responseVS.getMessage();
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {  
                    SMIMEMessageWrapper validatedVote = responseVS.getSmimeMessage();
                    Map validatedVoteDataMap = (JSONObject) JSONSerializer.toJSON(validatedVote.getSignedContent());
                    eventVS.getVoteVS().setReceipt(validatedVote);
                    ResponseVS voteResponse = new ResponseVS(ResponseVS.SC_OK);
                    voteResponse.setType(TypeVS.VOTEVS);
                    voteResponse.setData(eventVS.getVoteVS());
                    ContextVS.getInstance().addHashCertVSData(eventVS.getVoteVS().getHashCertVSBase64(), voteResponse);
                    //voteURL header
                    msg = ((List<String>)responseVS.getData()).iterator().next();
                }
                sendResponse(responseVS.getStatusCode(), msg);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                sendResponse(ResponseVS.SC_ERROR, ex.getMessage());
            }
       }
   }
}