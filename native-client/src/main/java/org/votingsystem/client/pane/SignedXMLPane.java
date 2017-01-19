package org.votingsystem.client.pane;

import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.util.DocumentContainer;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.Utils;
import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.SignedFile;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.model.Signature;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Messages;
import org.votingsystem.util.OperationType;

import java.security.cert.X509Certificate;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignedXMLPane extends GridPane implements DocumentContainer {

    private static Logger log = Logger.getLogger(SignedXMLPane.class.getName());

    private SignedFile signedFile;
    
    public SignedXMLPane(final SignedFile signedFile) throws Exception {
        super();
        this.signedFile = signedFile;
        setHgap(3);
        setVgap(3);
        setPadding(new Insets(5, 10, 5, 10));
        Button openSignatureInfoButton = new Button(Messages.currentInstance().get("openSignedFileButtonLbl"));
        openSignatureInfoButton.setOnAction(actionEvent -> SignersPane.showDialog(signedFile.getSignedDocument().getSignatures()));
        openSignatureInfoButton.setPrefWidth(150);
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        getColumnConstraints().addAll(column1, column2);
        setHalignment(openSignatureInfoButton, HPos.RIGHT);
        WebView signatureContentWebView = new WebView();
        if (signedFile.isValidSignature()) {
            openSignatureInfoButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
            openSignatureInfoButton.setText(Messages.currentInstance().get("signatureOKLbl"));
        } else {
            openSignatureInfoButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
            openSignatureInfoButton.setText(Messages.currentInstance().get("signatureERRORLbl"));
        }
        String contentStr = Formatter.format(signedFile);
        signatureContentWebView.getEngine().loadContent(contentStr);
        Signature firstSignature = signedFile.getSignedDocument().getFirstSignature();
        Button timeStampButton = new Button(Messages.currentInstance().get("timeStampButtonLbl"));
        timeStampButton.setGraphic((Utils.getIcon(FontAwesome.Glyph.CLOCK_ALT)));
        timeStampButton.setOnAction(actionEvent -> TimeStampPane.showDialog(firstSignature.getTimestampToken()));
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
        String timeStampDateStr = "";
        if(firstSignature.getTimestampToken() != null) {
            timeStampDateStr = DateUtils.getDateStr(firstSignature.getSignatureDate());
        }
        signatureContentWebView.getEngine().loadContent(new String(signedFile.getBody()),
                signedFile.getType().getMediaType());
    }

    private Region getSignedFileRegion() throws Exception {
        switch(signedFile.getType()) {
            case VOTE:
                HBox result = new HBox();
                Button checkVoteButton = new Button(Messages.currentInstance().get("checkVoteLbl"));
                checkVoteButton.setOnAction(actionEvent -> {
                    try {
                        ProgressDialog.show(new CheckVoteTask(signedFile.getSignedDocument().getVoteCert()),
                                Messages.currentInstance().get("checkVoteLbl"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                result.getChildren().add(checkVoteButton);
                return result;
            default:
                return Utils.getSpacer();
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
        return signedFile.getBody();
    }

    @Override public ContentType getContentType() {
        return ContentType.PKCS7_SIGNED;
    }

    public class CheckVoteTask extends Task<ResponseDto> {

        X509Certificate x509AnonymousCert;

        public CheckVoteTask(X509Certificate x509AnonymousCert) {
            this.x509AnonymousCert = x509AnonymousCert;
        }

        @Override protected ResponseDto call() throws Exception {
            updateMessage(Messages.currentInstance().get("checkVoteLbl"));
            CertVoteExtensionDto certExtensionDto = CertUtils.getCertExtensionData(
                    CertVoteExtensionDto.class, x509AnonymousCert, Constants.VOTE_OID);

            String voteStateServiceURL = OperationType.VOTE_REPOSITORY.getUrl(MainApp.instance().getVotingServiceEntityId());
            ResponseDto responseVS = HttpConn.getInstance().doGetRequest(voteStateServiceURL, ContentType.JSON.getName());
            if(ResponseDto.SC_OK == responseVS.getStatusCode()) {
                VoteDto voteDto = (VoteDto) responseVS.getMessage(VoteDto.class);
                StringBuilder sb = new StringBuilder();
                switch (voteDto.getState()) {
                    case OK:
                        sb.append(Messages.currentInstance().get("voteStateOKMsg"));
                        break;
                    case CANCELED:
                        sb.append(Messages.currentInstance().get("voteStateCANCELEDMsg"));
                        break;
                    case ERROR:
                        sb.append(Messages.currentInstance().get("voteStateERRORMsg"));
                        break;
                }
                sb.append("<br/><br/><b>" + Messages.currentInstance().get("optionSelectedLbl") + "</b>: " +
                        voteDto.getOptionSelected().getContent());
                MainApp.showMessage(sb.toString(), Messages.currentInstance().get("checkVoteLbl"));
            } else MainApp.showMessage(responseVS.getStatusCode(), responseVS.getMessage());
            return responseVS;
        }
    }

}