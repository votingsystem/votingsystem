package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
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
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PasswordDialog {

    private static Logger log = Logger.getLogger(PasswordDialog.class);

    private Stage stage;
    private VBox dialogVBox;
    private Text messageText;
    private Text timeLimitedMessageText;
    private Label capsLockPressedMessageLabel;
    private PasswordField password1Field;
    private Text password2Text;
    private PasswordField password2Field;
    private String password;
    private String mainMessage = null;
    boolean isCapsLockPressed = false;
    boolean isWithPasswordConfirm = true;

    public PasswordDialog() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent window) {      }
        });

        dialogVBox = new VBox(10);
        messageText = new Text();
        messageText.setWrappingWidth(320);
        messageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #6c0404;-fx-end-margin: 15;");
        timeLimitedMessageText = new Text();
        timeLimitedMessageText.setStyle("-fx-font-size: 14;-fx-font-weight: bold;-fx-fill: #888;-fx-end-margin: 5; -fx-start-margin: 5;");
        messageText.setTextAlignment(TextAlignment.CENTER);

        capsLockPressedMessageLabel = new Label(ContextVS.getMessage("capsLockKeyPressed"));
        capsLockPressedMessageLabel.setWrapText(true);
        capsLockPressedMessageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6c0404;");

        password1Field = new PasswordField();
        password2Field = new PasswordField();

        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        cancelButton.setOnAction(event -> stage.close());

        final Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(event -> checkPasswords());
        acceptButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));

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

        footerButtonsBox.getChildren().addAll(cancelButton, spacer, acceptButton);
        VBox.setMargin(footerButtonsBox, new javafx.geometry.Insets(20, 0, 0, 0));


        Text password1Text = new Text(ContextVS.getMessage("password1Lbl"));
        password2Text = new Text(ContextVS.getMessage("password2Lbl"));
        password2Text.setStyle("-fx-spacing: 50;");

        dialogVBox.getChildren().addAll(messageText, password1Text, password1Field, password2Text, password2Field,
                footerButtonsBox);
        dialogVBox.getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(dialogVBox, Color.TRANSPARENT));
        stage.getScene().getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));

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
        stage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
    }

    private void setCapsLockState (boolean pressed) {
        this.isCapsLockPressed = pressed;
        setMessage(null);
    }

    public String getPassword() {
        return password;
    }

    public boolean isShowing() {
        return stage.isShowing();
    }

    private void checkPasswords() {
        log.debug("checkPasswords");
        String password1 = new String(password1Field.getText());
        String password2 = null;
        if(!isWithPasswordConfirm) password2 = password1;
        else password2 = new String(password2Field.getText());
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

    public void close() {
        stage.close();
    }


    public void show(String mainMessage) {
        this.mainMessage = mainMessage;
        isWithPasswordConfirm = true;
        setMessage(mainMessage);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.toFront();
        stage.showAndWait();
    }

    public void showWithoutPasswordConfirm(String mainMessage) {
        showWithoutPasswordConfirm(mainMessage, null);
    }

    public void showWithoutPasswordConfirm(String mainMessage, final Integer visibilityInSeconds) {
        this.mainMessage = mainMessage;
        isWithPasswordConfirm = false;
        setMessage(mainMessage);
        if(dialogVBox.getChildren().contains(password2Text) && dialogVBox.getChildren().contains(password2Field)) {
            dialogVBox.getChildren().removeAll(password2Text, password2Field);
        }
        if(visibilityInSeconds != null) {
            dialogVBox.getChildren().add(1, timeLimitedMessageText);
            Task task = new Task() {
                @Override protected Object call() throws Exception {
                    AtomicInteger secondsOpened = new AtomicInteger(0);
                    while(secondsOpened.get() < visibilityInSeconds) {
                        PlatformImpl.runLater(() -> timeLimitedMessageText.setText(ContextVS.getMessage(
                                "timeLimitedWebSocketMessage", visibilityInSeconds - secondsOpened.getAndIncrement())));
                        Thread.sleep(1000);
                    }
                    PlatformImpl.runLater(() -> {if(stage.isShowing()) stage.close();});
                    return null;
                }
            };
            new Thread(task).start();
        }
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.toFront();
        stage.showAndWait();
    }

    public void toFront() {
        stage.centerOnScreen();
        stage.toFront();
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