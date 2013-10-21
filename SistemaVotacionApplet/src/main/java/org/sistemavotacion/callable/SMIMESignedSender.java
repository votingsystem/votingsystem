package org.sistemavotacion.callable;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SMIMESignedSender implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(
            SMIMESignedSender.class);

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

    @Override public Respuesta call() throws Exception {
        logger.debug("doInBackground - urlToSendDocument: " + urlToSendDocument);
        MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage);
        Respuesta respuesta = timeStamper.call();
        if(Respuesta.SC_OK != respuesta.getCodigoEstado()) return respuesta;
        smimeMessage = timeStamper.getSmimeMessage();
        byte[] messageToSendBytes = null; 
        String documentContentType = null;
        if(destinationCert != null) {
            messageToSendBytes = Encryptor.encryptSMIME(
                smimeMessage, destinationCert);
            documentContentType = Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            smimeMessage.writeTo(baos);
            messageToSendBytes = baos.toByteArray();
            baos.close();
            documentContentType = Contexto.SIGNED_CONTENT_TYPE;
        } 
        respuesta = Contexto.INSTANCE.getHttpHelper().sendByteArray(
                messageToSendBytes, documentContentType, urlToSendDocument,
                headerNameList.toArray(new String[headerNameList.size()]));            
        
       if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            if(keypair != null) {
                byte[] encryptedResponseBytes = respuesta.getMessageBytes();
                try {
                    SMIMEMessageWrapper signedMessage = Encryptor.decryptSMIMEMessage(
                        encryptedResponseBytes, keypair.getPublic(), keypair.getPrivate());
                    respuesta.setSmimeMessage(signedMessage);
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    respuesta.appendErrorMessage(ex.getMessage());
                }
            }
        }
        respuesta.setId(id);
        return respuesta;
    }

}