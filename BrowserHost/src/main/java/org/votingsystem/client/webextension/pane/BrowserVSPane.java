package org.votingsystem.client.webextension.pane;

import com.sun.javafx.application.PlatformImpl;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.service.EventBusService;
import org.votingsystem.client.webextension.service.OperationService;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSPane extends StackPane {

    private static Logger log = Logger.getLogger(BrowserVSPane.class.getSimpleName());

    private Text messageText;
    private boolean isCapsLockPressed = false;
    private Text capsLockPressedMessageText;
    private char[] password;
    private PasswordField password1Field;
    private PasswordField password2Field;
    private Text password2Text;
    private VBox passwordVBox;
    private Region passwordRegion;
    private String mainMessage = null;
    private final OperationService operationService;
    private OperationVS operationVS;
    private boolean isWithPasswordConfirm = true;

    public BrowserVSPane() {
        this.operationService = new OperationService();
        getStylesheets().add(Utils.getResource("/css/browservsPane.css"));
        getStyleClass().add("glassBox");

        operationService.setOnRunning(event -> log.info("operationService - OnRunning"));
        operationService.setOnCancelled(event -> log.info("operationService - OnCancelled"));
        operationService.setOnFailed(event -> log.info("operationService - OnFailed"));
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
        progressMessageText.textProperty().bind(operationService.messageProperty());
        progressBar.progressProperty().bind(operationService.progressProperty());
        progressRegion.visibleProperty().bind(operationService.runningProperty());
        progressBox.visibleProperty().bind(operationService.runningProperty());
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
        cancelButton.setOnAction(event -> {
            cancel();
            setPasswordDialogVisible(false, null);
        });
        cancelButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        final Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(event -> checkPasswords());
        acceptButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
        password1Field.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if ((event.getCode() == KeyCode.ENTER)) acceptButton.fire();
            setCapsLockState(java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(
                    java.awt.event.KeyEvent.VK_CAPS_LOCK));
        });
        password2Field.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if ((event.getCode() == KeyCode.ENTER)) acceptButton.fire();
            setCapsLockState(java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(
                    java.awt.event.KeyEvent.VK_CAPS_LOCK));
        });
        HBox footerButtonsBox = new HBox();
        footerButtonsBox.getChildren().addAll(cancelButton, Utils.getSpacer(), acceptButton);
        VBox.setMargin(footerButtonsBox, new Insets(20, 20, 10, 20));
        Text password1Text = new Text(ContextVS.getMessage("password1Lbl"));
        password2Text = new Text(ContextVS.getMessage("password2Lbl"));
        password2Text.setStyle("-fx-spacing: 50;");
        passwordVBox.getChildren().addAll(messageText, password1Text, password1Field, password2Text, password2Field,
                footerButtonsBox);
        passwordVBox.getStyleClass().add("password-dialog");
        passwordVBox.autosize();
        setPasswordDialogVisible(false, null);
        getChildren().addAll(progressRegion, progressBox, passwordRegion, passwordVBox);
    }

    public OperationService getOperationService() {
        return this.operationService;
    }

    public void processOperationVS(OperationVS operationVS, String passwordDialogMessage) {
        this.operationVS = operationVS;
        if(CryptoTokenVS.MOBILE != BrowserSessionService.getCryptoTokenType()) {
            PlatformImpl.runAndWait(() -> setPasswordDialogVisible(true, passwordDialogMessage));
        } else operationService.processOperationVS(null, operationVS);
    }

    private void cancel() {
        ResponseVS responseVS = new ResponseVS();
        responseVS.setType(TypeVS.CANCELED);
        responseVS.setData(operationVS);
        EventBusService.getInstance().post(responseVS);
    }

    public void setPasswordDialogVisible(boolean isVisible, String message) {
        isWithPasswordConfirm = false;
        setPasswordDialogVisible(isVisible, message, isWithPasswordConfirm);
    }

    public void setPasswordDialogVisible(boolean isVisible, String message, boolean isWithPasswordConfirm) {
        this.isWithPasswordConfirm = isWithPasswordConfirm;
        if(message == null) mainMessage = ContextVS.getMessage("passwordMissing");
        else mainMessage = message;
        setMessage(mainMessage);
        password1Field.setText("");
        password2Field.setText("");
        if(!isWithPasswordConfirm) {
            if(passwordVBox.getChildren().contains(password2Text)) passwordVBox.getChildren().remove(password2Text);
            if(passwordVBox.getChildren().contains(password2Field)) passwordVBox.getChildren().remove(password2Field);
        } else {
            if(!passwordVBox.getChildren().contains(password2Text)) {
                passwordVBox.getChildren().add(3, password2Text);
                passwordVBox.getChildren().add(4, password2Field);
            }
        }
        passwordVBox.setVisible(isVisible);
        passwordRegion.setVisible(isVisible);
    }

    private void setCapsLockState (boolean pressed) {
        this.isCapsLockPressed = pressed;
        setMessage(mainMessage);
    }

    public char[] getPassword() {
        return password;
    }

    public void processOperationVS(char[] password, OperationVS operationVS) {
        operationService.processOperationVS(password, operationVS);
    }

    private void checkPasswords() {
        log.info("checkPasswords");
        PlatformImpl.runLater(() -> {
            String password1 = new String(password1Field.getText());
            String password2 = null;
            if(isWithPasswordConfirm)  password2 = new String(password2Field.getText());
            else password2 = password1;
            if(password1.trim().isEmpty() && password2.trim().isEmpty()) setMessage(ContextVS.getMessage("passwordMissing"));
            else {
                if (password1.equals(password2)) {
                    password = password1.toCharArray();
                    setPasswordDialogVisible(false, null);
                    operationService.processOperationVS(password, operationVS);
                } else setMessage(ContextVS.getMessage("passwordError"));
                password1Field.setText("");
                password2Field.setText("");
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