package org.votingsystem.android.callable;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.VOTE_SIGN_MECHANISM;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteSender implements Callable<ResponseVS> {

    public static final String TAG = VoteSender.class.getSimpleName();

    private VoteVS vote;
    private AppContextVS contextVS = null;

    public VoteSender(VoteVS vote, AppContextVS context) {
        this.vote = vote;
        this.contextVS = context;
    }

    @Override public ResponseVS call() {
        LOGD(TAG + ".call", "eventvs subject: " + vote.getEventVS().getSubject());
        ResponseVS responseVS = null;
        try {
            vote.genVote();
            String serviceURL = contextVS.getControlCenter().getVoteServiceURL();
            String subject = contextVS.getString(R.string.request_msg_subject,
                    vote.getEventVS().getEventVSId());
            JSONObject accessRequestJSON = new JSONObject(vote.getAccessRequestDataMap());
            responseVS = contextVS.signMessage(contextVS.getAccessControl().getName(),
                    accessRequestJSON.toString(), subject, contextVS.getTimeStampServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            //send access request to fetch the anonymous certificate that signs the vote
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.TEXT.getName();
            CertificationRequestVS certificationRequest = CertificationRequestVS.getVoteRequest(
                    KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM, PROVIDER,
                    vote.getEventVS().getAccessControl().getServerURL(),
                    vote.getEventVS().getEventVSId().toString(), vote.getHashCertVSBase64());
            String accessRequestFileName = ContextVS.ACCESS_REQUEST_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, certificationRequest.getCsrPEM());
            mapToSend.put(accessRequestFileName, responseVS.getSMIME().getBytes());
            responseVS = HttpHelper.sendObjectMap(mapToSend,
                    contextVS.getAccessControl().getAccessServiceURL());
            if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            certificationRequest.initSigner(responseVS.getMessageBytes());
            JSONObject voteJSON = new JSONObject(vote.getVoteDataMap());
            SMIMEMessage signedVote = certificationRequest.getSMIME(
                    vote.getHashCertVSBase64(), vote.getEventVS().getControlCenter().getName(),
                    voteJSON.toString(), contextVS.getString(R.string.vote_msg_subject), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(signedVote, contextVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
                return responseVS;
            }
            signedVote = timeStamper.getSMIME();
            responseVS = HttpHelper.sendData(signedVote.getBytes(), ContentTypeVS.VOTE,serviceURL);
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                cancelAccessRequest(); //AccesRequest OK and Vote error -> Cancel access request
                return responseVS;
            } else {
                SMIMEMessage voteReceipt = new SMIMEMessage(new ByteArrayInputStream(
                        responseVS.getMessageBytes()));
                try {
                    vote.setVoteReceipt(voteReceipt);
                } catch(Exception ex) {
                    ex.printStackTrace();
                    cancelAccessRequest();
                    return new ResponseVS(ResponseVS.SC_ERROR,
                            contextVS.getString(R.string.vote_option_mismatch));
                }
                byte[] base64EncodedKey = Base64.encode(
                        certificationRequest.getPrivateKey().getEncoded());
                byte[] encryptedKey = Encryptor.encryptMessage(
                        base64EncodedKey, contextVS.getX509UserCert());
                vote.setCertificationRequest(certificationRequest);
                vote.setEncryptedKey(encryptedKey);
                responseVS.setData(vote);
            }
        } catch(ExceptionVS ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(contextVS.getString(R.string.exception_lbl),
                    contextVS.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, contextVS);
        } finally { return responseVS;}
    }

    private ResponseVS cancelAccessRequest() {
        LOGD(TAG + ".cancelAccessRequest", "cancelAccessRequest");
        try {
            String subject = contextVS.getString(R.string.cancel_vote_msg_subject);
            String serviceURL = contextVS.getAccessControl().getCancelVoteServiceURL();
            JSONObject cancelDataJSON = new JSONObject(vote.getCancelVoteDataMap());
            ResponseVS responseVS = contextVS.signMessage(contextVS.getAccessControl().getName(),
                    cancelDataJSON.toString(), subject, contextVS.getTimeStampServiceURL());
            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                    ContentTypeVS.JSON_SIGNED, serviceURL);
            return responseVS;
        } catch(Exception ex) {
            ex.printStackTrace();
            return ResponseVS.EXCEPTION(ex.getMessage(),
                    contextVS.getString(R.string.exception_lbl));
        }
    }

}