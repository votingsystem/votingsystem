package org.votingsystem.client.pane;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.client.model.SignedFile;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.DateUtils;

import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignedFilePane extends GridPane {

    private static Logger log = Logger.getLogger(SignedFilePane.class);

    private SignedFile signedFile;
    private WebView signatureContentWebView;
    private String receiptViewerURL;

    public SignedFilePane(final SignedFile signedFile) {
        super();
        this.signedFile = signedFile;
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(10, 10, 10, 10));
        Button openSignatureInfoButton = new Button(ContextVS.getMessage("openSignedFileButtonLbl"));
        openSignatureInfoButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                SMIMESignersPane.showDialog(signedFile);
            }});
        openSignatureInfoButton.setPrefWidth(200);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        getColumnConstraints().addAll(column1, column2);
        setHalignment(openSignatureInfoButton, HPos.RIGHT);
        signatureContentWebView = new WebView();
        signatureContentWebView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
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
            contentStr = Formatter.format(contentJSON);
        }  catch(Exception ex) {
            contentStr = signedFile.getSMIMEMessageWraper().getSignedContent();
        }
        signatureContentWebView.getEngine().loadContent(contentStr);
        signatureContentWebView.setPrefHeight(600);

        TimeStampToken timeStampToken = signedFile.getSMIMEMessageWraper().getTimeStampToken();

        Button timeStampButton = new Button(ContextVS.getMessage("timeStampButtonLbl"));
        timeStampButton.setGraphic((new ImageView(Utils.getImage(this, "clock"))));
        timeStampButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                TimeStampPane.showDialog(timeStampToken);
            }
        });
        setMargin(timeStampButton, new Insets(10, 0, 10, 0));



        VBox.setVgrow(signatureContentWebView, Priority.ALWAYS);
        add(timeStampButton, 0, 0);
        add(openSignatureInfoButton, 1, 0);
        add(signatureContentWebView, 0, 1);
        setColumnSpan(signatureContentWebView, 2);
        //add(contentWithoutFormatCheckBox, 0, 2);
        JSONObject signedContentJSON = null;
        if(signedFile.getSMIMEMessageWraper().getContentTypeVS() == ContentTypeVS.ASCIIDOC) {
            signedContentJSON =  (JSONObject) JSONSerializer.toJSON(signedFile.getOperationDocument());
        } else signedContentJSON =  (JSONObject)JSONSerializer.toJSON(signedFile.getSMIMEMessageWraper().getSignedContent());
        String timeStampDateStr = "";
        if(signedFile.getSMIMEMessageWraper().getTimeStampToken() != null) {
            timeStampDateStr = DateUtils.getDateStr(signedFile.getSMIMEMessageWraper().
                    getTimeStampToken().getTimeStampInfo().getGenTime(),"dd/MMM/yyyy HH:mm");
        }
        try {
            JSONObject signedContent = (JSONObject) JSONSerializer.toJSON(signedFile.getSMIMEMessageWraper().getSignedContent());
            signatureContentWebView.getEngine().loadContent(signedContent.toString(3), "application/json");
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void initComponents() {
        if(signedFile == null) {
            log.debug("### NULL signedFile");
            return;
        }
    }

    public SignedFile getSignedFile () {
        return signedFile;
    }


}
