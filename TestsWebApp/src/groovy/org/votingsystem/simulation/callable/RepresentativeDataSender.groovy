package org.votingsystem.simulation.callable;

import java.io.File;
import org.votingsystem.model.ResponseVS;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.apache.log4j.Logger;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeDataSender implements Callable<ResponseVS>{
    
    private static Logger log = Logger.getLogger(
            RepresentativeDataSender.class);
    
    private SMIMEMessageWrapper representativeDataSmimeMessage;
    private X509Certificate accesRequestServerCert = null;
    
    private File selectedImage;
    private ResponseVS respuesta = new ResponseVS(ResponseVS.SC_ERROR);
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

    @Override public ResponseVS call() throws Exception {
        log.debug("doInBackground - RepresentativeRequest service: " + urlToSendDocument);
        try {
            MessageTimeStamper timeStamper = new MessageTimeStamper(representativeDataSmimeMessage);
            respuesta = timeStamper.call();
            if(ResponseVS.SC_OK != respuesta.getStatusCode()) return respuesta;
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
            log.error(ex.getMessage(), ex);
            respuesta.appendErrorMessage(ex.getMessage());
        }
        return respuesta;
    }
    
}