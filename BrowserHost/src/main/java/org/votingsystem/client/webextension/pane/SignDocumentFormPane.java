package org.votingsystem.client.webextension.pane;

import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.dialog.DialogVS;
import org.votingsystem.client.webextension.dialog.PasswordDialog;
import org.votingsystem.client.webextension.dialog.ProgressDialog;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignDocumentFormPane extends GridPane {

    private static Logger log = Logger.getLogger(SignDocumentFormPane.class.getName());

    public enum Operation {SEND_SMIME, SIGN_SMIME}

    private TextArea textArea;
    private Button signButton;
    private SMIMEMessage smimeMessage;
    private HBox signedDocumentButtonsBox;
    private TextField serviceURLTextField;
    private TextField messageSubjectTextField;
    private TextField toUserTextField;

    public SignDocumentFormPane() {
        setPadding(new Insets(10, 10 , 10, 10));
        Label messageLbl = new Label(ContextVS.getMessage("enterSignatureContentMsg") + ":");
        messageLbl.setStyle("");
        add(messageLbl, 0, 2);
        textArea = new TextArea();
        textArea.setPrefHeight(500);
        textArea.setPrefWidth(700);
        textArea.setWrapText(true);
        add(textArea, 0, 3);

        GridPane.setHgrow(textArea, Priority.ALWAYS);
        GridPane.setVgrow(textArea, Priority.ALWAYS);

        signButton = new Button(ContextVS.getMessage("signLbl"));
        signButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.PENCIL)));
        signButton.setOnAction(actionEvent -> {
            PasswordDialog.showWithoutPasswordConfirm(password -> {
                    if(password == null) return;
                    ProgressDialog.show(new OperationHandlerTask(Operation.SIGN_SMIME, password, null), null);
                }, null);
        });
        Button saveButton = new Button(ContextVS.getMessage("saveLbl"));
        saveButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.SAVE)));
        saveButton.setOnAction(actionEvent -> saveMessage(smimeMessage));
        HBox.setMargin(saveButton, new Insets(0, 40, 0, 10));

        serviceURLTextField = new TextField();
        serviceURLTextField.setPrefWidth(300);
        serviceURLTextField.addEventHandler(KeyEvent.KEY_PRESSED,
                new EventHandler<KeyEvent>() {
                    public void handle(KeyEvent event) {
                        if (event.getCode().equals(KeyCode.ENTER)) {
                            sendDocumentToService();
                        }
                    }
                }
        );
        serviceURLTextField.setPromptText(ContextVS.getMessage("serviceURLbl"));
        HBox.setMargin(serviceURLTextField, new Insets(0, 0, 0, 30));
        HBox.setHgrow(serviceURLTextField, Priority.ALWAYS);
        Button sendButton = new Button(ContextVS.getMessage("sendLbl"));
        sendButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.UPLOAD)));
        sendButton.setOnAction(actionEvent -> sendDocumentToService());
        HBox.setMargin(sendButton, new Insets(0, 0, 0, 30));

        messageSubjectTextField = new TextField();
        messageSubjectTextField.setPrefWidth(400);
        messageSubjectTextField.setPromptText(ContextVS.getMessage("messageSubjectLbl"));
        HBox.setMargin(serviceURLTextField, new Insets(0, 10, 0, 0));

        toUserTextField = new TextField();
        toUserTextField.setPrefWidth(200);
        toUserTextField.setPromptText(ContextVS.getMessage("toUserLbl"));
        HBox.setMargin(toUserTextField, new Insets(0, 10, 0, 0));
        HBox.setHgrow(toUserTextField, Priority.ALWAYS);

        HBox messageFieldsBox = new HBox(10);
        messageFieldsBox.getChildren().addAll(messageSubjectTextField, toUserTextField);
        setMargin(messageFieldsBox, new Insets(0, 10, 0, 0));
        add(messageFieldsBox, 0,1);

        signedDocumentButtonsBox = new HBox(10);
        signedDocumentButtonsBox.setAlignment(Pos.CENTER_LEFT);
        signedDocumentButtonsBox.getChildren().addAll(saveButton, serviceURLTextField, sendButton);
        setMargin(signedDocumentButtonsBox, new Insets(10, 0, 10, 0));

        HBox buttonsBox = new HBox();
        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK)));
        cancelButton.setOnAction(actionEvent -> SignDocumentFormPane.this.getScene().getWindow().hide());
        buttonsBox.getChildren().addAll(signButton, Utils.getSpacer(), cancelButton);
        setMargin(buttonsBox, new Insets(20, 20, 0, 20));
        add(buttonsBox, 0, 4);
        textArea.requestFocus();
    }

    private void sendDocumentToService() {
        if("".equals(serviceURLTextField.getText())) {
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterServiceURLErrorMsg"));
            return;
        }
        if(smimeMessage == null) {
            BrowserHost.showMessage(ResponseVS.SC_ERROR, "Missing signed document");
        } else {
            final StringBuilder serviceURL = new StringBuilder("");
            if(serviceURLTextField.getText().startsWith("http://") || serviceURLTextField.getText().startsWith("https://")) {
                serviceURL.append(serviceURLTextField.getText().trim());
            } else serviceURL.append("http://" + serviceURLTextField.getText().trim());
            PasswordDialog.showWithoutPasswordConfirm(password -> {
                if(password == null) return;
                ProgressDialog.show(new OperationHandlerTask(Operation.SEND_SMIME, password, serviceURL.toString()), null);
            }, null);
        }
    }

    public void saveMessage (SMIMEMessage smimeMessage) {
        try {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showSaveDialog(getScene().getWindow());
            String fileName = file.getAbsolutePath();
            if(!fileName.endsWith(ContentTypeVS.SIGNED.getExtension())) fileName = fileName + ContentTypeVS.SIGNED.getExtension();
            FileOutputStream fos = new FileOutputStream(new File(fileName));
            fos.write(smimeMessage.getBytes());
            fos.close();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static void showDialog() {
        Platform.runLater(() -> {
            SignDocumentFormPane signersPane = new SignDocumentFormPane();
            new DialogVS(signersPane).setCaption(ContextVS.getMessage("signDocumentButtonLbl")).show();
        });
    }


    public class OperationHandlerTask extends Task<ResponseVS> {

        private Operation operation;
        private String serviceURL;
        private String toUser;
        private String messageSubject;
        private String textToSign;
        private char[] password;

        OperationHandlerTask(Operation operation, char[] password, String serviceURL) {
            this.operation = operation;
            this.serviceURL = serviceURL;
            this.password = password;
            this.toUser = toUserTextField.getText();
            this.messageSubject = messageSubjectTextField.getText();
            this.textToSign = textArea.getText();
        }

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
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                        responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                    break;
                case SIGN_SMIME:
                    try {
                        Map<String, Object> textToSignMap = null;
                        try {
                            textToSignMap = JSON.getMapper().readValue(
                                    textToSign.replaceAll("(\\r|\\n)", "\\\\n"), new TypeReference<HashMap<String, Object>>() {});
                        } catch (Exception ex) {
                            textToSignMap = new HashMap<>();
                            textToSignMap.put("message", textToSign);
                        }
                        textToSignMap.put("UUID", UUID.randomUUID().toString());
                        toUser = StringUtils.getNormalized(toUser);
                        String timeStampService = ActorVS.getTimeStampServiceURL(ContextVS.getMessage("defaultTimeStampServer"));
                        log.info("toUser: " + toUser + " - timeStampService: " + timeStampService);
                        smimeMessage = BrowserSessionService.getSMIME(null, toUser,
                                textToSignMap.toString(), password, messageSubject);
                        updateMessage(ContextVS.getMessage("gettingTimeStampMsg"));
                        updateProgress(40, 100);
                        MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampService);
                        smimeMessage = timeStamper.call();
                        responseVS = ResponseVS.OK(null).setSMIME(smimeMessage);
                    } catch(Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage() + " - " + textToSign.replaceAll("(\\r|\\n)", "\\\\n"), ex);
                        responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                    break;
            }
            processResult(operation, responseVS);
            return responseVS;
        }

    }

    public void processResult(Operation operation, ResponseVS responseVS) {
        log.info("processResult - operation: " + operation + " - result: " + responseVS.getStatusCode());
        switch(operation) {
            case SIGN_SMIME:
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    if(!getChildren().contains(signedDocumentButtonsBox)) {
                        Platform.runLater(new Runnable() {
                            @Override public void run() {
                                add(signedDocumentButtonsBox, 0, 0);
                            }
                        });
                    }
                    try {
                        smimeMessage = responseVS.getSMIME();
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                } else BrowserHost.showMessage(responseVS.getStatusCode(), responseVS.getMessage());
                break;
            case SEND_SMIME:
                BrowserHost.showMessage(responseVS.getStatusCode(), responseVS.getMessage());
                break;
        }
    }

}
