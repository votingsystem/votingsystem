package org.votingsystem.applet.validationtool.dialog;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBoxBuilder;
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

    public FXProgressDialog(final Window window) {
        this.parentWindow = window;
        progressDialogStage = new Stage(StageStyle.TRANSPARENT);
        progressDialogStage.initModality(Modality.WINDOW_MODAL);
        progressDialogStage.initOwner(window);
        //progressDialogStage.setTitle("Updating");
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(15);
        progressDialogStage.setScene(
                new Scene(HBoxBuilder.create().styleClass("modal-dialog").children(progressBar).build(), Color.TRANSPARENT)
        );
        progressDialogStage.getScene().getStylesheets().add(getClass().getResource("../css/modal-dialog.css").toExternalForm());

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

    public void show(boolean isVisible) {
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
