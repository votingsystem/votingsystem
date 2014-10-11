package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DocumentSignerPane extends GridPane implements DocumentSignerStackPane.OperationListener {

    private static Logger log = Logger.getLogger(DocumentSignerPane.class);

    private Stage stage;
    private TextArea textArea;
    private Button signButton;
    private SMIMEMessage smimeMessage;
    private HBox signedDocumentButtonsBox;
    private TextField serviceURLTextField;
    private TextField messageSubjectTextField;
    private TextField toUserTextField;
    private DocumentSignerStackPane documentSignerHelper;

    public DocumentSignerPane() {
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
        signButton.setGraphic((new ImageView(Utils.getImage(this, "pencil"))));
        signButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                documentSignerHelper.processOperation(DocumentSignerStackPane.Operation.SIGN_SMIME, toUserTextField.getText(),
                        textArea.getText(), messageSubjectTextField.getText(), smimeMessage, null);
            }
        });

        Button saveButton = new Button(ContextVS.getMessage("saveLbl"));
        saveButton.setGraphic((new ImageView(Utils.getImage(this, "save_data"))));
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                saveMessage(smimeMessage);
            }
        });
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
        sendButton.setGraphic((new ImageView(Utils.getImage(this, "upload"))));
        sendButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                sendDocumentToService();
            }
        });
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
        cancelButton.setGraphic((new ImageView(Utils.getImage(this, "cancel"))));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                DocumentSignerPane.this.getScene().getWindow().hide();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonsBox.getChildren().addAll(signButton, spacer, cancelButton);
        setMargin(buttonsBox, new Insets(20, 20, 0, 20));
        add(buttonsBox, 0, 4);
        textArea.requestFocus();

        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        //stage.initOwner(window);

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent window) {
            }
        });
        documentSignerHelper = new DocumentSignerStackPane(this);
        documentSignerHelper.getChildren().add(0, this);
        stage.setScene(new Scene(documentSignerHelper, javafx.scene.paint.Color.TRANSPARENT));
        stage.setTitle(ContextVS.getMessage("documentSignerDialogCaption"));
    }

    private void showMessage(final int statusCode, final String message) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                MessageDialog messageDialog = new MessageDialog();
                messageDialog.showMessage(statusCode, message);
            }
        });

    }

    private void sendDocumentToService() {
        if("".equals(serviceURLTextField.getText())) {
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterServiceURLErrorMsg"));
            return;
        }
        if(smimeMessage == null) {
            showMessage(ResponseVS.SC_ERROR, "Missing signed document");
        } else {
            String targetURL = null;
            if(serviceURLTextField.getText().startsWith("http://") || serviceURLTextField.getText().startsWith("https://")) {
                targetURL = serviceURLTextField.getText().trim();
            } else targetURL = "http://" + serviceURLTextField.getText().trim();
            documentSignerHelper.processOperation(DocumentSignerStackPane.Operation.SEND_SMIME, null, null,
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
            log.error(ex.getMessage(), ex);
        }
    }

    private void show() {
        stage.centerOnScreen();
        stage.show();
    }

    public static void showDialog() {
        log.debug("showDialog");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                DocumentSignerPane documentSignerPane = new DocumentSignerPane();
                documentSignerPane.show();
            }
        });
    }


    @Override public void processResult(DocumentSignerStackPane.Operation operation, ResponseVS responseVS) {
        log.debug("processResult - operation: " + operation + " - result: " + responseVS.getStatusCode());
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
                    smimeMessage = responseVS.getSmimeMessage();
                } else showMessage(responseVS.getStatusCode(), responseVS.getMessage());
                break;
            case SEND_SMIME:
                showMessage(responseVS.getStatusCode(), responseVS.getMessage());
                break;
        }
    }
}
