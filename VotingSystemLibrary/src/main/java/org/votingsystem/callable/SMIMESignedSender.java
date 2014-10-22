package org.votingsystem.callable;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class SMIMESignedSender implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(SMIMESignedSender.class);

    private String urlToSendDocument;
    private String timeStampServerURL;
    private SMIMEMessage smimeMessage;
    private X509Certificate destinationCert = null;
    private KeyPair keypair;
    private ContentTypeVS cotentType;
    private String[] headers = null;
    
    public SMIMESignedSender(SMIMEMessage smimeMessage, String urlToSendDocument, String timeStampServerURL,
            ContentTypeVS cotentType, KeyPair keypair, X509Certificate destinationCert, String... headers) {
        this.smimeMessage = smimeMessage;
        this.urlToSendDocument = urlToSendDocument;
        this.timeStampServerURL = timeStampServerURL;
        this.cotentType = cotentType;
        this.keypair = keypair;
        this.destinationCert = destinationCert;
        this.headers = headers;
    }

    @Override public ResponseVS call() throws Exception {
        log.debug("doInBackground - urlToSendDocument: " + urlToSendDocument);
        MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampServerURL);
        ResponseVS responseVS = timeStamper.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        smimeMessage = timeStamper.getSMIME();
        byte[] messageToSendBytes = null;
        if(cotentType.isEncrypted()) messageToSendBytes = Encryptor.encryptSMIME(smimeMessage, destinationCert);
        else if(cotentType.isSigned()) messageToSendBytes = smimeMessage.getBytes();

        responseVS = HttpHelper.getInstance().sendData(messageToSendBytes, cotentType, urlToSendDocument, headers);
        ContentTypeVS responseContentType = responseVS.getContentType();
        try {
            if(responseContentType != null && responseContentType.isSignedAndEncrypted()) {
                SMIMEMessage signedMessage = Encryptor.decryptSMIMEMessage(
                        responseVS.getMessageBytes(), keypair.getPublic(), keypair.getPrivate());
                responseVS.setSMIME(signedMessage);
            } else if(responseContentType != null && responseContentType.isEncrypted()) {
                byte[] decryptedBytes = Encryptor.decryptMessage(
                        responseVS.getMessageBytes(), keypair.getPublic(), keypair.getPrivate());
                responseVS.setMessageBytes(decryptedBytes);
            } else if(responseContentType != null && ResponseVS.SC_OK == responseVS.getStatusCode() &&
                    (responseContentType == ContentTypeVS.VOTE || responseContentType == ContentTypeVS.JSON_SIGNED)) {
                responseVS.setSMIME(new SMIMEMessage(new ByteArrayInputStream(responseVS.getMessageBytes())));
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            responseVS.setStatusCode(ResponseVS.SC_ERROR);
            responseVS.appendMessage(ex.getMessage());
        }
        return responseVS;
    }

}