package org.votingsystem.applet.validationtool.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.backup.*;
import org.votingsystem.applet.validationtool.model.MetaInf;
import org.votingsystem.applet.validationtool.panel.MessagePanel;
import org.votingsystem.applet.validationtool.panel.ProgressBarPanel;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.StatusVS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.Future;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class BackupValidationDialog extends JDialog implements ValidatorListener<ValidationEvent> {

    public enum Status implements StatusVS<Status> {INIT_VALIDATION, FINISH_VALIDATION}

    private static Logger logger = Logger.getLogger(BackupValidationDialog.class);

    private java.util.List<String> errorList;
    private MetaInf metaInf;
    private int filesProcessed = 0;
    private ValidatorListener validatorListener = null;
    private Container container;
    private ProgressBarPanel progressBarPanel;
    private MessagePanel messagePanel;
    private Future<ResponseVS> runningTask;
    private JButton errorsButton;
    private JButton cancelButton;

    public BackupValidationDialog(Frame parent, MetaInf metaInf, boolean modal) {
        super(parent, modal);
        this.metaInf = metaInf;
        initComponents();
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { logger.debug(" - window closed event received"); }

            public void windowClosing(WindowEvent e) { logger.debug(" - window closing event received"); }
        });
        pack();
        setLocationRelativeTo(null);
        validatorListener = this;
    }

    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill, insets 10 10 10 10", "", "[center][]20[]"));

        messagePanel = new MessagePanel();
        container.add(messagePanel, "cell 0 0, span 2, height 50::, grow, width 400:400:400, wrap");

        progressBarPanel = new ProgressBarPanel();
        container.add(progressBarPanel, "cell 0 1, span 2, width 400::, growx, wrap");

        errorsButton = new JButton(ContextVS.getMessage("errorsLbl"));
        errorsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showErrors();
            }
        });
        errorsButton.setVisible(false);
        container.add(errorsButton);

        cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel_16"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel();
            }
        });
        container.add(cancelButton, "width :150:, align right");

        long filesToProcess = 0;
        switch(metaInf.getType()) {
            case MANIFEST_EVENT:
                setTitle(ContextVS.getInstance().getMessage("validateManifestBackupCaption"));
                filesToProcess = metaInf.getNumSignatures();
                break;
            case VOTING_EVENT:
                setTitle(ContextVS.getInstance().getMessage("validateVotingBackupCaption"));
                filesToProcess = metaInf.getNumAccessRequest() + metaInf.getNumVotes() +
                        metaInf.getRepresentativesData().getNumRepresented() +
                        metaInf.getRepresentativesData().getNumRepresentativesWithAccessRequest();
                break;
            case CLAIM_EVENT:
                setTitle(ContextVS.getInstance().getMessage("validateClaimBackupCaption"));
                filesToProcess = metaInf.getNumSignatures();
                break;
        }
        progressBarPanel.setMaximum(new Long(filesToProcess).intValue());
    }

    private void cancel() {
        logger.debug("cancel");
        if(runningTask != null) runningTask.cancel(true);
        dispose();
    }

    public void initValidation(String decompressedBackupBaseDir) throws Exception {
        BackupWorker backupWorker = new BackupWorker(decompressedBackupBaseDir);
        runningTask = backupWorker;
        backupWorker.execute();
        setVisible(true);
    }

    @Override public void processValidationEvent(ResponseVS<ValidationEvent> validationEventResponse) {
        switch(metaInf.getType()) {
            case MANIFEST_EVENT:
                processManifestValidationEvent(validationEventResponse);
                break;
            case VOTING_EVENT:
                processVotingValidationEvent(validationEventResponse);
                break;
            case CLAIM_EVENT:
                processClaimValidationEvent(validationEventResponse);
                break;
        }
    }

    public void processVotingValidationEvent(ResponseVS<ValidationEvent> responseVS) {
        if(!responseVS.getErrorList().isEmpty()) {
            errorsButton.setVisible(true);
            String msg =  (errorList.size() > 1)? ContextVS.getInstance().getMessage("errorsLbl"):
                    ContextVS.getInstance().getMessage("errorLbl");
            errorsButton.setText(errorList.size() + " " + msg);
        }
        progressBarPanel.setValue(filesProcessed++);
        switch(responseVS.getData()) {
            case REPRESENTATIVE:
                messagePanel.setMessage(ContextVS.getMessage("validatingRepresentativeDataMsg"), null);
                break;
            case REPRESENTATIVE_FINISH:
                break;
            case ACCESS_REQUEST:
                messagePanel.setMessage(ContextVS.getMessage("validatingAccessRequestsMsg"), null);
                break;
            case ACCESS_REQUEST_FINISH:
                break;
            case VOTE:
                messagePanel.setMessage(ContextVS.getMessage("validatingVotesMsg"), null);
                break;
            case VOTE_FINISH:
                String message = null;
                Icon icon = null;
                if(errorList == null || errorList.isEmpty()) {
                    icon = ContextVS.getIcon(this, "accept_32");
                    message = ContextVS.getMessage("validationWithoutErrorsMsg",responseVS.getMessage());
                } else {
                    icon = ContextVS.getIcon(this, "cancel_32");
                    message = ContextVS.getMessage("validationWithErrorsMsg", responseVS.getMessage());
                }
                cancelButton.setText(ContextVS.getInstance().getMessage("closeLbl"));

                messagePanel.setMessage(message, icon);
                container.remove(progressBarPanel);
                break;
        }
        pack();
    }

    public void processManifestValidationEvent( ResponseVS<ValidationEvent> responseVS) {
        if(!responseVS.getErrorList().isEmpty()) {
            errorsButton.setVisible(true);
            String msg =  (errorList.size() > 1)? ContextVS.getInstance().getMessage("errorsLbl"):
                    ContextVS.getInstance().getMessage("errorLbl");
            errorsButton.setText(errorList.size() + " " + msg);
        }
        progressBarPanel.setValue(filesProcessed++);
        switch(responseVS.getData()) {
            case MANIFEST:
                messagePanel.setMessage(ContextVS.getInstance().getMessage("validatingManifestsMsg"), null);
                break;
            case MANIFEST_FINISIH:
                String message = null;
                Icon icon = null;
                if(errorList == null || errorList.isEmpty()) {
                    icon = ContextVS.getIcon(this, "accept_32");
                    message = ContextVS.getMessage("validationWithoutErrorsMsg",responseVS.getMessage());
                } else {
                    icon = ContextVS.getIcon(this, "cancel_32");
                    message = ContextVS.getMessage("validationWithErrorsMsg", responseVS.getMessage());
                }
                cancelButton.setText(ContextVS.getMessage("closeLbl"));
                messagePanel.setMessage(message, icon);
                container.remove(progressBarPanel);
                pack();
                break;
        }
    }

    public void processClaimValidationEvent( ResponseVS<ValidationEvent> responseVS) {
        if(!responseVS.getErrorList().isEmpty()) {
            errorsButton.setVisible(true);
            String msg =  (errorList.size() > 1)? ContextVS.getInstance().getMessage("errorsLbl"):
                    ContextVS.getInstance().getMessage("errorLbl");
            errorsButton.setText(errorList.size() + " " + msg);
        }
        progressBarPanel.setValue(filesProcessed++);
        switch(responseVS.getData()) {
            case CLAIM:
                messagePanel.setMessage(ContextVS.getInstance().getMessage("validatingClaimsDataMsg"), null);
                break;
            case CLAIM_FINISH:
                String message = null;
                Icon icon = null;
                if(errorList == null || errorList.isEmpty()) {
                    icon = ContextVS.getIcon(this, "accept_32");
                    message = ContextVS.getMessage("validationWithoutErrorsMsg",responseVS.getMessage());
                } else {
                    icon = ContextVS.getIcon(this, "cancel_32");
                    message = ContextVS.getMessage("validationWithErrorsMsg", responseVS.getMessage());
                }
                cancelButton.setText(ContextVS.getMessage("closeLbl"));
                messagePanel.setMessage(message, icon);
                container.remove(progressBarPanel);
                pack();
                break;
        }
    }

    private void showErrors() {
        String resultMessange = "";
        int numError = 0;
        for(String error: errorList) {
            resultMessange = resultMessange + "<br/> *** " + ++numError + " - " + error;
        }
        MessageDialog messageDialog = new MessageDialog(new JFrame(), false);
        messageDialog.showMessage("<html>" + resultMessange + "</html>",
                ContextVS.getInstance().getMessage("votingBackupErrorCaption"));
    }

    class BackupWorker extends SwingWorker<ResponseVS, Object> {

        private String decompressedBackupBaseDir = null;

        public BackupWorker(String decompressedBackupBaseDir) {
            this.decompressedBackupBaseDir = decompressedBackupBaseDir;
        }

        @Override public ResponseVS doInBackground() {
            logger.debug("worker.doInBackground: ");
            try {
                switch(metaInf.getType()) {
                    case VOTING_EVENT:
                        VotingBackupValidator votingBackupValidator = new VotingBackupValidator(
                                decompressedBackupBaseDir, validatorListener);
                        return votingBackupValidator.call();
                    case MANIFEST_EVENT:
                        ManifestBackupValidator manifestBackupValidator = new ManifestBackupValidator(
                                decompressedBackupBaseDir, validatorListener);
                        return manifestBackupValidator.call();
                    case CLAIM_EVENT:
                        ClaimBackupValidator claimBackupValidator = new ClaimBackupValidator(
                                decompressedBackupBaseDir, validatorListener);
                        return claimBackupValidator.call();
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            return new ResponseVS(ResponseVS.SC_ERROR, "Unknown event type: " + metaInf.getType());
        }

        @Override protected void done() {
            try {
                ResponseVS response = get();
                logger.debug("worker.done status:" + response.getStatusCode() + " - message: " + response.getMessage());
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ContextVS.init(null, "log4jValidationTool.properties", "validationToolMessages_", "es");
                    String zipFile = "./representative_00000001R.zip";
                    String outputFolder = ContextVS.APPTEMPDIR +  File.separator + UUID.randomUUID();
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    //BackupValidationDialog dialog = new BackupValidationDialog(new JFrame(), true);
                    //dialog.setVisible(true);
                    //dialog.unZipBackup(zipFile, outputFolder);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }

}