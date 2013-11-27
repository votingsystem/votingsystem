package org.votingsystem.applet.validationtool.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.backup.*;
import org.votingsystem.applet.validationtool.model.MetaInf;
import org.votingsystem.applet.validationtool.panel.MessagePanel;
import org.votingsystem.applet.validationtool.panel.ProgressBarPanel;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class BackupValidationDialog extends JDialog implements ValidatorListener<ValidationEvent> {

    private static Logger logger = Logger.getLogger(BackupValidationDialog.class);


    private static ExecutorService executor;
    private static CompletionService<ResponseVS> completionService;

    private java.util.List<String> errorList;
    private MetaInf metaInf;
    private int filesProcessed = 0;
    
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
            public void windowClosed(WindowEvent e) {
                logger.debug(" - window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
            }
        });
        setLocationRelativeTo(null);
        pack();
    }

    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill", "", "[][]20[]"));

        messagePanel = new MessagePanel();
        container.add(messagePanel, "cell 0 0, growx, wrap");

        progressBarPanel = new ProgressBarPanel();
        container.add(progressBarPanel, "cell 0 1, width 400::, growx, wrap");

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
        setLocationRelativeTo(null);
    }

    private void cancel() {
        logger.debug("cancel");
        shutdown();
        dispose();
    }

    public void initValidation(String decompressedBackupBaseDir) throws Exception {
        switch(metaInf.getType()) {
            case VOTING_EVENT:
                VotingBackupValidator votingBackupValidator = new VotingBackupValidator(decompressedBackupBaseDir, this);
                runningTask = submit(votingBackupValidator);
                break;
            case MANIFEST_EVENT:
                ManifestBackupValidator manifestBackupValidator = new ManifestBackupValidator(
                        decompressedBackupBaseDir, this);
                runningTask = submit(manifestBackupValidator);
                break;
            case CLAIM_EVENT:
                ClaimBackupValidator claimBackupValidator = new ClaimBackupValidator(decompressedBackupBaseDir, this);
                runningTask = submit(claimBackupValidator);
                break;
        }
        setVisible(true);
    }

    @Override public void processValidationEvent(
            ResponseVS<ValidationEvent> validationEventResponse) {
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

    public void processVotingValidationEvent(
            ResponseVS<ValidationEvent> responseVS) {
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            this.errorList = responseVS.getErrorList();
            if(!errorList.isEmpty()) {
                errorsButton.setVisible(true);
                String msg = null;
                if(errorList.size() > 1) {
                    msg = ContextVS.getInstance().getMessage("errorsLbl");
                } else msg = ContextVS.getInstance().getMessage("errorLbl");
                errorsButton.setText(errorList.size() + " " + msg);
            }
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
                    message = ContextVS.getInstance().getMessage("validationWithoutErrorsMsg",responseVS.getMessage());
                } else {
                    icon = ContextVS.getIcon(this, "cancel_32");
                    message = ContextVS.getInstance().getMessage("validationWithErrorsMsg", responseVS.getMessage());
                }
                cancelButton.setText(ContextVS.getInstance().getMessage("closeLbl"));

                messagePanel.setMessage(message, icon);
                progressBarPanel.setVisible(false);
                pack();
                break;
        }
    }

    public void processManifestValidationEvent( ResponseVS<ValidationEvent> responseVS) { }

    public void processClaimValidationEvent(
            ResponseVS<ValidationEvent> responseVS) {
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            this.errorList = responseVS.getErrorList();
            if(!errorList.isEmpty()) {
                errorsButton.setVisible(true);
                String msg = null;
                if(errorList.size() > 1) {
                    msg = ContextVS.getInstance().getMessage("errorsLbl");
                } else msg = ContextVS.getInstance().getMessage("errorLbl");
                errorsButton.setText(errorList.size() + " " + msg);
            }
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
                    message = ContextVS.getInstance().getMessage("validationWithoutErrorsMsg",responseVS.getMessage());
                } else {
                    icon = ContextVS.getIcon(this, "cancel_32");
                    message = ContextVS.getInstance().getMessage("validationWithErrorsMsg", responseVS.getMessage());
                }
                cancelButton.setText(ContextVS.getInstance().getMessage("closeLbl"));
                messagePanel.setMessage(message, icon);
                progressBarPanel.setVisible(false);
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

    public void shutdown() {
        try {
            logger.debug("------------- shutdown ----------------- ");
            if(executor != null) executor.shutdown();
            //HttpHelper.getInstance().shutdown();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }


    public Future<ResponseVS> submit(Callable<ResponseVS> callable) {
        if(executor == null || executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(10);
            completionService = new ExecutorCompletionService<ResponseVS>(executor);
        }
        return completionService.submit(callable);
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