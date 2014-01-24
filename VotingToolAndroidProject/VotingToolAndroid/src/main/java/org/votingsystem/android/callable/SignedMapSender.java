package org.votingsystem.android.callable;

import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.VotingSystemKeyStoreException;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;

import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.mail.Header;

import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SignedMapSender implements Callable<ResponseVS> {

    public static final String TAG = "SignedMapSender";

    private SMIMEMessageWrapper smimeMessage = null;
    private X509Certificate receiverCert = null;
    private char[] password;
    private AppContextVS contextVS = null;
    private String fromUser = null;
    private String toUser = null;
    private String subject = null;
    private String signedFileName = null;
    private String textToSign = null;
    private Map<String, Object> mapToSend;
    private String serviceURL = null;
    private ContentTypeVS contentType;
    private PublicKey decriptorPublicKey;
    private PrivateKey decriptorPrivateKey;

    public SignedMapSender(String fromUser, String toUser, String textToSign,
            Map<String, Object> mapToSend, String subject, Header header, String serviceURL,
            String signedFileName, ContentTypeVS contentType, char[] password,
            X509Certificate receiverCert, PublicKey decriptorPublicKey,
            PrivateKey decriptorPrivateKey, AppContextVS context) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.textToSign = textToSign;
        this.mapToSend = mapToSend;
        this.subject = subject;
        this.contentType = contentType;
        this.password = password;
        this.contextVS = context;
        this.serviceURL = serviceURL;
        this.signedFileName = signedFileName;
        this.decriptorPublicKey = decriptorPublicKey;
        this.decriptorPrivateKey = decriptorPrivateKey;
        this.receiverCert = receiverCert;
    }

    @Override public ResponseVS call() {
        Log.d(TAG + ".call()", "serviceURL: " + serviceURL);
        ResponseVS responseVS = null;
        try {
            FileInputStream fis = contextVS.openFileInput(KEY_STORE_FILE);
            byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    keyStoreBytes, USER_CERT_ALIAS, password, SIGNATURE_ALGORITHM);
            smimeMessage = signedMailGenerator.genMimeMessage(fromUser, toUser,textToSign, subject);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, contextVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
                responseVS.setCaption(contextVS.getString(R.string.timestamp_service_error_caption));
                responseVS.setNotificationMessage(responseVS.getMessage());
                return responseVS;
            }
            smimeMessage = timeStamper.getSmimeMessage();
            byte[] messageToSend = null;
            if(contentType.isEncrypted())
                messageToSend = Encryptor.encryptSMIME(smimeMessage, receiverCert);
            else messageToSend = smimeMessage.getBytes();
            mapToSend.put(signedFileName, messageToSend);
            responseVS = HttpHelper.sendObjectMap(mapToSend, serviceURL);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                if(responseVS.getContentType() != null &&
                        responseVS.getContentType().isEncrypted()) {
                    if(responseVS.getContentType().isSigned()) {
                        SMIMEMessageWrapper signedMessage = Encryptor.decryptSMIMEMessage(
                                responseVS.getMessageBytes(), decriptorPublicKey, decriptorPrivateKey);
                        responseVS.setSmimeMessage(signedMessage);
                    } else {
                        byte[] decryptedMessageBytes = Encryptor.decryptCMS(decriptorPrivateKey,
                                responseVS.getMessageBytes());
                        responseVS.setMessageBytes(decryptedMessageBytes);
                    }
                }
            }
        } catch(VotingSystemKeyStoreException ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(contextVS.getString(R.string.pin_error_msg),
                    contextVS.getString(R.string.exception_lbl));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex.getMessage(),
                    contextVS.getString(R.string.exception_lbl));
        } finally {return responseVS;}
    }

}
