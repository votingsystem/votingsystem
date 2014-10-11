package org.votingsystem.client.pane;

import com.sun.javafx.application.PlatformImpl;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.apache.log4j.Logger;
import org.votingsystem.client.util.SignatureService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSPane extends StackPane {

    private static Logger log = Logger.getLogger(BrowserVSPane.class);

    private Text messageText;
    private boolean isCapsLockPressed = false;
    private Text capsLockPressedMessageText;
    private String password;
    private PasswordField password1Field;
    private PasswordField password2Field;
    private VBox passwordVBox;
    private Region passwordRegion;
    private String mainMessage = null;
    private final SignatureService signatureService;
    private OperationVS operationVS;

    public BrowserVSPane() {
        this.signatureService = new SignatureService();
        Region progressRegion = new Region();
        progressRegion.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        progressRegion.setPrefSize(240, 160);

        passwordRegion = new Region();
        passwordRegion.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        passwordRegion.setPrefSize(240, 160);

        VBox progressBox = new VBox();
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPrefWidth(400);
        progressBox.setPrefHeight(300);

        Text progressMessageText = new Text();
        progressMessageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #f9f9f9;");
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(10);
        progressMessageText.textProperty().bind(signatureService.messageProperty());
        progressBar.progressProperty().bind(signatureService.progressProperty());
        progressRegion.visibleProperty().bind(signatureService.runningProperty());
        progressBox.visibleProperty().bind(signatureService.runningProperty());
        progressBox.getChildren().addAll(progressMessageText, progressBar);

        passwordVBox = new VBox(10);
        messageText = new Text();
        messageText.setWrappingWidth(320);
        messageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #6c0404;");
        VBox.setMargin(messageText, new Insets(0, 0, 15, 0));
        messageText.setTextAlignment(TextAlignment.CENTER);

        capsLockPressedMessageText = new Text(ContextVS.getMessage("capsLockKeyPressed"));
        capsLockPressedMessageText.setWrappingWidth(320);
        capsLockPressedMessageText.setStyle("-fx-font-weight: bold; -fx-fill: #6c0404;");
        VBox.setMargin(messageText, new Insets(0, 0, 15, 0));
        capsLockPressedMessageText.setTextAlignment(TextAlignment.CENTER);

        password1Field = new PasswordField();
        password2Field = new PasswordField();

        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                setPasswordDialogVisible(false);
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
                    setCapsLockState(java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(
                            java.awt.event.KeyEvent.VK_CAPS_LOCK));
                }
            }
        );

        password2Field.addEventHandler(KeyEvent.KEY_PRESSED,
            new EventHandler<KeyEvent>() {
                public void handle(KeyEvent event) {
                    if ((event.getCode() == KeyCode.ENTER)) {
                        acceptButton.fire();
                    }
                    setCapsLockState(java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(
                            java.awt.event.KeyEvent.VK_CAPS_LOCK));
                }
            }
        );

        HBox footerButtonsBox = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footerButtonsBox.getChildren().addAll(acceptButton, spacer, cancelButton);
        VBox.setMargin(footerButtonsBox, new Insets(20, 20, 10, 20));

        Text password1Text = new Text(ContextVS.getMessage("password1Lbl"));
        Text password2Text = new Text(ContextVS.getMessage("password2Lbl"));
        password2Text.setStyle("-fx-spacing: 50;");

        passwordVBox.getChildren().addAll(messageText, password1Text, password1Field, password2Text, password2Field,
                footerButtonsBox);
        passwordVBox.getStylesheets().add(getClass().getResource("/resources/css/modal-dialog.css").toExternalForm());

        passwordVBox.getStyleClass().add("message-lbl-bold");
        passwordVBox.getStyleClass().add("modal-dialog");
        passwordVBox.setStyle("-fx-background-color: #f9f9f9; -fx-max-height:280px;-fx-max-width:350px;");
        passwordVBox.autosize();
        setPasswordDialogVisible(false);
        getChildren().addAll(progressRegion, progressBox, passwordRegion, passwordVBox);
    }

    public SignatureService getSignatureService() {
        return this.signatureService;
    }

    public void processOperationVS(OperationVS operationVS) {
        this.operationVS = operationVS;
        PlatformImpl.runAndWait(new Runnable() {
            @Override public void run() {
                setPasswordDialogVisible(true);
            }
        });
    }

    public void setPasswordDialogVisible(boolean isVisible) {
        setMessage(ContextVS.getMessage("passwordMissing"));
        passwordVBox.setVisible(isVisible);
        passwordRegion.setVisible(isVisible);
    }

    private void setCapsLockState (boolean pressed) {
        this.isCapsLockPressed = pressed;
        setMessage(ContextVS.getMessage("passwordMissing"));
    }

    public String getPassword() {
        return password;
    }

    public void processOperationVS(String password, OperationVS operationVS) {
        signatureService.processOperationVS(password, operationVS);
    }

    private void checkPasswords() {
        log.debug("checkPasswords");
        PlatformImpl.runLater(new Runnable(){
            @Override public void run() {
                String password1 = new String(password1Field.getText());
                String password2 = new String(password2Field.getText());
                if(password1.trim().isEmpty() && password2.trim().isEmpty()) setMessage(ContextVS.getMessage("passwordMissing"));
                else {
                    if (password1.equals(password2)) {
                        password = password1;
                        setPasswordDialogVisible(false);
                        signatureService.processOperationVS(password, operationVS);
                    } else {
                        setMessage(ContextVS.getMessage("passwordError"));
                    }
                    password1Field.setText("");
                    password2Field.setText("");
                }
            }
        });
    }

    private void setMessage (String message) {
        if (message == null) messageText.setText(mainMessage);
        else messageText.setText(message);
        if(isCapsLockPressed) {
            if(!passwordVBox.getChildren().contains(capsLockPressedMessageText))
                passwordVBox.getChildren().add(0, capsLockPressedMessageText);
        }
        else passwordVBox.getChildren().removeAll(capsLockPressedMessageText);
        passwordVBox.autosize();
    }

}
