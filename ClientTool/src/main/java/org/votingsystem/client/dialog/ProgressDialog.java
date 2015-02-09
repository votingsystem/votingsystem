package org.votingsystem.client.dialog;

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
import javafx.stage.*;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ProgressDialog extends VBox {

    private static Logger log = Logger.getLogger(ProgressDialog.class);

    public ProgressDialog(Task<ResponseVS> progressTask) {
        setAlignment(Pos.CENTER);
        setPrefWidth(300);
        Text progressMessageText = new Text();
        progressMessageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #555;");
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(10);
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> {
            progressTask.cancel(true);
            getScene().getWindow().hide();
        });
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        HBox footerButtonBox = new HBox();
        footerButtonBox.getChildren().addAll(Utils.getSpacer(), cancelButton);
        setMargin(footerButtonBox, new Insets(30, 20, 0, 10));
        setMargin(progressMessageText, new Insets(0, 0, 10, 0));
        getChildren().addAll(progressMessageText, progressBar, footerButtonBox);
        progressMessageText.textProperty().bind(progressTask.messageProperty());
        progressBar.progressProperty().bind(progressTask.progressProperty());
        this.visibleProperty().bind(progressTask.runningProperty());
        progressTask.setOnSucceeded(workerStateEvent -> ProgressDialog.this.getScene().getWindow().hide());
        progressTask.setOnCancelled(workerStateEvent -> ProgressDialog.this.getScene().getWindow().hide());
        progressTask.setOnFailed(workerStateEvent -> ProgressDialog.this.getScene().getWindow().hide());
        new Thread(progressTask).start();
    }

    public static void showDialog(Task<ResponseVS> progressTask, String caption) {
        log.debug("showDialog");
        Platform.runLater(() -> {
            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(VotingSystemApp.getInstance().getScene().getWindow());
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
            stage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
            ProgressDialog progressDialog = new ProgressDialog(progressTask);
            progressDialog.getStyleClass().add("modal-dialog");
            stage.setScene(new Scene(progressDialog));
            stage.getScene().getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
            stage.setTitle(caption);
            stage.show();
        });
    }

}
