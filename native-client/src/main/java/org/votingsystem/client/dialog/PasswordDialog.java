package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Utils;
import org.votingsystem.util.Messages;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PasswordDialog extends AppDialog {

    private static Logger log = Logger.getLogger(PasswordDialog.class.getName());

    public interface Listener {
        public void processPassword(char[] password);
    }

    private VBox dialogVBox;
    private Text messageText;
    private Text timeLimitedMessageText;
    private Label capsLockPressedMessageLabel;
    private PasswordField password1Field;
    private Text password2Text;
    private PasswordField password2Field;
    private char[] password;
    private String mainMessage = null;
    private Listener listener;
    boolean isCapsLockPressed = false;
    boolean isWithPasswordConfirm = true;
    private static PasswordDialog INSTANCE = null;

    public PasswordDialog() {
        super(new VBox(10));
        dialogVBox = (VBox) getContentPane();
        dialogVBox.setStyle("-fx-pref-width: 350px;-fx-padding: 0 20 20 20;-fx-alignment: center;" +
                "-fx-font-size: 16;-fx-font-weight: bold;-fx-color: #f9f9f9;");

        messageText = new Text();
        messageText.setWrappingWidth(320);
        messageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #6c0404;-fx-end-margin: 25;");
        timeLimitedMessageText = new Text();
        timeLimitedMessageText.setStyle("-fx-font-size: 14;-fx-font-weight: bold;-fx-fill: #888;-fx-end-margin: 5; -fx-start-margin: 5;");
        messageText.setTextAlignment(TextAlignment.CENTER);

        capsLockPressedMessageLabel = new Label(Messages.currentInstance().get("capsLockKeyPressed"));
        capsLockPressedMessageLabel.setWrapText(true);
        capsLockPressedMessageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6c0404;");

        password1Field = new PasswordField();
        password2Field = new PasswordField();

        addCloseListener(event -> closePasswordDialog());

        final Button acceptButton = new Button(Messages.currentInstance().get("acceptLbl"));
        acceptButton.setOnAction(event -> checkPasswords());
        acceptButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));

        password1Field.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
                    if ((keyEvent.getCode() == KeyCode.ENTER)) {
                        acceptButton.fire();
                    }
                    setCapsLockState(Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK));
                }
        );

        password2Field.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
                    if ((keyEvent.getCode() == KeyCode.ENTER)) {
                        acceptButton.fire();
                    }
                    setCapsLockState(Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK));
                }
        );

        HBox footerButtonsBox = new HBox();
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), acceptButton);
        VBox.setMargin(footerButtonsBox, new javafx.geometry.Insets(20, 0, 0, 0));

        Text password1Text = new Text(Messages.currentInstance().get("password1Lbl"));
        password2Text = new Text(Messages.currentInstance().get("password2Lbl"));
        //password2Text.setStyle("-fx-spacing: 50;");
        dialogVBox.getChildren().addAll(messageText, timeLimitedMessageText, password1Text, password1Field,
                password2Text, password2Field,
                footerButtonsBox);
    }

    private void setCapsLockState (boolean pressed) {
        this.isCapsLockPressed = pressed;
        setMessage(null);
    }

    public boolean isShowing() {
        return getStage().isShowing();
    }

    private void closePasswordDialog() {
        if(password == null) listener.processPassword(password);
        hide();
    }

    private void checkPasswords() {
        log.info("checkPasswords");
        String password1 = new String(password1Field.getText());
        String password2 = null;
        if(!isWithPasswordConfirm) password2 = password1;
        else password2 = new String(password2Field.getText());
        if(password1.trim().isEmpty() && password2.trim().isEmpty()) setMessage(Messages.currentInstance().get("password1Lbl"));
        else {
            if (password1.equals(password2)) {
                password = password1.toCharArray();
                listener.processPassword(password);
                getStage().close();
            } else {
                setMessage(Messages.currentInstance().get("passwordError"));
                password1Field.setText("");
                password2Field.setText("");
                password = null;
            }
        }
    }

    public static void showWithPasswordConfirm(Listener listener, String mainMessage) {
        if(INSTANCE == null) INSTANCE = new PasswordDialog();
        INSTANCE.listener = listener;
        Platform.runLater(() -> {
            INSTANCE.password1Field.setText("");
            INSTANCE.password2Field.setText("");
            INSTANCE.password = null;
            INSTANCE.timeLimitedMessageText.setVisible(false);
            INSTANCE.mainMessage = mainMessage;
            INSTANCE.isWithPasswordConfirm = true;
            INSTANCE.setMessage(mainMessage);
            INSTANCE.showWithPasswordConfirm();
        });
    }

    private void showWithPasswordConfirm() {
        if(!dialogVBox.getChildren().contains(password2Text)) {
            dialogVBox.getChildren().add(4, password2Text);
            dialogVBox.getChildren().add(5, password2Field);
        }
        show();
    }

    public static void showWithoutPasswordConfirm(Listener listener, String mainMessage) {
        showWithoutPasswordConfirm(listener, mainMessage, null);
    }

    public static void showWithoutPasswordConfirm(Listener listener, String mainMessage, Integer visibilityInSeconds) {
        Platform.runLater(() -> {
            if(INSTANCE == null) INSTANCE = new PasswordDialog();
            INSTANCE.listener = listener;
            INSTANCE.showWithoutPasswordConfirm(mainMessage, visibilityInSeconds);
        });
    }


    private void showWithoutPasswordConfirm(String mainMessage, final Integer visibilityInSeconds) {
        this.mainMessage = mainMessage;
        isWithPasswordConfirm = false;
        setMessage(mainMessage);
        password1Field.setText("");
        password2Field.setText("");
        password = null;
        timeLimitedMessageText.setVisible(false);
        if(dialogVBox.getChildren().contains(password2Text) && dialogVBox.getChildren().contains(password2Field)) {
            dialogVBox.getChildren().removeAll(password2Text, password2Field);
        }
        if(visibilityInSeconds != null) {
            timeLimitedMessageText.setVisible(true);
            Task task = new Task() {
                @Override protected Object call() throws Exception {
                    AtomicInteger secondsOpened = new AtomicInteger(0);
                    while(secondsOpened.get() < visibilityInSeconds) {
                        PlatformImpl.runLater(() -> timeLimitedMessageText.setText(Messages.currentInstance().get(
                                "timeLimitedWebSocketMessage", visibilityInSeconds - secondsOpened.getAndIncrement())));
                        Thread.sleep(1000);
                    }
                    PlatformImpl.runLater(() -> {if(getStage().isShowing()) getStage().close();});
                    return null;
                }
            };
            new Thread(task).start();
        }
        show();
    }

    private void setMessage (String message) {
        if (message == null) messageText.setText(mainMessage);
        else messageText.setText(message);
        password = null;
        if(isCapsLockPressed) {
            if(!dialogVBox.getChildren().contains(capsLockPressedMessageLabel))
                dialogVBox.getChildren().add(0, capsLockPressedMessageLabel);
        } else dialogVBox.getChildren().removeAll(capsLockPressedMessageLabel);
        getStage().sizeToScene();
    }

}