package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.votingsystem.client.backup.*;
import org.votingsystem.client.model.MetaInf;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BackupValidatorPane extends StackPane implements ValidatorListener<ValidationEvent> {

    private static Logger logger = Logger.getLogger(BackupValidatorPane.class);

    private java.util.List<String> errorList;
    private MetaInf metaInf;
    private int filesProcessed = 0;
    private int numFilesToProcess = 0;
    private Button errorsButton;
    private ValidatorListener validatorListener = null;
    private ProgressBar progressBar;
    private Text progressMessageText;
    private Button cancelButton;
    private BackupValidationTask runningTask;


    public BackupValidatorPane(String decompressedBackupBaseDir, MetaInf metaInf) {
        this.metaInf = metaInf;
        Region progressRegion = new Region();
        progressRegion.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        progressRegion.setPrefSize(240, 160);

        VBox progressBox = new VBox();
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPrefWidth(400);
        progressBox.setPrefHeight(300);

        progressMessageText = new Text();
        progressMessageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #f9f9f9;");
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(10);



        HBox buttonHBox = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        errorsButton = new Button(ContextVS.getMessage("errorsLbl"));
        errorsButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                showErrors();
            }});
        errorsButton.setVisible(false);


        cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                runningTask.cancel();
            }});
        cancelButton.setGraphic(new ImageView(Utils.getImage(this, "cancel_16")));

        buttonHBox.getChildren().addAll(errorsButton, spacer, cancelButton);

        setMargin(buttonHBox, new Insets(30, 20, 0, 20));

        progressBox.getChildren().addAll(progressMessageText, progressBar);
        getChildren().addAll(progressRegion, progressBox, buttonHBox);

        runningTask = new BackupValidationTask(decompressedBackupBaseDir);


        File backupDir = new File(decompressedBackupBaseDir);
        numFilesToProcess = backupDir.listFiles().length;
    }

    private void init() {
        new Thread(runningTask).start();
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
                ImageView icon = null;
                if(errorList == null || errorList.isEmpty()) {
                    icon = new ImageView(Utils.getImage(this, "accept_32"));
                    message = ContextVS.getMessage("validationWithoutErrorsMsg",responseVS.getMessage());
                } else {
                    icon = new ImageView(Utils.getImage(this, "cancel_32"));
                    message = ContextVS.getMessage("validationWithErrorsMsg", responseVS.getMessage());
                }
                cancelButton.setText(ContextVS.getInstance().getMessage("closeLbl"));

                progressMessageText.setText(message);
                progressBar.setVisible(false);
                break;
        }
    }

    public void processManifestValidationEvent( ResponseVS<ValidationEvent> responseVS) {
        if(!responseVS.getErrorList().isEmpty()) {
            errorsButton.setVisible(true);
            String msg =  (errorList.size() > 1)? ContextVS.getInstance().getMessage("errorsLbl"):
                    ContextVS.getInstance().getMessage("errorLbl");
            errorsButton.setText(errorList.size() + " " + msg);
        }
        progressBar.setProgress(filesProcessed++ / numFilesToProcess);
        switch(responseVS.getData()) {
            case MANIFEST:
                progressMessageText.setText(ContextVS.getInstance().getMessage("validatingManifestsMsg"));
                break;
            case MANIFEST_FINISIH:
                String message = null;
                ImageView icon = null;
                if(errorList == null || errorList.isEmpty()) {
                    icon = new ImageView(Utils.getImage(this, "accept_32"));
                    message = ContextVS.getMessage("validationWithoutErrorsMsg",responseVS.getMessage());
                } else {
                    icon = new ImageView(Utils.getImage(this, "cancel_32"));
                    message = ContextVS.getMessage("validationWithErrorsMsg", responseVS.getMessage());
                }
                cancelButton.setText(ContextVS.getMessage("closeLbl"));
                progressMessageText.setText(message);
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
        progressBar.setProgress(filesProcessed++ / numFilesToProcess);
        switch(responseVS.getData()) {
            case CLAIM:
                progressMessageText.setText(ContextVS.getInstance().getMessage("validatingClaimsDataMsg"));
                break;
            case CLAIM_FINISH:
                String message = null;
                ImageView icon = null;
                if(errorList == null || errorList.isEmpty()) {
                    icon = new ImageView(Utils.getImage(this, "accept_32"));
                    message = ContextVS.getMessage("validationWithoutErrorsMsg",responseVS.getMessage());
                } else {
                    icon = new ImageView(Utils.getImage(this, "cancel_32"));
                    message = ContextVS.getMessage("validationWithErrorsMsg", responseVS.getMessage());
                }
                cancelButton.setText(ContextVS.getMessage("closeLbl"));
                progressMessageText.setText(message);
                break;
        }
    }

    private void showErrors() {
        String resultMessange = "";
        int numError = 0;
        for(String error: errorList) {
            resultMessange = resultMessange + "<br/> *** " + ++numError + " - " + error;
        }
        HTMLMessageDialog messageDialog = new HTMLMessageDialog();
        messageDialog.showMessage("<html>" + resultMessange + "</html>",
                ContextVS.getInstance().getMessage("votingBackupErrorCaption"));
    }

    public static void showDialog(String decompressedBackupBaseDir, MetaInf metaInf) {
        logger.debug("showDialog - zipFilePath");
        final BackupValidatorPane decompressBackupPane = new BackupValidatorPane(decompressedBackupBaseDir, metaInf);
        decompressBackupPane.init();
        Platform.runLater(new Runnable() {
            @Override public void run() {
                Stage stage = new Stage();
                stage.initModality(Modality.WINDOW_MODAL);
                //stage.initOwner(window);
                stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
                    @Override public void handle(WindowEvent window) { }
                });
                stage.setScene(new Scene(decompressBackupPane));
                stage.setTitle(ContextVS.getMessage("decompressBackupCaption"));
                stage.centerOnScreen();
                stage.show();
            }
        });
    }

    public class BackupValidationTask extends Task<ResponseVS> {

        private String decompressedBackupBaseDir = null;

        public BackupValidationTask(String decompressedBackupBaseDir) {
            this.decompressedBackupBaseDir = decompressedBackupBaseDir;
        }

        @Override protected ResponseVS call() throws Exception {
            logger.debug("worker.doInBackground: ");
            try {
                switch(metaInf.getType()) {
                    case VOTING_EVENT:
                        VotingBackupValidator votingBackupValidator = new VotingBackupValidator(
                                decompressedBackupBaseDir, validatorListener);
                        return votingBackupValidator.call();
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
    }
}
