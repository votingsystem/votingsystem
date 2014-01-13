package org.votingsystem.callable;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SMIMESignedSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(SMIMESignedSender.class);

    private String urlToSendDocument;
    private String timeStampServerURL;
    private SMIMEMessageWrapper smimeMessage;
    private X509Certificate destinationCert = null;
    private KeyPair keypair;
    private ContentTypeVS cotentType;
    
    String[] headers = null;
    
    public SMIMESignedSender(SMIMEMessageWrapper smimeMessage, String urlToSendDocument, String timeStampServerURL,
            ContentTypeVS cotentType, KeyPair keypair, X509Certificate destinationCert, String... headerNames) {
        headers = headerNames;
        this.smimeMessage = smimeMessage;
        this.urlToSendDocument = urlToSendDocument;
        this.timeStampServerURL = timeStampServerURL;
        this.cotentType = cotentType;
        this.keypair = keypair;
        this.destinationCert = destinationCert;
    }

    @Override public ResponseVS call() throws Exception {
        logger.debug("doInBackground - urlToSendDocument: " + urlToSendDocument);
        MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampServerURL);
        ResponseVS responseVS = timeStamper.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        smimeMessage = timeStamper.getSmimeMessage();
        byte[] messageToSendBytes = null;
        if(cotentType.isEncrypted()) messageToSendBytes = Encryptor.encryptSMIME(smimeMessage, destinationCert);
        else if(cotentType.isSigned()) messageToSendBytes = smimeMessage.getBytes();

        responseVS = HttpHelper.getInstance().sendData(messageToSendBytes, cotentType, urlToSendDocument, headers);
        ContentTypeVS responseContentType = responseVS.getContentType();
        try {
            if(responseContentType != null && responseContentType.isSignedAndEncrypted()) {
                SMIMEMessageWrapper signedMessage = Encryptor.decryptSMIMEMessage(
                        responseVS.getMessageBytes(), keypair.getPublic(), keypair.getPrivate());
                responseVS.setSmimeMessage(signedMessage);
            } else if(responseContentType != null && responseContentType.isEncrypted()) {
                byte[] decryptedBytes = Encryptor.decryptMessage(
                        responseVS.getMessageBytes(), keypair.getPublic(), keypair.getPrivate());
                responseVS.setMessageBytes(decryptedBytes);
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            responseVS.setStatusCode(ResponseVS.SC_ERROR);
            responseVS.appendMessage(ex.getMessage());
        }
        return responseVS;
    }

}