package org.votingsystem.client.pane;

import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.util.SignatureService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.ContentSignerHelper;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;

import java.util.UUID;


/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class DocumentSignerStackPane extends StackPane {

    private static Logger logger = Logger.getLogger(DocumentSignerStackPane.class);

    public enum Operation {SEND_SMIME, SIGN_SMIME}

    public interface OperationListener {
        public void processResult(Operation operation, ResponseVS responseVS);
    }

    private Text messageText;
    private boolean isCapsLockPressed = false;
    private Label capsLockPressedMessageLabel;
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
    private SMIMEMessageWrapper smimeMessage;
    private ProgressBar progressBar;
    private Region progressRegion;
    private VBox progressBox;
    private OperationListener operationListener;
    private Task<ResponseVS> operationHandlerTask;

    public DocumentSignerStackPane(OperationListener operationListener) {
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

        Text progressMessageText = new Text();
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

        capsLockPressedMessageLabel = new Label(ContextVS.getMessage("capsLockKeyPressed"));
        capsLockPressedMessageLabel.setWrapText(true);
        capsLockPressedMessageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6c0404;");

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

        operationHandlerTask = new OperationHandlerTask();
        progressBar.progressProperty().bind(operationHandlerTask.progressProperty());
        progressRegion.visibleProperty().bind(operationHandlerTask.runningProperty());
        progressBox.visibleProperty().bind(operationHandlerTask.runningProperty());
    }

    public void processOperation(Operation operation, String toUser, String textToSign, String subject,
                                 SMIMEMessageWrapper smimeMessage, String serviceURL) {
        this.operation = operation;
        this.smimeMessage = smimeMessage;
        this.toUser = toUser;
        this.messageSubject = subject;
        this.serviceURL = serviceURL;
        this.textToSign = textToSign;
        if(operation == Operation.SEND_SMIME) {
            operationHandlerTask = new OperationHandlerTask();
            progressBar.progressProperty().bind(operationHandlerTask.progressProperty());
            progressRegion.visibleProperty().bind(operationHandlerTask.runningProperty());
            progressBox.visibleProperty().bind(operationHandlerTask.runningProperty());
            new Thread(operationHandlerTask).start();
        } else {
            PlatformImpl.runAndWait(new Runnable() {
                @Override public void run() {
                    setPasswordDialogVisible(true);
                }
            });
        }
    }

    private void showMessage(int statusCode, String message) {
        MessageDialog messageDialog = new MessageDialog();
        messageDialog.showMessage(statusCode, message);
    }

    public void setPasswordDialogVisible(boolean isVisible) {
        passwordVBox.setVisible(isVisible);
        passwordRegion.setVisible(isVisible);
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
        PlatformImpl.runLater(new Runnable(){
            @Override public void run() {
                String password1 = new String(password1Field.getText());
                String password2 = new String(password2Field.getText());
                if(password1.trim().isEmpty() && password2.trim().isEmpty()) setMessage(ContextVS.getMessage("passwordMissing"));
                else {
                    if (password1.equals(password2)) {
                        password = password1;
                        setPasswordDialogVisible(false);
                        new Thread(operationHandlerTask).start();
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
            if(!passwordVBox.getChildren().contains(capsLockPressedMessageLabel))
                passwordVBox.getChildren().add(0, capsLockPressedMessageLabel);
        }
        else passwordVBox.getChildren().removeAll(capsLockPressedMessageLabel);
        passwordVBox.autosize();
    }


    public class OperationHandlerTask extends Task<ResponseVS> {

        OperationHandlerTask() { }

        @Override protected ResponseVS call() throws Exception {
            ResponseVS responseVS = null;
            switch(operation) {
                case SEND_SMIME:
                    try {
                        responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                                serviceURL);
                        showMessage(responseVS.getStatusCode(), responseVS.getMessage());
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                    break;
                case SIGN_SMIME:
                    try {
                        JSONObject textToSignJSON = (JSONObject) JSONSerializer.toJSON(textToSign.replaceAll("(\\r|\\n)", "\\\\n"));
                        textToSignJSON.put("UUID", UUID.randomUUID().toString());
                        toUser = StringUtils.getNormalized(toUser);
                        String timeStampService = ActorVS.getTimeStampServiceURL(ContextVS.getMessage("defaultTimeStampServer"));
                        logger.debug("toUser: " + toUser + " - timeStampService: " + timeStampService);
                        smimeMessage = ContentSignerHelper.genMimeMessage(null, toUser,
                                textToSignJSON.toString(), password.toCharArray(), messageSubject, null);
                        MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampService);
                        responseVS = timeStamper.call();
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                    break;
            }
            if(operationListener != null) operationListener.processResult(operation, responseVS);
            return responseVS;
        }

    }
}
