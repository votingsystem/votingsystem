package org.votingsystem.android.callable;

import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.VotingSystemKeyStoreException;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SMIMESignedSender implements Callable<ResponseVS> {

    public static final String TAG = "SMIMESignedSender";

    private SMIMEMessageWrapper smimeMessage = null;
    private X509Certificate receiverCert = null;
    private char[] password;
    private AppContextVS contextVS = null;
    private String serviceURL = null;
    private String fromUser = null;
    private String toUser = null;
    private String subject = null;
    private String textToSign = null;
    private ContentTypeVS contentType;

    public SMIMESignedSender(String fromUser, String toUser, String serviceURL,
            String textToSign, ContentTypeVS contentType, String subject,
            char[] password, X509Certificate receiverCert, AppContextVS context) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.textToSign = textToSign;
        this.subject = subject;
        this.contentType = contentType;
        this.password = password;
        this.contextVS = context;
        this.serviceURL = serviceURL;
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
            responseVS  = HttpHelper.sendData(messageToSend, contentType, serviceURL);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                if(responseVS.getContentType() != null &&
                        responseVS.getContentType().isEncrypted()) {
                    KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
                    PrivateKey privateKey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, password);
                    Certificate[] chain = keyStore.getCertificateChain(USER_CERT_ALIAS);
                    PublicKey publicKey = ((X509Certificate)chain[0]).getPublicKey();
                    if(responseVS.getContentType().isSigned()) {
                        SMIMEMessageWrapper signedMessage = Encryptor.decryptSMIMEMessage(
                                responseVS.getMessageBytes(), publicKey, privateKey);
                        responseVS.setSmimeMessage(signedMessage);
                    } else {
                        byte[] decryptedMessageBytes = Encryptor.decryptFile(
                                responseVS.getMessageBytes(), publicKey, privateKey);
                        responseVS = new ResponseVS(ResponseVS.SC_OK, decryptedMessageBytes);
                    }
                }
            }
        } catch(VotingSystemKeyStoreException ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                    contextVS.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
        } finally {return responseVS;}
    }

    public SMIMEMessageWrapper getSMIMEMessage() {
        return smimeMessage;
    }

}