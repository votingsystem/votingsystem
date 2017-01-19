package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Window;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.backup.BackupValidator;
import org.votingsystem.client.backup.ElectionBackupValidator;
import org.votingsystem.client.backup.ValidationEvent;
import org.votingsystem.client.backup.ValidatorListener;
import org.votingsystem.client.dialog.AppDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.ElectionStatsDto;
import org.votingsystem.util.Messages;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BackupValidatorPane extends VBox implements ValidatorListener {

    private static Logger log = Logger.getLogger(BackupValidatorPane.class.getName());

    private java.util.List<String> errorList;
    private ElectionStatsDto electionStats;
    private Integer filesProcessed = 0;
    private Long numFilesToProcess = 0L;
    private Button errorsButton;
    private ProgressBar progressBar;
    private Text progressMessageText;
    private Text progressMessageCounter;
    private VBox progressBox;
    private Button cancelButton;
    private BackupValidationTask runningTask;


    public BackupValidatorPane(String decompressedBackupBaseDir, ElectionStatsDto electionStats) {
        this.electionStats = electionStats;
        progressBox = new VBox();
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPrefWidth(330);
        progressBox.setPrefHeight(150);
        progressMessageText = new Text();
        progressMessageCounter = new Text();
        progressMessageText.setStyle("-fx-font-size: 14;-fx-font-weight: bold;-fx-fill: #555;");
        progressMessageCounter.setStyle("-fx-font-size: 9;-fx-font-weight: bold;-fx-fill: #888;-fx-start-margin: 10;");
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(10);
        HBox buttonHBox = new HBox();
        errorsButton = new Button(Messages.currentInstance().get("errorsLbl"));
        errorsButton.setOnAction(actionEvent -> showErrors());
        errorsButton.setVisible(false);
        cancelButton = new Button(Messages.currentInstance().get("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> {
                runningTask.cancelValidation();
                getScene().getWindow().hide();
            });
        cancelButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        buttonHBox.getChildren().addAll(errorsButton, Utils.getSpacer(), cancelButton);
        setMargin(buttonHBox, new Insets(0, 20, 20, 20));
        progressBox.getChildren().addAll(progressMessageText, progressMessageCounter, progressBar);
        getChildren().addAll(progressBox, buttonHBox);
        runningTask = new BackupValidationTask(decompressedBackupBaseDir);
        File backupDir = new File(decompressedBackupBaseDir);
        numFilesToProcess = electionStats.getNumIdentityRequests() + electionStats.getNumVotes();
    }

    private void init() {
        new Thread(runningTask).start();
    }

    @Override public void processValidationEvent(ValidationEvent validationEvent) {
        Platform.runLater(() -> {
            processVotingValidationEvent(validationEvent);
        });
    }

    public void processVotingValidationEvent(ValidationEvent validationEvent) {
        if(!validationEvent.getErrorList().isEmpty()) {
            this.errorList = validationEvent.getErrorList();
            errorsButton.setVisible(true);
            String msg =  (errorList.size() > 1) ? Messages.currentInstance().get("errorsLbl"):
                    Messages.currentInstance().get("errorLbl");
            errorsButton.setText(errorList.size() + " " + msg);
        }
        Platform.runLater(() -> {
            progressBar.setProgress((filesProcessed++).doubleValue() / numFilesToProcess.doubleValue());
            progressMessageCounter.setText(filesProcessed + "/" + numFilesToProcess);
        });

        switch(validationEvent.getType()) {
            case ACCESS_REQUEST:
                progressMessageText.setText(Messages.currentInstance().get("validatingAccessRequestsMsg"));
                break;
            case ACCESS_REQUEST_FINISH:
                break;
            case VOTE:
                progressMessageText.setText(Messages.currentInstance().get("validatingVotesMsg"));
                break;
            case VOTE_FINISH:
                String message = null;
                if(errorList == null || errorList.isEmpty()) {
                    message = Messages.currentInstance().get("validationWithoutErrorsMsg",validationEvent.getMessage());
                } else {
                    message = Messages.currentInstance().get("validationWithErrorsMsg", validationEvent.getMessage());
                }
                MainApp.showMessage(message, Messages.currentInstance().get("validationFinishedLbl"));
                if(errorList!= null && errorList.size() > 0) {
                    progressBox.setVisible(false);
                } else getScene().getWindow().hide();
                break;
            default:
                log.info("processVotingValidationEvent - unprocessed: " + validationEvent.getType());
        }
    }

    private void showErrors() {
        StringBuilder sb = new StringBuilder();
        int numError = 0;
        for(String error: errorList) {
            sb.append("<br/><br/><b>" + ++numError + "</b> - " + error);
        }
        MainApp.showMessage(sb.toString(), Messages.currentInstance().get("votingBackupErrorCaption"));
    }

    public static void validateBackup(String decompressedBackupBaseDir, ElectionStatsDto electionStats, Window parentWindow) {
        log.info("validateBackup - decompressedBackupBaseDir: " + decompressedBackupBaseDir);
        Platform.runLater(() -> {
            BackupValidatorPane validatorPane = new BackupValidatorPane(decompressedBackupBaseDir, electionStats);
            validatorPane.init();
            new AppDialog(validatorPane).setCaption(Messages.currentInstance().get("checkBackupCaption")).show();
        });
    }

    public class BackupValidationTask extends Task<ResponseDto> {

        private String decompressedBackupBaseDir = null;
        private BackupValidator<ResponseDto> backupValidator;

        public BackupValidationTask(String decompressedBackupBaseDir) {
            this.decompressedBackupBaseDir = decompressedBackupBaseDir;
        }

        public void cancelValidation() {
            if(backupValidator != null) backupValidator.cancel();
            this.cancel(true);
        }

        @Override protected ResponseDto call() {
            log.info("worker.doInBackground");
            try {
                backupValidator = new ElectionBackupValidator(
                        decompressedBackupBaseDir, BackupValidatorPane.this);
                return backupValidator.call();
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                MainApp.showMessage(ResponseDto.SC_ERROR, ex.getMessage());
            }
            return new ResponseDto(ResponseDto.SC_ERROR, "Error validating backup");
        }
    }

}