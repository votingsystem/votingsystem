package org.sistemavotacion.callable;

import java.io.File;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeDataSender implements Callable<Respuesta>{
    
    private static Logger logger = LoggerFactory.getLogger(
            RepresentativeDataSender.class);
    
    private SMIMEMessageWrapper representativeDataSmimeMessage;
    private X509Certificate accesRequestServerCert = null;
    
    private File selectedImage;
    private Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR);
    private String urlToSendDocument;
    
    public RepresentativeDataSender(
            SMIMEMessageWrapper representativeDataSmimeMessage, 
            File selectedImage, String urlToSendDocument, 
            X509Certificate accesRequestServerCert) throws Exception {
        this.urlToSendDocument = urlToSendDocument;
        this.selectedImage = selectedImage;
        this.representativeDataSmimeMessage = representativeDataSmimeMessage;
        this.accesRequestServerCert = accesRequestServerCert;
    }

    @Override public Respuesta call() throws Exception {
        logger.debug("doInBackground - RepresentativeRequest service: " + urlToSendDocument);
        try {
            MessageTimeStamper timeStamper = new MessageTimeStamper(representativeDataSmimeMessage);
            respuesta = timeStamper.call();
            if(Respuesta.SC_OK != respuesta.getCodigoEstado()) return respuesta;
            representativeDataSmimeMessage = timeStamper.getSmimeMessage();
            byte[] representativeEncryptedDataBytes = Encryptor.encryptSMIME(
                    representativeDataSmimeMessage, accesRequestServerCert);
            Map<String, Object> fileMap = new HashMap<String, Object>();
            String representativeDataFileName = 
                    Contexto.REPRESENTATIVE_DATA_FILE_NAME + ":" + 
                    Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
            fileMap.put(representativeDataFileName, representativeEncryptedDataBytes);
            fileMap.put(Contexto.IMAGE_FILE_NAME, selectedImage);
            respuesta = Contexto.INSTANCE.getHttpHelper().sendObjectMap(
                    fileMap, urlToSendDocument);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta.appendErrorMessage(ex.getMessage());
        }
        return respuesta;
    }
    
}