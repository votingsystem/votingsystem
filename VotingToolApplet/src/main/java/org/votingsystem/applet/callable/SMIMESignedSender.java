package org.votingsystem.applet.callable;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.votingsystem.applet.util.HttpHelper;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.apache.log4j.Logger;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SMIMESignedSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(SMIMESignedSender.class);

    private String urlToSendDocument;
    private SMIMEMessageWrapper smimeMessage;
    private X509Certificate destinationCert = null;
    private KeyPair keypair;
    private Integer id;
    
    private List<String> headerNameList = new ArrayList<String>();
    
    public SMIMESignedSender(Integer id, SMIMEMessageWrapper smimeMessage, 
            String urlToSendDocument, KeyPair keypair, X509Certificate destinationCert,
            String... headerNames) {
        if(headerNames != null) {
            for(String headerName: headerNames) {
                headerNameList.add(headerName);
            }
        }
        this.id = id;
        this.smimeMessage = smimeMessage;
        this.urlToSendDocument = urlToSendDocument;
        this.keypair = keypair;
        this.destinationCert = destinationCert;
    }

    @Override public ResponseVS call() throws Exception {
        logger.debug("doInBackground - urlToSendDocument: " + urlToSendDocument);
        MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage);
        ResponseVS responseVS = timeStamper.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        smimeMessage = timeStamper.getSmimeMessage();
        byte[] messageToSendBytes = null; 
        String documentContentType = null;
        if(destinationCert != null) {
            messageToSendBytes = Encryptor.encryptSMIME(
                smimeMessage, destinationCert);
            documentContentType = ContentTypeVS.SIGNED_AND_ENCRYPTED;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            smimeMessage.writeTo(baos);
            messageToSendBytes = baos.toByteArray();
            baos.close();
            documentContentType = org.votingsystem.model.ContentTypeVS.SIGNED;
        } 
        responseVS = HttpHelper.INSTANCE.sendByteArray(
                messageToSendBytes, documentContentType, urlToSendDocument,
                headerNameList.toArray(new String[headerNameList.size()]));            
        
       if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            if(keypair != null) {
                byte[] encryptedResponseBytes = responseVS.getMessageBytes();
                try {
                    SMIMEMessageWrapper signedMessage = Encryptor.decryptSMIMEMessage(
                        encryptedResponseBytes, keypair.getPublic(), keypair.getPrivate());
                    responseVS.setSmimeMessage(signedMessage);
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    responseVS.appendErrorMessage(ex.getMessage());
                }
            }
        }
        return responseVS;
    }

}