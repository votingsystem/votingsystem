package org.votingsystem.client.pane;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import org.bouncycastle.tsp.TimeStampToken;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.voting.VoteCertExtensionDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.SignedFile;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CMSPane extends GridPane implements DocumentVS {

    private static Logger log = Logger.getLogger(CMSPane.class.getName());

    private SignedFile signedFile;
    
    public CMSPane(final SignedFile signedFile) throws Exception {
        super();
        this.signedFile = signedFile;
        setHgap(3);
        setVgap(3);
        setPadding(new Insets(5, 10, 5, 10));
        Button openSignatureInfoButton = new Button(ContextVS.getMessage("openSignedFileButtonLbl"));
        openSignatureInfoButton.setOnAction(actionEvent -> CMSSignersPane.showDialog(signedFile));
        openSignatureInfoButton.setPrefWidth(150);
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        getColumnConstraints().addAll(column1, column2);
        setHalignment(openSignatureInfoButton, HPos.RIGHT);
        WebView signatureContentWebView = new WebView();
        if (signedFile.isValidSignature()) {
            openSignatureInfoButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
            openSignatureInfoButton.setText(ContextVS.getMessage("signatureOKLbl"));
        } else {
            openSignatureInfoButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
            openSignatureInfoButton.setText(ContextVS.getMessage("signatureERRORLbl"));
        }
        String contentStr = null;
        try {
            Map dataMap = signedFile.getCMS().getSignedContent(new TypeReference<HashMap<String, Object>>() {});
            contentStr = Formatter.format(dataMap);
        }  catch(Exception ex) {
            contentStr = signedFile.getCMS().getSignedContentStr();
        }
        signatureContentWebView.getEngine().loadContent(contentStr);
        TimeStampToken timeStampToken = signedFile.getCMS().getTimeStampToken();
        Button timeStampButton = new Button(ContextVS.getMessage("timeStampButtonLbl"));
        timeStampButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.CLOCK_ALT)));
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
        Map signedContentMap = signedFile.getCMS().getSignedContent(new TypeReference<HashMap<String, Object>>() {});
        String timeStampDateStr = "";
        if(signedFile.getCMS().getTimeStampToken() != null) {
            timeStampDateStr = DateUtils.getDateStr(signedFile.getCMS().
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
                checkVoteButton.setOnAction(actionEvent -> {
                    try {
                        ProgressDialog.show(new CheckVoteTask(
                            signedFile.getCMS().getVote().getX509Certificate()), ContextVS.getMessage("checkVoteLbl"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
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
        return signedFile.getCMS().toPEM();
    }

    @Override public ContentType getContentTypeVS() {
        return ContentType.SIGNED;
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
            ResponseVS responseVS = HttpHelper.getInstance().getData(voteStateServiceURL, ContentType.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                VoteDto voteDto = (VoteDto) responseVS.getMessage(VoteDto.class);
                StringBuilder sb = new StringBuilder();
                switch (voteDto.getState()) {
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
                sb.append("<br/><br/><b>" + ContextVS.getMessage("optionSelectedLbl") + "</b>: " +
                        voteDto.getOptionSelected().getContent());
                MainApp.showMessage(sb.toString(), ContextVS.getMessage("checkVoteLbl"));
            } else MainApp.showMessage(responseVS.getStatusCode(), responseVS.getMessage());
            return responseVS;
        }
    }

}