package org.sistemavotacion.test.simulation.callable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.mail.Header;
import javax.mail.internet.MimeMessage;
import org.sistemavotacion.Contexto;
import static org.sistemavotacion.Contexto.KEY_SIZE;
import static org.sistemavotacion.Contexto.PROVIDER;
import static org.sistemavotacion.Contexto.SIG_NAME;
import static org.sistemavotacion.Contexto.VOTE_SIGN_MECHANISM;
import org.sistemavotacion.callable.MessageTimeStamper;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta; 
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestor implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(AccessRequestor.class);

    private Evento evento;   
    private SMIMEMessageWrapper smimeMessage;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private X509Certificate destinationCert = null;
    
    public AccessRequestor (SMIMEMessageWrapper smimeMessage,
            Evento evento, X509Certificate destinationCert) throws Exception {
        this.smimeMessage = smimeMessage;
        this.evento = evento;
        this.destinationCert = destinationCert;
        this.pkcs10WrapperClient = new PKCS10WrapperClient(
                KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM, PROVIDER, 
                evento.getControlAcceso().getServerURL(), 
                evento.getEventoId().toString(), 
                evento.getHashCertificadoVotoHex());
    }
    
    @Override public Respuesta call() { 
        logger.debug("call - urlAccessRequest: " + evento.getUrlSolicitudAcceso());
        try {
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage);
            Respuesta respuesta = timeStamper.call();
            if(Respuesta.SC_OK != respuesta.getCodigoEstado()) return respuesta;
            smimeMessage = timeStamper.getSmimeMessage();

            Header header = new Header("votingSystemMessageType", "voteCsr");
            byte[] encryptedCSRBytes = Encryptor.encryptMessage(
                    pkcs10WrapperClient.getPEMEncodedRequestCSR(), 
                    destinationCert, header);
            byte[] accessRequesEncryptedBytes = Encryptor.encryptSMIME(
                    smimeMessage, destinationCert);
            String csrFileName = Contexto.CSR_FILE_NAME + ":" + 
                    Contexto.ENCRYPTED_CONTENT_TYPE;
            String accessRequestFileName = Contexto.ACCESS_REQUEST_FILE_NAME + ":" + 
                    Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, encryptedCSRBytes);
            mapToSend.put(accessRequestFileName, accessRequesEncryptedBytes);

            respuesta = Contexto.INSTANCE.getHttpHelper().
                    sendObjectMap(mapToSend, evento.getUrlSolicitudAcceso());
            if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                byte[] encryptedData = respuesta.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, 
                        pkcs10WrapperClient.getPublicKey(), 
                        pkcs10WrapperClient.getPrivateKey());
                pkcs10WrapperClient.initSigner(decryptedData);
            }
            
            return respuesta;
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        }
    }
    
    public PKCS10WrapperClient getPKCS10WrapperClient() {
        return pkcs10WrapperClient;
    }
    
}