package org.sistemavotacion.worker;

import java.io.File;
import java.io.FileOutputStream;
import javax.swing.SwingWorker;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.mail.internet.MimeMessage;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeRequestWorker extends SwingWorker<Respuesta, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(
            RepresentativeRequestWorker.class);
    
    private VotingSystemWorkerType workerType;
    private SMIMEMessageWrapper smimeMessage;
    private VotingSystemWorkerListener workerListener;
    private X509Certificate accesRequestServerCert = null;
    
    private File selectedImage;
    private Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR);
    private String urlToSendDocument;
    
    public RepresentativeRequestWorker(VotingSystemWorkerType workerType,
            SMIMEMessageWrapper smimeMessage, File selectedImage, 
            String urlToSendDocument, X509Certificate accesRequestServerCert, 
            VotingSystemWorkerListener workerListener) throws Exception {
        this.workerType = workerType;
        this.urlToSendDocument = urlToSendDocument;
        this.selectedImage = selectedImage;
        this.smimeMessage = smimeMessage;
        this.workerListener = workerListener;
        this.accesRequestServerCert = accesRequestServerCert;
    }
    
    @Override protected void done() {//on the EDT
        if(workerListener != null) workerListener.showResult(this);
    }
    
    @Override protected Respuesta doInBackground() throws Exception {
        logger.debug("doInBackground - RepresentativeRequest service: " + urlToSendDocument);
                TimeStampRequest timeStampRequest = smimeMessage.getTimeStampRequest();
        respuesta = Contexto.INSTANCE.getHttpHelper().sendByteArray(
                timeStampRequest.getEncoded(), "timestamp-query", 
                Contexto.INSTANCE.getURLTimeStampServer());
        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            byte[] bytesToken = respuesta.getBytesArchivo();
            TimeStampToken timeStampToken = new TimeStampToken(
                    new CMSSignedData(bytesToken));
            X509Certificate timeStampCert = Contexto.INSTANCE.getTimeStampServerCert();
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().
                setProvider(Contexto.PROVIDER).build(timeStampCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
            smimeMessage.setTimeStampToken(timeStampToken);
            try {
                File encryptedDocument = File.createTempFile("encryptedDocument", ".p7m");
                encryptedDocument.deleteOnExit();
                MimeMessage mimeMessage = Encryptor.encryptSMIME(
                        smimeMessage, accesRequestServerCert);
                mimeMessage.writeTo(new FileOutputStream(encryptedDocument));
                Map<String, Object> fileMap = new HashMap<String, Object>();
                String representativeDataFileName = 
                        Contexto.REPRESENTATIVE_DATA_FILE_NAME + ":" + 
                        Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
                fileMap.put(representativeDataFileName, encryptedDocument);
                fileMap.put(Contexto.IMAGE_FILE_NAME, selectedImage);

                respuesta = Contexto.INSTANCE.getHttpHelper().sendObjectMap(
                        fileMap, urlToSendDocument);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                respuesta.appendErrorMessage(ex.getMessage());
            }
        }
        return respuesta;
    }

    @Override public String getMessage() {
        if(respuesta != null) return respuesta.getMensaje();
        else return null;
    }
    
    @Override  public int getStatusCode() {
        if(respuesta == null) return Respuesta.SC_ERROR;
        else return respuesta.getCodigoEstado();
    }

    @Override public String getErrorMessage() {
        if(workerType != null) return "### ERROR - " + workerType + " - msg: " 
                + respuesta.getMensaje(); 
        else return "### ERROR - msg: " + respuesta.getMensaje();  
    }
        
    @Override public VotingSystemWorkerType getType() {
        return workerType;
    }
    
}