package org.votingsystem.client.dialog;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.votingsystem.client.util.FXUtils;
import org.votingsystem.model.ContextVS;

import javax.swing.*;
import java.awt.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class FXPasswordDialog {

    private static Logger logger = Logger.getLogger(FXPasswordDialog.class);

    private Stage stage;
    private VBox dialogBox;
    private Label messageLabel;
    private HBox messagePanel;
    private PasswordField password1Field;
    private PasswordField password2Field;
    private Button cancelButton;
    private String password;
    private String mainMessage = null;
    boolean isCapsLockPressed = false;

    public FXPasswordDialog() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent window) {      }
        });

        dialogBox = new VBox(10);
        messageLabel = new Label();
        messageLabel.setWrapText(true);

        password1Field = new PasswordField();
        password2Field = new PasswordField();


        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                stage.close();
            }});
        cancelButton.setGraphic(new ImageView(FXUtils.getImage(this, "cancel_16")));

        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                checkPasswords();
            }});
        acceptButton.setGraphic(new ImageView(FXUtils.getImage(this, "accept")));


        HBox footerButtonsBox = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footerButtonsBox.getChildren().addAll(acceptButton, spacer, cancelButton);
        VBox.setMargin(footerButtonsBox, new javafx.geometry.Insets(20, 0, 0, 0));



        dialogBox.getChildren().addAll(messageLabel, password1Field, password2Field, footerButtonsBox);
        dialogBox.getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(dialogBox, Color.TRANSPARENT));
        stage.getScene().getStylesheets().add(getClass().getResource("/resources/css/modal-dialog.css").toExternalForm());
        // allow the dialog to be dragged around.
        final Node root = stage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = stage.getX() - mouseEvent.getScreenX();
                dragDelta.y = stage.getY() - mouseEvent.getScreenY();
            }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                stage.setX(mouseEvent.getScreenX() + dragDelta.x);
                stage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
    }


    public String getPassword() {
        return password;
    }

    private void checkPasswords() {
        logger.debug("checkPasswords");
        String password1 = new String(password1Field.getText());
        String password2 = new String(password2Field.getText());
        if(password1.trim().isEmpty() && password2.trim().isEmpty()) setMessage(ContextVS.getMessage("passwordMissing"));
        else {
            if (password1.equals(password2)) {
                password = password1;
                stage.close();
            } else {
                setMessage(ContextVS.getMessage("passwordError"));
                password1Field.setText("");
                password2Field.setText("");
            }
        }
    }

    public void show(String mainMessage, String caption) {
        this.mainMessage = mainMessage;
        setMessage(mainMessage);
        if(caption != null) stage.setTitle(caption);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
    }

    private void setMessage (String mensaje) {
        if (mensaje == null) {
            if(isCapsLockPressed) {
                messageLabel.setText("<html><b>" +
                        ContextVS.getMessage("capsLockKeyPressed") + "</b><br/><br/>" + mainMessage + "</html>");
            } else {
                messageLabel.setText(mainMessage);
            }
        } else {
            if(isCapsLockPressed) {
                messageLabel.setText("<html><b>" + ContextVS.getMessage("capsLockKeyPressed")+ "</b><br/>" +
                        mensaje + "</html>");
            }  else messageLabel.setText(mensaje);
        }
        stage.sizeToScene();
    }

    class Delta { double x, y; }

}