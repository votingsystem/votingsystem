package org.votingsystem.android.callable;

import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import javax.mail.Header;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class AnonymousSMIMESender implements Callable<ResponseVS> {

    public static final String TAG = "AnonymousSMIMESender";
;
    private AppContextVS contextVS;
    private CertificationRequestVS certificationRequest;
    private String fromUser;
    private String toUser;
    private String textToSign;
    private String subject;
    private String serviceURL;
    private X509Certificate receiverCert;
    private ContentTypeVS contentType;
    private Header header;

    public AnonymousSMIMESender(String fromUser, String toUser, String textToSign, String subject,
            Header header, String serviceURL, X509Certificate receiverCert,
            ContentTypeVS contentType,CertificationRequestVS certificationRequest,
            AppContextVS context) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.textToSign = textToSign;
        this.subject = subject;
        this.header = header;
        this.serviceURL = serviceURL;
        this.receiverCert = receiverCert;
        this.contextVS = context;
        this.contentType = contentType;
        this.certificationRequest = certificationRequest;
    }

    @Override public ResponseVS call() {
        Log.d(TAG + ".call()", "");
        ResponseVS responseVS = null;
        try {
            SMIMEMessageWrapper signedMessage = certificationRequest.genMimeMessage(fromUser, toUser,
                    textToSign, subject, header);
            MessageTimeStamper timeStamper = new MessageTimeStamper(signedMessage, contextVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
                return responseVS;
            }
            signedMessage = timeStamper.getSmimeMessage();
            byte[] messageToSend = Encryptor.encryptSMIME(signedMessage, receiverCert);
            responseVS = HttpHelper.sendData(messageToSend, contentType, serviceURL);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessageWrapper receipt = Encryptor.decryptSMIMEMessage(
                        responseVS.getMessageBytes(), certificationRequest.getKeyPair().getPublic(),
                        certificationRequest.getKeyPair().getPrivate());
                responseVS.setSmimeMessage(receipt);
            } else return responseVS;
        } catch(Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if(message == null || message.isEmpty()) message = contextVS.getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(contextVS.getString(R.string.exception_lbl),
                    message);
        } finally { return responseVS; }
    }

}