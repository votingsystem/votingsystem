package org.votingsystem.client.pane;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.client.Browser;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.voting.VoteCertExtensionDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.SignedFile;
import org.votingsystem.util.*;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMEPane extends GridPane implements DocumentVS {

    private static Logger log = Logger.getLogger(SMIMEPane.class.getSimpleName());

    private SignedFile signedFile;
    
    public SMIMEPane(final SignedFile signedFile) throws Exception {
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
            openSignatureInfoButton.setGraphic(Utils.getIcon(FontAwesomeIcons.CHECK));
            openSignatureInfoButton.setText(ContextVS.getMessage("signatureOKLbl"));
        } else {
            openSignatureInfoButton.setGraphic(Utils.getIcon(FontAwesomeIcons.TIMES, Utils.COLOR_RED_DARK));
            openSignatureInfoButton.setText(ContextVS.getMessage("signatureERRORLbl"));
        }
        String contentStr = null;
        try {
            Map dataMap = JSON.getMapper().readValue(signedFile.getSMIME().getSignedContent(),
                    new TypeReference<HashMap<String, Object>>() {});
            contentStr = Formatter.format(dataMap);
        }  catch(Exception ex) {
            contentStr = signedFile.getSMIME().getSignedContent();
        }
        signatureContentWebView.getEngine().loadContent(contentStr);
        TimeStampToken timeStampToken = signedFile.getSMIME().getTimeStampToken();
        Button timeStampButton = new Button(ContextVS.getMessage("timeStampButtonLbl"));
        timeStampButton.setGraphic((Utils.getIcon(FontAwesomeIcons.CLOCK_ALT)));
        timeStampButton.setOnAction(actionEvent -> TimeStampPane.showDialog(timeStampToken));
        setMargin(timeStampButton, new Insets(5, 0, 5, 0));
        add(timeStampButton, 0, 0);
        Region signedFileRegion = getSignedFileRegion();
        setMargin(signedFileRegion, new Insets(5, 10, 5, 0));
        add(signedFileRegion, 1, 0);
        add(openSignatureInfoButton, 2, 0);
        add(signatureContentWebView, 0, 1);
        RowConstraints row1 = new RowConstraints();
        row1.setVgrow(Priority.ALWAYS);
        getRowConstraints().addAll(new RowConstraints(), row1);
        setColumnSpan(signatureContentWebView, 3);
        //add(contentWithoutFormatCheckBox, 0, 2);
        Map signedContentMap = JSON.getMapper().readValue(signedFile.getSMIME().getSignedContent(),
                new TypeReference<HashMap<String, Object>>() {});
        String timeStampDateStr = "";
        if(signedFile.getSMIME().getTimeStampToken() != null) {
            timeStampDateStr = DateUtils.getDateStr(signedFile.getSMIME().
                    getTimeStampToken().getTimeStampInfo().getGenTime(),"dd/MMM/yyyy HH:mm");
        }
        try {
            String signedContentStr = JSON.getMapper().configure(
                    SerializationFeature.INDENT_OUTPUT,true).writeValueAsString(signedContentMap);
            signatureContentWebView.getEngine().loadContent(signedContentStr, "application/json");
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private Region getSignedFileRegion() throws Exception {
        TypeVS operation = signedFile.getTypeVS();
        if(operation == null) return Utils.getSpacer();
        switch(operation) {
            case SEND_VOTE:
                HBox result = new HBox();
                Button checkVoteButton = new Button(ContextVS.getMessage("checkVoteLbl"));
                checkVoteButton.setOnAction(actionEvent -> ProgressDialog.showDialog(new CheckVoteTask(
                        signedFile.getSMIME().getVoteVS().getX509Certificate()), ContextVS.getMessage("checkVoteLbl"),
                        Browser.getInstance().getScene().getWindow()));
                result.getChildren().add(checkVoteButton);
                return result;
            default: return Utils.getSpacer();
        }
    }

    private void initComponents() {
        if(signedFile == null) {
            log.info("### NULL signedFile");
            return;
        }
    }

    public String getCaption() throws Exception {
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
            VoteCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(
                    VoteCertExtensionDto.class, x509AnonymousCert, ContextVS.VOTE_OID);
            String voteStateServiceURL = ContextVS.getInstance().getAccessControl().getVoteStateServiceURL(
                    StringUtils.toHex(certExtensionDto.getHashCertVS()));
            ResponseVS responseVS = HttpHelper.getInstance().getData(voteStateServiceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                //[state:voteVS.state.toString(), value:voteVS.optionSelected.content]
                Map responseMap = responseVS.getMessageMap();
                VoteVS.State voteState = VoteVS.State.valueOf((String) responseMap.get("state"));
                StringBuilder sb = new StringBuilder();
                switch (voteState) {
                    case OK:
                        sb.append(ContextVS.getMessage("voteStateOKMsg"));
                        break;
                    case CANCELED:
                        sb.append(ContextVS.getMessage("voteStateCANCELEDMsg"));
                        break;
                    case ERROR:
                        sb.append(ContextVS.getMessage("voteStateERRORMsg"));
                        break;
                }
                sb.append("<br/><br/><b>" + ContextVS.getMessage("optionSelectedLbl") + "</b>: " + responseMap.get("value"));
                showMessage(sb.toString(), ContextVS.getMessage("checkVoteLbl"));
            } else showMessage(responseVS);
            return responseVS;
        }
    }

}