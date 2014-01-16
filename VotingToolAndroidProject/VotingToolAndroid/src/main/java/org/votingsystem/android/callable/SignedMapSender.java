package org.votingsystem.android.callable;

import android.content.Context;
import android.util.Log;
import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.VotingSystemKeyStoreException;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;

import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
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
    private Context context = null;
    private String fromUser = null;
    private String toUser = null;
    private String subject = null;
    private String signedFileName = null;
    private String textToSign = null;
    private Map<String, Object> mapToSend;
    private String serviceURL = null;
    private ContentTypeVS contentType;

    public SignedMapSender(String fromUser, String toUser, String textToSign,
            Map<String, Object> mapToSend, String subject, Header header, String serviceURL,
            String signedFileName, ContentTypeVS contentType, char[] password,
            X509Certificate receiverCert, Context context) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.textToSign = textToSign;
        this.mapToSend = mapToSend;
        this.subject = subject;
        this.contentType = contentType;
        this.password = password;
        this.context = context;
        this.serviceURL = serviceURL;
        this.signedFileName = signedFileName;
        this.receiverCert = receiverCert;
    }

    @Override public ResponseVS call() {
        Log.d(TAG + ".call()", "serviceURL: " + serviceURL);
        ResponseVS responseVS = null;
        try {
            FileInputStream fis = context.openFileInput(KEY_STORE_FILE);
            byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    keyStoreBytes, USER_CERT_ALIAS, password, SIGNATURE_ALGORITHM);
            smimeMessage = signedMailGenerator.genMimeMessage(fromUser, toUser,textToSign, subject);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, context);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
                responseVS.setCaption(context.getString(R.string.timestamp_service_error_caption));
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
                    KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
                    PrivateKey privateKey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, password);
                    Certificate[] chain = keyStore.getCertificateChain(USER_CERT_ALIAS);
                    PublicKey publicKey = ((X509Certificate)chain[0]).getPublicKey();
                    KeyPair keypair = new KeyPair(publicKey, privateKey);
                    byte[] encryptedData = responseVS.getMessageBytes();
                    byte[] decryptedData = Encryptor.decryptFile(encryptedData, publicKey,
                            privateKey);
                    responseVS.setMessageBytes(decryptedData);
                }
            }
        } catch(VotingSystemKeyStoreException ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(context.getString(R.string.pin_error_msg),
                    context.getString(R.string.exception_lbl));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex.getMessage(),
                    context.getString(R.string.exception_lbl));
        } finally {return responseVS;}
    }

}
