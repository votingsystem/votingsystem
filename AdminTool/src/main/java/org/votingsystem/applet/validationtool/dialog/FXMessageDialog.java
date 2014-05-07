package org.votingsystem.applet.validationtool.dialog;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.votingsystem.model.ContextVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class FXMessageDialog {

    private final Stage dialog;
    private Label messageLabel;

    public FXMessageDialog(final Window window) {
        dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(window);


        VBox verticalBox = new VBox(10);
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                dialog.close();
            }});
        verticalBox.getChildren().addAll(messageLabel, acceptButton);
        verticalBox.getStyleClass().add("modal-dialog");
        dialog.setScene(new Scene(verticalBox, Color.TRANSPARENT));
        dialog.getScene().getStylesheets().add(getClass().getResource("../css/modal-dialog.css").toExternalForm());
        // allow the dialog to be dragged around.
        final Node root = dialog.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = dialog.getX() - mouseEvent.getScreenX();
                dragDelta.y = dialog.getY() - mouseEvent.getScreenY();
            }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                dialog.setX(mouseEvent.getScreenX() + dragDelta.x);
                dialog.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
    }

    public void showMessage(String message) {
        messageLabel.setText(message);

        dialog.setX(dialog.getOwner().getX() + dialog.getOwner().getWidth() / 2 - dialog.getWidth() / 2);
        dialog.setY(dialog.getOwner().getY() + dialog.getOwner().getHeight() / 2 - dialog.getHeight() / 2);

        dialog.show();
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