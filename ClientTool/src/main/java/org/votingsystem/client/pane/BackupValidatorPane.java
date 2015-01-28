package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.backup.ClaimBackupValidator;
import org.votingsystem.client.backup.ValidationEvent;
import org.votingsystem.client.backup.ValidatorListener;
import org.votingsystem.client.backup.VotingBackupValidator;
import org.votingsystem.client.model.MetaInf;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

import java.io.File;

import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BackupValidatorPane extends VBox implements ValidatorListener<ValidationEvent> {

    private static Logger log = Logger.getLogger(BackupValidatorPane.class);

    private java.util.List<String> errorList;
    private MetaInf metaInf;
    private int filesProcessed = 0;
    private int numFilesToProcess = 0;
    private Button errorsButton;
    private ProgressBar progressBar;
    private Text progressMessageText;
    private Button cancelButton;
    private BackupValidationTask runningTask;


    public BackupValidatorPane(String decompressedBackupBaseDir, MetaInf metaInf) {
        this.metaInf = metaInf;
        VBox progressBox = new VBox();
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPrefWidth(330);
        progressBox.setPrefHeight(150);
        progressMessageText = new Text();
        progressMessageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #555;");
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(10);
        HBox buttonHBox = new HBox();
        errorsButton = new Button(ContextVS.getMessage("errorsLbl"));
        errorsButton.setOnAction(actionEvent -> showErrors());
        errorsButton.setVisible(false);
        cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> {
                runningTask.cancel();
                getScene().getWindow().hide();
            });
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        buttonHBox.getChildren().addAll(errorsButton, Utils.getSpacer(), cancelButton);
        setMargin(buttonHBox, new Insets(30, 20, 20, 20));
        progressBox.getChildren().addAll(progressMessageText, progressBar);
        getChildren().addAll(progressBox, buttonHBox);
        runningTask = new BackupValidationTask(decompressedBackupBaseDir);
        File backupDir = new File(decompressedBackupBaseDir);
        numFilesToProcess = backupDir.listFiles().length;
    }

    private void init() {
        new Thread(runningTask).start();
    }

    @Override public void processValidationEvent(ResponseVS<ValidationEvent> validationEventResponse) {
        Platform.runLater(() -> {
            switch(metaInf.getType()) {
                case VOTING_EVENT:
                    processVotingValidationEvent(validationEventResponse);
                    break;
                case CLAIM_EVENT:
                    processClaimValidationEvent(validationEventResponse);
                    break;
            }
        });
    }

    public void processVotingValidationEvent(ResponseVS<ValidationEvent> responseVS) {
        if(!responseVS.getErrorList().isEmpty()) {
            this.errorList = responseVS.getErrorList();
            errorsButton.setVisible(true);
            String msg =  (errorList.size() > 1) ? ContextVS.getInstance().getMessage("errorsLbl"):
                    ContextVS.getInstance().getMessage("errorLbl");
            errorsButton.setText(errorList.size() + " " + msg);
        }
        progressBar.setProgress(filesProcessed++/numFilesToProcess);
        switch(responseVS.getData()) {
            case REPRESENTATIVE:
                progressMessageText.setText(ContextVS.getMessage("validatingRepresentativeDataMsg"));
                break;
            case REPRESENTATIVE_FINISH:
                break;
            case ACCESS_REQUEST:
                progressMessageText.setText(ContextVS.getMessage("validatingAccessRequestsMsg"));
                break;
            case ACCESS_REQUEST_FINISH:
                break;
            case VOTE:
                progressMessageText.setText(ContextVS.getMessage("validatingVotesMsg"));
                break;
            case VOTE_FINISH:
                String message = null;
                if(errorList == null || errorList.isEmpty()) {
                    message = ContextVS.getMessage("validationWithoutErrorsMsg",responseVS.getMessage());
                } else {
                    message = ContextVS.getMessage("validationWithErrorsMsg", responseVS.getMessage());
                }
                cancelButton.setText(ContextVS.getInstance().getMessage("closeLbl"));
                progressMessageText.setText(message);
                progressBar.setVisible(false);
                break;
            default: log.debug("processVotingValidationEvent - unprocessed: " + responseVS.getData().toString());
        }
    }

    public void processClaimValidationEvent(ResponseVS<ValidationEvent> responseVS) {
        if(!responseVS.getErrorList().isEmpty()) {
            errorsButton.setVisible(true);
            String msg =  (errorList.size() > 1)? ContextVS.getInstance().getMessage("errorsLbl"):
                    ContextVS.getInstance().getMessage("errorLbl");
            errorsButton.setText(errorList.size() + " " + msg);
        }
        progressBar.setProgress(filesProcessed++ / numFilesToProcess);
        switch(responseVS.getData()) {
            case CLAIM:
                progressMessageText.setText(ContextVS.getInstance().getMessage("validatingClaimsDataMsg"));
                break;
            case CLAIM_FINISH:
                String message = null;
                if(errorList == null || errorList.isEmpty()) {
                    message = ContextVS.getMessage("validationWithoutErrorsMsg",responseVS.getMessage());
                } else {
                    message = ContextVS.getMessage("validationWithErrorsMsg", responseVS.getMessage());
                }
                cancelButton.setText(ContextVS.getMessage("closeLbl"));
                progressMessageText.setText(message);
                break;
        }
    }

    private void showErrors() {
        StringBuilder sb = new StringBuilder();
        int numError = 0;
        for(String error: errorList) {
            sb.append("<br/><br/><b>" + ++numError + "</b> - " + error);
        }
        HTMLMessageDialog messageDialog = new HTMLMessageDialog();
        messageDialog.showMessage("<html style='font-family: arial, helvetica, sans-serif; color: #555;'>" +
                sb.toString() + "</html>", ContextVS.getInstance().getMessage("votingBackupErrorCaption"));
    }

    public static void validateBackup(String decompressedBackupBaseDir, MetaInf metaInf) {
        log.debug("validateBackup - decompressedBackupBaseDir: " + decompressedBackupBaseDir);
        final BackupValidatorPane validatorPane = new BackupValidatorPane(decompressedBackupBaseDir, metaInf);
        validatorPane.init();
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            //stage.initOwner(window);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
            stage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
            stage.setScene(new Scene(validatorPane));
            stage.setTitle(ContextVS.getMessage("decompressBackupCaption"));
            stage.centerOnScreen();
            stage.show();
        });
    }

    public class BackupValidationTask extends Task<ResponseVS> {

        private String decompressedBackupBaseDir = null;

        public BackupValidationTask(String decompressedBackupBaseDir) {
            this.decompressedBackupBaseDir = decompressedBackupBaseDir;
        }

        @Override protected ResponseVS call() throws Exception {
            log.debug("worker.doInBackground: ");
            try {
                switch(metaInf.getType()) {
                    case VOTING_EVENT:
                        VotingBackupValidator votingBackupValidator = new VotingBackupValidator(
                                decompressedBackupBaseDir, BackupValidatorPane.this);
                        return votingBackupValidator.call();
                    case CLAIM_EVENT:
                        ClaimBackupValidator claimBackupValidator = new ClaimBackupValidator(
                                decompressedBackupBaseDir, BackupValidatorPane.this);
                        return claimBackupValidator.call();
                }
            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
            return new ResponseVS(ResponseVS.SC_ERROR, "Unknown event type: " + metaInf.getType());
        }
    }

}