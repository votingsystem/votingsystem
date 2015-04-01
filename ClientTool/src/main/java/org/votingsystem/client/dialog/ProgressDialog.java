package org.votingsystem.client.dialog;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import java.util.logging.Logger;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ProgressDialog extends VBox {

    private static Logger log = Logger.getLogger(ProgressDialog.class.getSimpleName());

    public ProgressDialog(Task<ResponseVS> progressTask) {
        setAlignment(Pos.CENTER);
        Label progressMessageLbl = new Label();
        progressMessageLbl.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-text-fill: #888;-fx-wrap-text: true;");
        progressMessageLbl.setWrapText(true);
        VBox.setVgrow(progressMessageLbl, Priority.ALWAYS);
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(350);
        setPrefHeight(200);
        progressBar.setLayoutY(10);
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> {
            progressTask.cancel(true);
            getScene().getWindow().hide();
        });
        cancelButton.setGraphic(Utils.getIcon(FontAwesomeIconName.TIMES, Utils.COLOR_RED_DARK));
        HBox footerButtonBox = new HBox();
        footerButtonBox.getChildren().addAll(Utils.getSpacer(), cancelButton);
        setMargin(footerButtonBox, new Insets(20, 20, 0, 10));
        setMargin(progressMessageLbl, new Insets(0, 0, 10, 0));
        getChildren().addAll(progressMessageLbl, progressBar, footerButtonBox);
        progressMessageLbl.textProperty().bind(progressTask.messageProperty());
        progressBar.progressProperty().bind(progressTask.progressProperty());
        this.visibleProperty().bind(progressTask.runningProperty());
        progressTask.setOnSucceeded(workerStateEvent -> ProgressDialog.this.getScene().getWindow().hide());
        progressTask.setOnCancelled(workerStateEvent -> ProgressDialog.this.getScene().getWindow().hide());
        progressTask.setOnFailed(workerStateEvent -> ProgressDialog.this.getScene().getWindow().hide());
        new Thread(progressTask).start();
    }

    public static void showDialog(Task progressTask, String caption, Window owner) {
        log.info("showDialog");
        Platform.runLater(() -> {
            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
            stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
            ProgressDialog progressDialog = new ProgressDialog(progressTask);
            progressDialog.getStyleClass().add("modal-dialog");
            stage.setScene(new Scene(progressDialog));
            stage.getScene().getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
            stage.setTitle(caption);
            stage.toFront();
            stage.show();
        });
    }

}
