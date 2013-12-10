package org.votingsystem.callable;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeDataSender implements Callable<ResponseVS>{
    
    private static Logger logger = Logger.getLogger(
            RepresentativeDataSender.class);
    
    private SMIMEMessageWrapper representativeDataSmimeMessage;
    private X509Certificate accesRequestServerCert = null;
    
    private File selectedImage;
    private String urlToSendDocument;
    
    public RepresentativeDataSender(SMIMEMessageWrapper representativeDataSmimeMessage, File selectedImage,
            String urlToSendDocument) throws Exception {
        this.urlToSendDocument = urlToSendDocument;
        this.selectedImage = selectedImage;
        this.representativeDataSmimeMessage = representativeDataSmimeMessage;
        this.accesRequestServerCert = ContextVS.getInstance().getAccessControl().getX509Certificate();
    }

    @Override public ResponseVS call() throws Exception {
        logger.debug("doInBackground - RepresentativeRequest service: " + urlToSendDocument);
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR);
        try {
            MessageTimeStamper timeStamper = new MessageTimeStamper(representativeDataSmimeMessage);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            representativeDataSmimeMessage = timeStamper.getSmimeMessage();
            byte[] representativeEncryptedDataBytes = Encryptor.encryptSMIME(
                    representativeDataSmimeMessage, accesRequestServerCert);
            Map<String, Object> fileMap = new HashMap<String, Object>();
            String representativeDataFileName = 
                    ContextVS.REPRESENTATIVE_DATA_FILE_NAME + ":" + ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED.getName();
            fileMap.put(representativeDataFileName, representativeEncryptedDataBytes);
            fileMap.put(ContextVS.IMAGE_FILE_NAME, selectedImage);
            responseVS = HttpHelper.getInstance().sendObjectMap(fileMap, urlToSendDocument);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            responseVS.appendMessage(ex.getMessage());
        }
        return responseVS;
    }
    
}