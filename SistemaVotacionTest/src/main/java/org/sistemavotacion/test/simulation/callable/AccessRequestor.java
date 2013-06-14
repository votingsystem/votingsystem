package org.sistemavotacion.test.simulation.callable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.mail.internet.MimeMessage;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.sistemavotacion.Contexto;
import static org.sistemavotacion.Contexto.KEY_SIZE;
import static org.sistemavotacion.Contexto.PROVIDER;
import static org.sistemavotacion.Contexto.SIG_NAME;
import static org.sistemavotacion.Contexto.VOTE_SIGN_MECHANISM;
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
            TimeStampRequest timeStampRequest = smimeMessage.getTimeStampRequest();
            Respuesta respuesta = Contexto.INSTANCE.getHttpHelper().sendByteArray(
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

                File csrEncryptedFile = File.createTempFile("csrEncryptedFile", ".p7m");
                csrEncryptedFile.deleteOnExit();
                Encryptor.encryptMessage(pkcs10WrapperClient.getPEMEncodedRequestCSR(), 
                        csrEncryptedFile, destinationCert);

                MimeMessage mimeMessage = Encryptor.encryptSMIME(
                        smimeMessage, destinationCert);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                mimeMessage.writeTo(baos);
                baos.close();
                String csrFileName = Contexto.CSR_FILE_NAME + ":" + 
                        Contexto.ENCRYPTED_CONTENT_TYPE;

                String accessRequestFileName = Contexto.ACCESS_REQUEST_FILE_NAME + ":" + 
                        Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
                Map<String, Object> mapToSend = new HashMap<String, Object>();
                mapToSend.put(csrFileName, csrEncryptedFile);
                mapToSend.put(accessRequestFileName, baos.toByteArray());

                respuesta = Contexto.INSTANCE.getHttpHelper().
                        sendObjectMap(mapToSend, evento.getUrlSolicitudAcceso());
                if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    byte[] encryptedData = respuesta.getBytesArchivo();
                    byte[] decryptedData = Encryptor.decryptFile(encryptedData, 
                            pkcs10WrapperClient.getPublicKey(), 
                            pkcs10WrapperClient.getPrivateKey());
                    pkcs10WrapperClient.initSigner(decryptedData);
                }
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