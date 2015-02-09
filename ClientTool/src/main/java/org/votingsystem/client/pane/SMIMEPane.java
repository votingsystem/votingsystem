package org.votingsystem.client.pane;

import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.tsp.TimeStampToken;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.*;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.SignedFile;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;
import java.io.File;
import java.security.cert.X509Certificate;
import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMEPane extends GridPane implements DocumentVS {

    private static Logger log = Logger.getLogger(SMIMEPane.class);

    private SignedFile signedFile;
    public SMIMEPane(final SignedFile signedFile) {
        super();
        this.signedFile = signedFile;
        setHgap(3);
        setVgap(3);
        setPadding(new Insets(5, 10, 5, 10));
        Button openSignatureInfoButton = new Button(ContextVS.getMessage("openSignedFileButtonLbl"));
        openSignatureInfoButton.setOnAction(actionEvent -> SMIMESignersPane.showDialog(signedFile));
        openSignatureInfoButton.setPrefWidth(150);
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        getColumnConstraints().addAll(column1, column2);
        setHalignment(openSignatureInfoButton, HPos.RIGHT);
        WebView signatureContentWebView = new WebView();
        signatureContentWebView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        if (signedFile.isValidSignature()) {
            openSignatureInfoButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));
            openSignatureInfoButton.setText(ContextVS.getMessage("signatureOKLbl"));
        } else {
            openSignatureInfoButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
            openSignatureInfoButton.setText(ContextVS.getMessage("signatureERRORLbl"));
        }
        String contentStr = null;
        try {
            JSONObject contentJSON = (JSONObject) JSONSerializer.toJSON(
                    signedFile.getSMIME().getSignedContent());
            contentStr = Formatter.format(contentJSON);
        }  catch(Exception ex) {
            contentStr = signedFile.getSMIME().getSignedContent();
        }
        signatureContentWebView.getEngine().loadContent(contentStr);
        TimeStampToken timeStampToken = signedFile.getSMIME().getTimeStampToken();
        Button timeStampButton = new Button(ContextVS.getMessage("timeStampButtonLbl"));
        timeStampButton.setGraphic((Utils.getImage(FontAwesome.Glyph.CLOCK_ALT)));
        timeStampButton.setOnAction(actionEvent -> TimeStampPane.showDialog(timeStampToken));
        setMargin(timeStampButton, new Insets(5, 0, 5, 0));

        VBox.setVgrow(signatureContentWebView, Priority.ALWAYS);
        VBox.setVgrow(this, Priority.ALWAYS);
        add(timeStampButton, 0, 0);
        Region signedFileRegion = getSignedFileRegion();
        setMargin(signedFileRegion, new Insets(5, 10, 5, 0));
        add(signedFileRegion, 1, 0);
        add(openSignatureInfoButton, 2, 0);
        add(signatureContentWebView, 0, 1);
        setColumnSpan(signatureContentWebView, 3);
        //add(contentWithoutFormatCheckBox, 0, 2);
        JSONObject signedContentJSON = null;
        if(signedFile.getSMIME().getContentTypeVS() == ContentTypeVS.ASCIIDOC) {
            signedContentJSON =  (JSONObject) JSONSerializer.toJSON(signedFile.getOperationDocument());
        } else signedContentJSON =  (JSONObject)JSONSerializer.toJSON(signedFile.getSMIME().getSignedContent());
        String timeStampDateStr = "";
        if(signedFile.getSMIME().getTimeStampToken() != null) {
            timeStampDateStr = DateUtils.getDateStr(signedFile.getSMIME().
                    getTimeStampToken().getTimeStampInfo().getGenTime(),"dd/MMM/yyyy HH:mm");
        }
        try {
            JSONObject signedContent = (JSONObject) JSONSerializer.toJSON(signedFile.getSMIME().getSignedContent());
            signatureContentWebView.getEngine().loadContent(signedContent.toString(3), "application/json");
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private Region getSignedFileRegion() {
        TypeVS operation = signedFile.getTypeVS();
        if(operation == null) return Utils.getSpacer();
        switch(operation) {
            case SEND_SMIME_VOTE:
                HBox result = new HBox();
                Button checkVoteButton = new Button(ContextVS.getMessage("checkVoteLbl"));
                checkVoteButton.setOnAction(actionEvent -> ProgressDialog.showDialog(new CheckVoteTask(
                        signedFile.getSMIME().getVoteVS().getX509Certificate()), ContextVS.getMessage("checkVoteLbl")));
                result.getChildren().add(checkVoteButton);
                return result;
            default: return Utils.getSpacer();
        }
    }

    private void initComponents() {
        if(signedFile == null) {
            log.debug("### NULL signedFile");
            return;
        }
    }

    public String getCaption() {
        return (signedFile != null)? signedFile.getCaption() : null;
    }

    public SignedFile getSignedFile () {
        return signedFile;
    }

    @Override public byte[] getDocumentBytes() throws Exception {
        return signedFile.getSMIME().getBytes();
    }

    @Override public ContentTypeVS getContentTypeVS() {
        return ContentTypeVS.SIGNED;
    }

    public class CheckVoteTask extends Task<ResponseVS> {

        X509Certificate x509AnonymousCert;

        public CheckVoteTask(X509Certificate x509AnonymousCert) {
            this.x509AnonymousCert = x509AnonymousCert;
        }

        @Override protected ResponseVS call() throws Exception {
            updateMessage(ContextVS.getMessage("checkVoteLbl"));
            JSONObject certExtensionData = CertUtils.getCertExtensionData(x509AnonymousCert, ContextVS.VOTE_OID);
            String hashCertVS = certExtensionData.getString("hashCertVS");
            String voteStateServiceURL = ContextVS.getInstance().getAccessControl().getVoteStateServiceURL(
                    StringUtils.toHex(hashCertVS));
            ResponseVS responseVS = HttpHelper.getInstance().getData(voteStateServiceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                //[state:voteVS.state.toString(), value:voteVS.optionSelected.content]
                JSONObject responseJSON = (JSONObject) responseVS.getMessageJSON();
                VoteVS.State voteState = VoteVS.State.valueOf(responseJSON.getString("state"));
                StringBuilder sb = new StringBuilder();
                switch (voteState) {
                    case OK:
                        sb.append(ContextVS.getMessage("voteStateOKMsg"));
                        break;
                    case CANCELLED:
                        sb.append(ContextVS.getMessage("voteStateCANCELLEDMsg"));
                        break;
                    case ERROR:
                        sb.append(ContextVS.getMessage("voteStateERRORMsg"));
                        break;
                }
                sb.append("<br/><br/><b>" + ContextVS.getMessage("optionSelectedLbl") + "</b>: " +
                        responseJSON.getString("value"));
                showMessage(sb.toString(), ContextVS.getMessage("checkVoteLbl"));
            } else showMessage(responseVS);
            return responseVS;
        }
    }

}