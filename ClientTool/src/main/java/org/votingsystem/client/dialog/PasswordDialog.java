package org.votingsystem.client.dialog;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;

import java.awt.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class PasswordDialog {

    private static Logger logger = Logger.getLogger(PasswordDialog.class);

    private Stage stage;
    private VBox dialogVBox;
    private Text messageText;
    private Label capsLockPressedMessageLabel;
    private HBox messagePanel;
    private PasswordField password1Field;
    private PasswordField password2Field;
    private Button cancelButton;
    private String password;
    private String mainMessage = null;
    boolean isCapsLockPressed = false;

    public PasswordDialog() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent window) {      }
        });

        dialogVBox = new VBox(10);
        messageText = new Text();
        messageText.setWrappingWidth(320);
        messageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #870000;");
        VBox.setMargin(messageText, new Insets(0, 0, 15, 0));
        messageText.setTextAlignment(TextAlignment.CENTER);

        capsLockPressedMessageLabel = new Label(ContextVS.getMessage("capsLockKeyPressed"));
        capsLockPressedMessageLabel.setWrapText(true);
        capsLockPressedMessageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #870000;");

        password1Field = new PasswordField();
        password2Field = new PasswordField();

        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                stage.close();
            }});
        cancelButton.setGraphic(new ImageView(Utils.getImage(this, "cancel_16")));

        final Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                checkPasswords();
            }});
        acceptButton.setGraphic(new ImageView(Utils.getImage(this, "accept")));

        password1Field.addEventHandler(KeyEvent.KEY_PRESSED,
            new EventHandler<KeyEvent>() {
                public void handle(KeyEvent event) {
                    if ((event.getCode() == KeyCode.ENTER)) {
                        acceptButton.fire();
                    }
                    setCapsLockState(Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK));
                }
            }
        );

        password2Field.addEventHandler(KeyEvent.KEY_PRESSED,
            new EventHandler<KeyEvent>() {
                public void handle(KeyEvent event) {
                    if ((event.getCode() == KeyCode.ENTER)) {
                        acceptButton.fire();
                    }
                    setCapsLockState(Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK));
                }
            }
        );

        HBox footerButtonsBox = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footerButtonsBox.getChildren().addAll(acceptButton, spacer, cancelButton);
        VBox.setMargin(footerButtonsBox, new javafx.geometry.Insets(20, 0, 0, 0));


        Text password1Text = new Text(ContextVS.getMessage("password1Lbl"));
        Text password2Text = new Text(ContextVS.getMessage("password2Lbl"));
        password2Text.setStyle("-fx-spacing: 50;");

        dialogVBox.getChildren().addAll(messageText, password1Text, password1Field, password2Text, password2Field,
                footerButtonsBox);
        dialogVBox.getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(dialogVBox, Color.TRANSPARENT));
        stage.getScene().getStylesheets().add(getClass().getResource("/resources/css/modal-dialog.css").toExternalForm());

        dialogVBox.getStyleClass().add("message-lbl-bold");

        // allow the dialog to be dragged around.
        final Node root = stage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = stage.getX() - mouseEvent.getScreenX();
                dragDelta.y = stage.getY() - mouseEvent.getScreenY();
            }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                stage.setX(mouseEvent.getScreenX() + dragDelta.x);
                stage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
        dialogVBox.setPrefWidth(350);
    }

    private void setCapsLockState (boolean pressed) {
        this.isCapsLockPressed = pressed;
        setMessage(null);
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

    public void show(String mainMessage) {
        this.mainMessage = mainMessage;
        setMessage(mainMessage);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.showAndWait();
    }

    private void setMessage (String message) {
        if (message == null) messageText.setText(mainMessage);
        else messageText.setText(message);
        if(isCapsLockPressed) {
            if(!dialogVBox.getChildren().contains(capsLockPressedMessageLabel))
                dialogVBox.getChildren().add(0, capsLockPressedMessageLabel);
        }
        else dialogVBox.getChildren().removeAll(capsLockPressedMessageLabel);
        stage.sizeToScene();
    }

    class Delta { double x, y; }

}