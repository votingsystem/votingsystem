package org.votingsystem.client.webextension.pane;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;

import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignDocumentFormPane extends GridPane implements SignDocumentFormStackPane.OperationListener {

    private static Logger log = Logger.getLogger(SignDocumentFormPane.class.getSimpleName());

    private Stage stage;
    private TextArea textArea;
    private Button signButton;
    private SMIMEMessage smimeMessage;
    private HBox signedDocumentButtonsBox;
    private TextField serviceURLTextField;
    private TextField messageSubjectTextField;
    private TextField toUserTextField;
    private SignDocumentFormStackPane documentSignerHelper;

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
        signButton.setOnAction(actionEvent -> documentSignerHelper.processOperation(
                SignDocumentFormStackPane.Operation.SIGN_SMIME, toUserTextField.getText(),
                textArea.getText(), messageSubjectTextField.getText(), smimeMessage, null));
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
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        //stage.initOwner(window);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
        documentSignerHelper = new SignDocumentFormStackPane(this);
        documentSignerHelper.getChildren().add(0, this);
        stage.setScene(new Scene(documentSignerHelper, javafx.scene.paint.Color.TRANSPARENT));
        stage.setTitle(ContextVS.getMessage("signDocumentButtonLbl"));
    }

    private void sendDocumentToService() {
        if("".equals(serviceURLTextField.getText())) {
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterServiceURLErrorMsg"));
            return;
        }
        if(smimeMessage == null) {
            BrowserHost.showMessage(ResponseVS.SC_ERROR, "Missing signed document");
        } else {
            String targetURL = null;
            if(serviceURLTextField.getText().startsWith("http://") || serviceURLTextField.getText().startsWith("https://")) {
                targetURL = serviceURLTextField.getText().trim();
            } else targetURL = "http://" + serviceURLTextField.getText().trim();
            documentSignerHelper.processOperation(SignDocumentFormStackPane.Operation.SEND_SMIME, null, null,
                    messageSubjectTextField.getText(), smimeMessage, targetURL);
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

    private void show() {
        stage.centerOnScreen();
        stage.show();
    }

    public static void showDialog() {
        log.info("validateBackup");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                SignDocumentFormPane signDocumentFormPane = new SignDocumentFormPane();
                signDocumentFormPane.show();
            }
        });
    }


    @Override public void processResult(SignDocumentFormStackPane.Operation operation, ResponseVS responseVS) {
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
