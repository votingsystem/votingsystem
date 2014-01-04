package org.votingsystem.android.callable;

import android.content.Context;
import android.util.Log;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.VotingSystemKeyStoreException;
import org.votingsystem.util.HttpHelper;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VoteSender implements Callable<ResponseVS> {

    public static final String TAG = "VoteSender";

    private VoteVS vote;
    private char[] password;
    private Context context = null;
    private ContextVS contextVS = null;
    private byte[] keyStoreBytes = null;

    public VoteSender(VoteVS vote, byte[] keyStoreBytes, char[] password, Context context) {
        this.vote = vote;
        this.keyStoreBytes = keyStoreBytes;
        this.password = password;
        this.context = context;
        contextVS = ContextVS.getInstance(context);
    }

    @Override public ResponseVS call() {
        Log.d(TAG + ".processVote(...)", "call - event subject: " + vote.getEventVS().getSubject());
        ResponseVS responseVS = null;
        try {
            vote.genVote();
            String serviceURL = contextVS.getControlCenter().getVoteServiceURL();
            String subject = context.getString(R.string.request_msg_subject,
                    vote.getEventVS().getEventVSId());
            String userVS = null;
            if (contextVS.getUserVS() != null) userVS = contextVS.getUserVS().getNif();

            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
            PrivateKey privateKey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, password);
            Certificate[] chain = keyStore.getCertificateChain(USER_CERT_ALIAS);
            X509Certificate userCert = (X509Certificate) chain[0];
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    privateKey, chain, ContextVS.SIGNATURE_ALGORITHM);

            JSONObject accessRequestJSON = new JSONObject(vote.getAccessRequestDataMap());
            SMIMEMessageWrapper accessRequest = signedMailGenerator.genMimeMessage(
                    userVS, contextVS.getAccessControl().getNameNormalized(),
                    accessRequestJSON.toString(), subject);
            AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(accessRequest,
                    vote, contextVS.getAccessControl().getCertificate(),
                    contextVS.getAccessControl().getAccessServiceURL(),context);
            responseVS = accessRequestDataSender.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            JSONObject voteJSON = new JSONObject(vote.getVoteDataMap());
            CertificationRequestVS certificationRequest = accessRequestDataSender.getPKCS10WrapperClient();
            SMIMEMessageWrapper signedVote = certificationRequest.genMimeMessage(
                    vote.getHashCertVSBase64(), vote.getEventVS().getControlCenter().getNameNormalized(),
                    voteJSON.toString(), context.getString(R.string.vote_msg_subject), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(signedVote, context);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                //AccesRequest OK and Vote error -> Cancel access request
                cancelAccessRequest(signedMailGenerator, userVS);
                return responseVS;
            }
            signedVote = timeStamper.getSmimeMessage();
            byte[] messageToSend = Encryptor.encryptSMIME(signedVote,
                    vote.getEventVS().getControlCenter().getCertificate());
            responseVS = HttpHelper.sendData(messageToSend,ContentTypeVS.VOTE,serviceURL);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessageWrapper voteReceipt = Encryptor.decryptSMIMEMessage(
                        responseVS.getMessageBytes(), certificationRequest.getKeyPair().getPublic(),
                        certificationRequest.getKeyPair().getPrivate());
                vote.setVoteReceipt(voteReceipt);
                byte[] base64EncodedKey = Base64.encode(
                        certificationRequest.getPrivateKey().getEncoded());
                byte[] encryptedKey = Encryptor.encryptMessage(base64EncodedKey, userCert);
                vote.setPkcs10WrapperClient(certificationRequest);
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
            Log.d(TAG + ".getSolicitudAcceso(...)", " - contentDigest: " + contentDigest);*/
        } catch(VotingSystemKeyStoreException ex) {
            ex.printStackTrace();
            return new ResponseVS(ResponseVS.SC_ERROR, context.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            return new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
        }
        return responseVS;
    }


    private ResponseVS cancelAccessRequest(SignedMailGenerator signedMailGenerator, String userVS) {
        Log.d(TAG + ".cancelAccessRequest(...)", "");
        try {
            String subject = context.getString(R.string.cancel_vote_msg_subject);
            String serviceURL = contextVS.getAccessControl().getCancelVoteServiceURL();
            JSONObject cancelDataJSON = new JSONObject(vote.getCancelVoteDataMap());
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL,
                    cancelDataJSON.toString(), ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED,
                    subject,  keyStoreBytes, password,
                    contextVS.getAccessControl().getCertificate(), context);
            return smimeSignedSender.call();
        } catch(Exception ex) {
            ex.printStackTrace();
            return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
    }

}