package org.sistemavotacion.worker;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import javax.mail.internet.MimeMessage;
import javax.swing.SwingWorker;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
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
public class SMIMESignedSenderWorker extends SwingWorker<Respuesta, String> 
        implements VotingSystemWorker  {
    
    private static Logger logger = LoggerFactory.getLogger(
            SMIMESignedSenderWorker.class);

    private VotingSystemWorkerType workerType;
    private String urlToSendDocument;
    private VotingSystemWorkerListener workerListener;
    private Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR);
    private SMIMEMessageWrapper smimeMessage;
    private X509Certificate destinationCert = null;
    private KeyPair keypair;

    
    public SMIMESignedSenderWorker(VotingSystemWorkerType workerType, 
            SMIMEMessageWrapper smimeMessage, String urlToSendDocument, 
            KeyPair keypair, X509Certificate destinationCert,
            VotingSystemWorkerListener workerListener) {
        this.workerType = workerType;
        this.smimeMessage = smimeMessage;
        this.workerListener = workerListener;
        this.urlToSendDocument = urlToSendDocument;
        this.keypair = keypair;
        this.destinationCert = destinationCert;
    }
    
    @Override protected void done() {//on the EDT
        try {
            respuesta = get();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta.appendErrorMessage(ex.getMessage());
        } 
        workerListener.showResult(this);
    }
    
    @Override protected Respuesta doInBackground() throws Exception {
        logger.debug("doInBackground - urlToSendDocument: " + urlToSendDocument);
        TimeStampRequest timeStampRequest = smimeMessage.getTimeStampRequest();
        if(timeStampRequest == null) {
            logger.error("TimeStampRequest null");
            return new Respuesta(Respuesta.SC_ERROR, "TimeStampRequest null");
        }
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
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String documentContentType = null;
            if(destinationCert != null) {
                MimeMessage mimeMessage = Encryptor.encryptSMIME(
                    smimeMessage, destinationCert);
                mimeMessage.writeTo(baos);
                documentContentType = Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
            } else {
                smimeMessage.writeTo(baos);
                documentContentType = Contexto.SIGNED_CONTENT_TYPE;
            } 
            respuesta = Contexto.INSTANCE.getHttpHelper().sendByteArray(
                baos.toByteArray(), documentContentType, urlToSendDocument);            
            baos.close();
           if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                if(keypair != null) {
                    byte[] encryptedResponseBytes = respuesta.getBytesArchivo();
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
        }
        return respuesta;
    }

    public byte[] getMessageBytes() {
        if(respuesta == null) return null;
        else return respuesta.getBytesArchivo();
    }
    
    @Override public String getMessage() {
        if(respuesta == null) return null;
        else return respuesta.getMensaje();
    }

    @Override public int getStatusCode() {
        if(respuesta == null) return Respuesta.SC_ERROR;
        else return respuesta.getCodigoEstado();
    }
    
    @Override public Respuesta getRespuesta() {
        return respuesta;
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