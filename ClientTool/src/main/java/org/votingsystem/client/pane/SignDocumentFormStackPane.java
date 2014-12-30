package org.votingsystem.client.pane;

import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;

import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignDocumentFormStackPane extends StackPane {

    private static Logger log = Logger.getLogger(SignDocumentFormStackPane.class);

    public enum Operation {SEND_SMIME, SIGN_SMIME}

    public interface OperationListener {
        public void processResult(Operation operation, ResponseVS responseVS);
    }

    private Text messageText;
    private boolean isCapsLockPressed = false;
    private Text capsLockPressedMessageText;
    private String password;
    private PasswordField password1Field;
    private PasswordField password2Field;
    private VBox passwordVBox;
    private Region passwordRegion;
    private String mainMessage = null;
    private Operation operation;
    private String serviceURL;
    private String toUser;
    private String textToSign;
    private String messageSubject;
    private SMIMEMessage smimeMessage;
    private ProgressBar progressBar;
    private Region progressRegion;
    private VBox progressBox;
    private OperationListener operationListener;
    private Text progressMessageText;

    public SignDocumentFormStackPane(OperationListener operationListener) {
        this.operationListener = operationListener;
        progressRegion = new Region();
        progressRegion.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        progressRegion.setPrefSize(240, 160);

        passwordRegion = new Region();
        passwordRegion.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        passwordRegion.setPrefSize(240, 160);

        progressBox = new VBox();
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPrefWidth(400);
        progressBox.setPrefHeight(300);

        progressMessageText = new Text();
        progressMessageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #f9f9f9;");
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(10);
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
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));

        final Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                checkPasswords();
            }});
        acceptButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));

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
        passwordVBox.getStylesheets().add(Utils.getResource("/css/documentSignerPasswordDialog.css"));
        passwordVBox.getStyleClass().add("message-lbl-bold");
        passwordVBox.getStyleClass().add("modal-dialog");
        passwordVBox.setStyle("-fx-background-color: #f9f9f9; -fx-max-height:280px;-fx-max-width:350px;");
        passwordVBox.autosize();
        setPasswordDialogVisible(false);
        getChildren().addAll(progressRegion, progressBox, passwordRegion, passwordVBox);

        Task<ResponseVS> operationHandlerTask = new OperationHandlerTask();
        progressMessageText.textProperty().bind(operationHandlerTask.messageProperty());
        progressBar.progressProperty().bind(operationHandlerTask.progressProperty());
        progressRegion.visibleProperty().bind(operationHandlerTask.runningProperty());
        progressBox.visibleProperty().bind(operationHandlerTask.runningProperty());
    }

    public void processOperation(Operation operation, String toUser, String textToSign, String subject,
             SMIMEMessage smimeMessage, String serviceURL) {
        this.operation = operation;
        this.smimeMessage = smimeMessage;
        this.toUser = toUser;
        this.messageSubject = subject;
        this.serviceURL = serviceURL;
        this.textToSign = textToSign;
        if(operation == Operation.SEND_SMIME) initBackgroundTask();
        else PlatformImpl.runAndWait(new Runnable() {
                @Override public void run() {
                    setPasswordDialogVisible(true);
                }
            });
    }

    private void initBackgroundTask() {
        log.debug("processOperation");
        Task<ResponseVS> operationHandlerTask = new OperationHandlerTask();
        progressMessageText.textProperty().bind(operationHandlerTask.messageProperty());
        progressBar.progressProperty().bind(operationHandlerTask.progressProperty());
        progressRegion.visibleProperty().bind(operationHandlerTask.runningProperty());
        progressBox.visibleProperty().bind(operationHandlerTask.runningProperty());
        new Thread(operationHandlerTask).start();
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
                        initBackgroundTask();
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

    public class OperationHandlerTask extends Task<ResponseVS> {

        OperationHandlerTask() { }

        @Override protected ResponseVS call() throws Exception {
            ResponseVS responseVS = null;
            switch(operation) {
                case SEND_SMIME:
                    try {
                        updateMessage(ContextVS.getMessage("sendingDocumentMsg"));
                        updateProgress(20, 100);
                        responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                                serviceURL);
                        updateProgress(80, 100);
                    } catch(Exception ex) {
                        log.error(ex.getMessage(), ex);
                        responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                    break;
                case SIGN_SMIME:
                    try {
                        JSONObject textToSignJSON = (JSONObject) JSONSerializer.toJSON(textToSign.replaceAll("(\\r|\\n)", "\\\\n"));
                        textToSignJSON.put("UUID", UUID.randomUUID().toString());
                        toUser = StringUtils.getNormalized(toUser);
                        String timeStampService = ActorVS.getTimeStampServiceURL(ContextVS.getMessage("defaultTimeStampServer"));
                        log.debug("toUser: " + toUser + " - timeStampService: " + timeStampService);
                        smimeMessage = SessionService.getSMIME(null, toUser,
                                textToSignJSON.toString(), password, messageSubject, null);
                        updateMessage(ContextVS.getMessage("gettingTimeStampMsg"));
                        updateProgress(40, 100);
                        MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampService);
                        responseVS = timeStamper.call();
                    } catch(Exception ex) {
                        log.error(ex.getMessage() + " - " + textToSign.replaceAll("(\\r|\\n)", "\\\\n"), ex);
                        responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                    break;
            }
            if(operationListener != null) operationListener.processResult(operation, responseVS);
            return responseVS;
        }

    }
}
