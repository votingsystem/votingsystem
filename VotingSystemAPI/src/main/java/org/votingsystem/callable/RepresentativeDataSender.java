package org.votingsystem.callable;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MediaTypeVS;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class RepresentativeDataSender implements Callable<ResponseVS>{
    
    private static Logger log = Logger.getLogger(RepresentativeDataSender.class.getSimpleName());
    
    private SMIMEMessage representativeDataSmimeMessage;
    private X509Certificate accesRequestServerCert = null;
    
    private File selectedImage;
    private String urlToSendDocument;
    
    public RepresentativeDataSender(SMIMEMessage representativeDataSmimeMessage, File selectedImage,
            String urlToSendDocument) throws Exception {
        this.urlToSendDocument = urlToSendDocument;
        this.selectedImage = selectedImage;
        this.representativeDataSmimeMessage = representativeDataSmimeMessage;
        this.accesRequestServerCert = ContextVS.getInstance().getAccessControl().getX509Certificate();
    }

    @Override public ResponseVS call() throws Exception {
        log.info("doInBackground - RepresentativeRequest service: " + urlToSendDocument);
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR);
        try {
            MessageTimeStamper timeStamper = new MessageTimeStamper(representativeDataSmimeMessage,
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL());
            representativeDataSmimeMessage = timeStamper.call();
            //byte[] representativeEncryptedDataBytes = Encryptor.encryptSMIME(
            //        representativeDataSmimeMessage, accesRequestServerCert);
            Map<String, Object> fileMap = new HashMap<String, Object>();
            String representativeDataFileName = 
                    ContextVS.REPRESENTATIVE_DATA_FILE_NAME + ":" + MediaTypeVS.JSON_SIGNED;
            fileMap.put(representativeDataFileName, representativeDataSmimeMessage.getBytes());
            fileMap.put(ContextVS.IMAGE_FILE_NAME, selectedImage);
            responseVS = HttpHelper.getInstance().sendObjectMap(fileMap, urlToSendDocument);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS.appendMessage(ex.getMessage());
        }
        return responseVS;
    }
    
}