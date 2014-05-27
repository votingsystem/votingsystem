package org.votingsystem.client.pane;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.model.SignedFile;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;



/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SignedFilePane extends GridPane {

    private static Logger logger = Logger.getLogger(SignedFilePane.class);

    private SignedFile signedFile;
    private WebView signatureContentWebView;
    private CheckBox contentFormattedCheckBox = null;

    public SignedFilePane(final SignedFile signedFile) {
        super();
        this.signedFile = signedFile;
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(10, 10, 10, 10));
        Button openSignatureInfoButton = new Button(ContextVS.getMessage("openSignedFileButtonLbl"));
        openSignatureInfoButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                DocumentSignersPane.showDialog(signedFile);
            }});
        openSignatureInfoButton.setPrefWidth(200);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        getColumnConstraints().addAll(column1, column2);
        setHalignment(openSignatureInfoButton, HPos.RIGHT);
        if(signedFile.isPDF()) {
            Button openPDFButton = new Button(ContextVS.getMessage("openPDFLbl"));
            openPDFButton.setGraphic(new ImageView(Utils.getImage(this, "pdf")));
            openPDFButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent actionEvent) {
                    openPDFDocument();
                }
            });
            add(openPDFButton, 0, 0);
            if (signedFile.isValidSignature()) {
                openSignatureInfoButton.setText(ContextVS.getMessage("signatureOKLbl"));
                openSignatureInfoButton.setGraphic(new ImageView(Utils.getImage(this, "accept")));
            } else {
                openSignatureInfoButton.setText(ContextVS.getMessage("signatureERRORLbl"));
                openSignatureInfoButton.setGraphic(new ImageView(Utils.getImage(this, "cancel")));
                openSignatureInfoButton.setDisable(true);
            }
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            add(spacer, 0, 0);
            add(openSignatureInfoButton, 1, 0);
        } else {
            signatureContentWebView = new WebView();
            contentFormattedCheckBox = new CheckBox(ContextVS.getMessage("formattedCheckBoxLbl"));
            contentFormattedCheckBox.setSelected(true);
            contentFormattedCheckBox.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent actionEvent) {
                    changeContentFormat();
                }});
            if (signedFile.isValidSignature()) {
                openSignatureInfoButton.setGraphic(new ImageView(Utils.getImage(this, "accept")));
                openSignatureInfoButton.setText(ContextVS.getMessage("signatureOKLbl"));
            } else {
                openSignatureInfoButton.setGraphic(new ImageView(Utils.getImage(this, "cancel")));
                openSignatureInfoButton.setText(ContextVS.getMessage("signatureERRORLbl"));
            }
            String contentStr = null;
            try {
                JSONObject contentJSON = (JSONObject) JSONSerializer.toJSON(
                        signedFile.getSMIMEMessageWraper().getSignedContent());
                contentStr = Formatter.format(contentJSON.toString(5));
            }  catch(Exception ex) {
                contentStr = signedFile.getSMIMEMessageWraper().getSignedContent();
            }
            signatureContentWebView.getEngine().loadContent(contentStr);
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setContent(signatureContentWebView);
            scrollPane.setPrefHeight(600);

            VBox.setVgrow(signatureContentWebView, Priority.ALWAYS);
            add(openSignatureInfoButton, 1, 0);
            add(signatureContentWebView, 0, 1);
            setColumnSpan(signatureContentWebView, 2);
            add(contentFormattedCheckBox, 0, 2);
        }
    }

    private void initComponents() {
        if(signedFile == null) {
            logger.debug("### NULL signedFile");
            return;
        }
    }


    private void openPDFDocument() {
        try {
            VotingSystemApp.getInstance().getHostServices().showDocument(signedFile.getFile().getAbsolutePath());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void changeContentFormat() {
        logger.debug("changeContentFormat: " + contentFormattedCheckBox.isSelected());
        if (contentFormattedCheckBox.isSelected()) {
            try {
                String formattedText = Formatter.format(signedFile.getSMIMEMessageWraper().getSignedContent());
                signatureContentWebView.getEngine().loadContent(formattedText);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else signatureContentWebView.getEngine().loadContent(signedFile.getSMIMEMessageWraper().getSignedContent());
    }
}
