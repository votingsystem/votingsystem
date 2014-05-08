package org.votingsystem.admintool.dialog;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class FXProgressDialog {

    private final Stage progressDialogStage;
    private final Window parentWindow;
    private Label messageLabel;
    private VBox verticalBox;

    public FXProgressDialog(final Window window) {
        this.parentWindow = window;
        progressDialogStage = new Stage(StageStyle.TRANSPARENT);
        progressDialogStage.initModality(Modality.WINDOW_MODAL);
        progressDialogStage.initOwner(window);
        //progressDialogStage.setTitle("Updating");


        verticalBox = new VBox(10);
        messageLabel = new Label();
        messageLabel.setWrapText(true);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(15);

        verticalBox.getChildren().addAll(messageLabel, progressBar);
        verticalBox.getStyleClass().add("modal-dialog");
        progressDialogStage.setScene(new Scene(verticalBox, Color.TRANSPARENT));

        progressDialogStage.getScene().getStylesheets().add(getClass().getResource("/resources/css/modal-dialog.css").toExternalForm());


        // allow the dialog to be dragged around.
        final Node root = progressDialogStage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = progressDialogStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = progressDialogStage.getY() - mouseEvent.getScreenY();
            }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                progressDialogStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                progressDialogStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
        progressDialogStage.initOwner(window);
    }

    public void show(String message, boolean isVisible) {
        if(message == null || "".equals(message)) {
            messageLabel.setText("");
            verticalBox.getChildren().removeAll(messageLabel);
        } else {
            messageLabel.setText(message);
            verticalBox.getChildren().add(0, messageLabel);
        }
        if(isVisible) {
            //parentWindow.getScene().getRoot().setEffect(new BoxBlur());
            progressDialogStage.show();
        } else {
            //parentWindow.getScene().getRoot().setEffect(null);
            progressDialogStage.hide();
        }
    }

    /*private void clickShow(ActionEvent event) {
        Stage stage = new Stage();
        Parent root = FXMLLoader.load(YourClassController.class.getResource("YourClass.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("My modal window");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(((Node)event.getSource()).getScene().getWindow() );
        stage.show();
    }*/

    class Delta { double x, y; }

}
