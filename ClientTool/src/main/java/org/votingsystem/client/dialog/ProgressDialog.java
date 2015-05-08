package org.votingsystem.client.dialog;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ProgressDialog extends DialogVS {

    private static Logger log = Logger.getLogger(ProgressDialog.class.getSimpleName());

    public ProgressDialog(Task<ResponseVS> progressTask) {
        super(new VBox(10));
        VBox mainDialog = (VBox) getContentPane();
        mainDialog.getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        mainDialog.getStyleClass().add("modal-dialog");
        Label progressMessageLbl = new Label();
        progressMessageLbl.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-text-fill: #888;-fx-wrap-text: true;");
        progressMessageLbl.setWrapText(true);
        VBox.setVgrow(progressMessageLbl, Priority.ALWAYS);
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(350);
        mainDialog.setPrefHeight(200);
        progressBar.setLayoutY(10);
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> {
            progressTask.cancel(true);
            hide();
        });
        cancelButton.setGraphic(Utils.getIcon(FontAwesomeIcons.TIMES, Utils.COLOR_RED_DARK));
        HBox footerButtonBox = new HBox();
        footerButtonBox.getChildren().addAll(Utils.getSpacer(), cancelButton);
        mainDialog.setMargin(footerButtonBox, new Insets(20, 20, 0, 10));
        mainDialog.setMargin(progressMessageLbl, new Insets(0, 0, 10, 0));
        mainDialog.getChildren().addAll(progressMessageLbl, progressBar, footerButtonBox);
        progressMessageLbl.textProperty().bind(progressTask.messageProperty());
        progressBar.progressProperty().bind(progressTask.progressProperty());
        mainDialog.visibleProperty().bind(progressTask.runningProperty());
        progressTask.setOnSucceeded(workerStateEvent -> hide());
        progressTask.setOnCancelled(workerStateEvent -> hide());
        progressTask.setOnFailed(workerStateEvent -> hide());
        new Thread(progressTask).start();
    }

    public static void showDialog(Task progressTask) {
        log.info("showDialog");
        showDialog(progressTask, null);
    }

    public static void showDialog(Task progressTask, String caption) {
        log.info("showDialog");
        Platform.runLater(() -> {
            ProgressDialog progressDialog = new ProgressDialog(progressTask);
            progressDialog.show(caption);
        });
    }
}
