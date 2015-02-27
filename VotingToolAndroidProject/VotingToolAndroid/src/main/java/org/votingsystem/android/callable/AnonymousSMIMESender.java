package org.votingsystem.android.callable;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import javax.mail.Header;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AnonymousSMIMESender implements Callable<ResponseVS> {

    public static final String TAG = AnonymousSMIMESender.class.getSimpleName();
;
    private AppContextVS contextVS;
    private CertificationRequestVS certificationRequest;
    private String fromUser;
    private String toUser;
    private String textToSign;
    private String subject;
    private String serviceURL;
    private X509Certificate receiverCert;
    private Header header;

    public AnonymousSMIMESender(String fromUser, String toUser, String textToSign, String subject,
            Header header, String serviceURL, X509Certificate receiverCert,
            CertificationRequestVS certificationRequest,  AppContextVS context) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.textToSign = textToSign;
        this.subject = subject;
        this.header = header;
        this.serviceURL = serviceURL;
        this.receiverCert = receiverCert;
        this.contextVS = context;
        this.certificationRequest = certificationRequest;
    }

    @Override public ResponseVS call() {
        LOGD(TAG + ".call", "");
        ResponseVS responseVS = null;
        try {
            SMIMEMessage signedMessage = certificationRequest.getSMIME(fromUser, toUser,
                    textToSign, subject, header);
            MessageTimeStamper timeStamper = new MessageTimeStamper(signedMessage, contextVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
                return responseVS;
            }
            signedMessage = timeStamper.getSMIME();
            byte[] messageToSend = null;
            ContentTypeVS contentType = null;
            if(receiverCert != null) {
                contentType = ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED;
                messageToSend = Encryptor.encryptSMIME(signedMessage, receiverCert);
            } else {
                contentType = ContentTypeVS.JSON_SIGNED;
                messageToSend = signedMessage.getBytes();
            }
            responseVS = HttpHelper.sendData(messageToSend, contentType, serviceURL);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessage receipt = null;
                if(contentType == ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED) {
                    receipt = Encryptor.decryptSMIME(responseVS.getMessageBytes(),
                            certificationRequest.getKeyPair().getPrivate());
                } else if(contentType == ContentTypeVS.JSON_SIGNED) {
                    receipt = new SMIMEMessage(new ByteArrayInputStream(responseVS.getMessageBytes()));
                }
                responseVS.setSMIME(receipt);
            } else return responseVS;
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, contextVS);
        } finally { return responseVS; }
    }

}