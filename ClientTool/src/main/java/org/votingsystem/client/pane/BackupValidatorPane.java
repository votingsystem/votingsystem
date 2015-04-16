package org.votingsystem.client.pane;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.votingsystem.client.backup.BackupValidator;
import org.votingsystem.client.backup.ElectionBackupValidator;
import org.votingsystem.client.backup.ValidationEvent;
import org.votingsystem.client.backup.ValidatorListener;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.voting.MetaInf;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BackupValidatorPane extends VBox implements ValidatorListener<ValidationEvent> {

    private static Logger log = Logger.getLogger(BackupValidatorPane.class.getSimpleName());

    private java.util.List<String> errorList;
    private MetaInf metaInf;
    private Integer filesProcessed = 0;
    private Integer numFilesToProcess = 0;
    private Button errorsButton;
    private ProgressBar progressBar;
    private Text progressMessageText;
    private Text progressMessageCounter;
    private VBox progressBox;
    private Button cancelButton;
    private BackupValidationTask runningTask;


    public BackupValidatorPane(String decompressedBackupBaseDir, MetaInf metaInf) {
        this.metaInf = metaInf;
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
        errorsButton = new Button(ContextVS.getMessage("errorsLbl"));
        errorsButton.setOnAction(actionEvent -> showErrors());
        errorsButton.setVisible(false);
        cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> {
                runningTask.cancelValidation();
                getScene().getWindow().hide();
            });
        cancelButton.setGraphic(Utils.getIcon(FontAwesomeIconName.TIMES, Utils.COLOR_RED_DARK));
        buttonHBox.getChildren().addAll(errorsButton, Utils.getSpacer(), cancelButton);
        setMargin(buttonHBox, new Insets(0, 20, 20, 20));
        progressBox.getChildren().addAll(progressMessageText, progressMessageCounter, progressBar);
        getChildren().addAll(progressBox, buttonHBox);
        runningTask = new BackupValidationTask(decompressedBackupBaseDir);
        File backupDir = new File(decompressedBackupBaseDir);
        numFilesToProcess = metaInf.getNumFilesToProcess().intValue();
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
        Platform.runLater(() -> {
            progressBar.setProgress((filesProcessed++).doubleValue() / numFilesToProcess.doubleValue());
            progressMessageCounter.setText(filesProcessed + "/" + numFilesToProcess);
        });

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
                showMessage(message, ContextVS.getMessage("validationFinishedLbl"));
                if(errorList!= null && errorList.size() > 0) {
                    progressBox.setVisible(false);
                } else getScene().getWindow().hide();
                break;
            default: log.info("processVotingValidationEvent - unprocessed: " + responseVS.getData().toString());
        }
    }

    private void showErrors() {
        StringBuilder sb = new StringBuilder();
        int numError = 0;
        for(String error: errorList) {
            sb.append("<br/><br/><b>" + ++numError + "</b> - " + error);
        }
        showMessage(sb.toString(), ContextVS.getInstance().getMessage("votingBackupErrorCaption"));
    }

    public static void validateBackup(String decompressedBackupBaseDir, MetaInf metaInf, Window parentWindow) {
        log.info("validateBackup - decompressedBackupBaseDir: " + decompressedBackupBaseDir);
        final BackupValidatorPane validatorPane = new BackupValidatorPane(decompressedBackupBaseDir, metaInf);
        validatorPane.init();
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.initOwner(parentWindow);
            stage.initModality(Modality.WINDOW_MODAL);
            //stage.initOwner(window);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
            stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
            stage.setScene(new Scene(validatorPane));
            stage.setTitle(ContextVS.getMessage("checkBackupCaption"));
            stage.centerOnScreen();
            stage.show();
        });
    }

    public class BackupValidationTask extends Task<ResponseVS> {

        private String decompressedBackupBaseDir = null;
        private BackupValidator<ResponseVS> backupValidator;

        public BackupValidationTask(String decompressedBackupBaseDir) {
            this.decompressedBackupBaseDir = decompressedBackupBaseDir;
        }

        public void cancelValidation() {
            if(backupValidator != null) backupValidator.cancel();
            this.cancel(true);
        }

        @Override protected ResponseVS call() throws Exception {
            log.info("worker.doInBackground");
            try {
                switch(metaInf.getType()) {
                    case VOTING_EVENT:
                        backupValidator = new ElectionBackupValidator (
                                decompressedBackupBaseDir, BackupValidatorPane.this);
                        return backupValidator.call();
                }
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
            return new ResponseVS(ResponseVS.SC_ERROR, "Unknown event type: " + metaInf.getType());
        }
    }

}