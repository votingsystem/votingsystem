package org.votingsystem.android.callable;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.ANDROID_PROVIDER;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;

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
        LOGD(TAG + ".call", "Event subject: " + vote.getEventVS().getSubject());
        ResponseVS responseVS = null;
        try {
            vote.genVote();
            String serviceURL = contextVS.getControlCenter().getVoteServiceURL();
            String subject = contextVS.getString(R.string.request_msg_subject,
                    vote.getEventVS().getEventVSId());
            String userVS = contextVS.getUserVS().getNif();

            KeyStore.PrivateKeyEntry keyEntry = contextVS.getUserPrivateKey();
            X509Certificate userCert = (X509Certificate) keyEntry.getCertificate();
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(keyEntry.getPrivateKey(),
                    keyEntry.getCertificateChain(), SIGNATURE_ALGORITHM, ANDROID_PROVIDER);

            JSONObject accessRequestJSON = new JSONObject(vote.getAccessRequestDataMap());
            SMIMEMessage accessRequest = signedMailGenerator.getSMIME(
                    userVS, contextVS.getAccessControl().getNameNormalized(),
                    accessRequestJSON.toString(), subject);
            AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(accessRequest,
                    vote, contextVS.getAccessControl().getCertificate(),
                    contextVS.getAccessControl().getAccessServiceURL(),contextVS);
            responseVS = accessRequestDataSender.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            JSONObject voteJSON = new JSONObject(vote.getVoteDataMap());
            CertificationRequestVS certificationRequest =
                    accessRequestDataSender.getCertificationRequest();
            SMIMEMessage signedVote = certificationRequest.getSMIME(
                    vote.getHashCertVSBase64(), vote.getEventVS().getControlCenter().getNameNormalized(),
                    voteJSON.toString(), contextVS.getString(R.string.vote_msg_subject), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(signedVote, contextVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
                return responseVS;
            }
            signedVote = timeStamper.getSMIME();
            byte[] messageToSend = Encryptor.encryptSMIME(signedVote,
                    vote.getEventVS().getControlCenter().getCertificate());
            responseVS = HttpHelper.sendData(messageToSend,ContentTypeVS.VOTE,serviceURL);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessage voteReceipt = Encryptor.decryptSMIME(responseVS.getMessageBytes(),
                        certificationRequest.getKeyPair().getPrivate());
                try {
                    vote.setVoteReceipt(voteReceipt);
                } catch(Exception ex) {
                    ex.printStackTrace();
                    cancelAccessRequest(signedMailGenerator, userVS);
                    return new ResponseVS(ResponseVS.SC_ERROR,
                            contextVS.getString(R.string.vote_option_mismatch));
                }
                byte[] base64EncodedKey = Base64.encode(
                        certificationRequest.getPrivateKey().getEncoded());
                byte[] encryptedKey = Encryptor.encryptMessage(base64EncodedKey, userCert);
                vote.setCertificationRequest(certificationRequest);
                vote.setEncryptedKey(encryptedKey);
                responseVS.setData(vote);
            } else {//AccesRequest OK and Vote error -> Cancel access request
                cancelAccessRequest(signedMailGenerator, userVS);
                return responseVS;
            }
            /* problema -> javax.activation.UnsupportedDataTypeException:
             * no object DCH for MIME type application/pkcs7-signature
            MimeMessage solicitudAccesoMimeMessage = dnies.gen(userVS,
                    Aplicacion.getAccessControl().getNameNormalized(),
                    signedContent, subject, null, SignedMailGenerator.Type.USER);
            Object content = solicitudAccesoMimeMessage.getContent();
            MimeMultipart mimeMultipart = null;
            if (content.getClass().isAssignableFrom(MimeMultipart.class)) {
                mimeMultipart = (MimeMultipart) content;
            }
            SMIMESigned smimeSigned = new SMIMESigned(mimeMultipart);*/
            /*Tambien se puede get el digest
            SMIMESignedGenerator gen = dnies.getSMIMESignedGenerator();
            byte[] contentDigestBytes = (byte[])gen.getGeneratedDigests().get(SMIMESignedGenerator.DIGEST_SHA1);
            String contentDigest = Base64.encodeToString(contentDigestBytes, Base64.DEFAULT);
            LOGD(TAG + ".getSolicitudAcceso(...)", " - contentDigest: " + contentDigest);*/
        } catch(ExceptionVS ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(contextVS.getString(R.string.exception_lbl),
                    contextVS.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if(message == null || message.isEmpty()) message = contextVS.getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(contextVS.getString(R.string.exception_lbl),
                    message);
        } finally { return responseVS;}
    }


    private ResponseVS cancelAccessRequest(SignedMailGenerator signedMailGenerator, String userVS) {
        LOGD(TAG + ".cancelAccessRequest(...)", "");
        try {
            String subject = contextVS.getString(R.string.cancel_vote_msg_subject);
            String serviceURL = contextVS.getAccessControl().getCancelVoteServiceURL();
            JSONObject cancelDataJSON = new JSONObject(vote.getCancelVoteDataMap());
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(
                    contextVS.getUserVS().getNif(),contextVS.getAccessControl().getNameNormalized(),
                    serviceURL, cancelDataJSON.toString(), ContentTypeVS.JSON_SIGNED,
                    subject, contextVS.getAccessControl().getCertificate(), contextVS);
            return smimeSignedSender.call();
        } catch(Exception ex) {
            ex.printStackTrace();
            return ResponseVS.getExceptionResponse(ex.getMessage(),
                    contextVS.getString(R.string.exception_lbl));
        }
    }

}