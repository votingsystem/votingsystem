package org.sistemavotacion.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.PrivateKey;
import javax.swing.SwingWorker;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.mail.internet.MimeMessage;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.bouncycastle.util.encoders.Base64;
import static org.sistemavotacion.Contexto.KEY_SIZE;
import static org.sistemavotacion.Contexto.PROVIDER;
import static org.sistemavotacion.Contexto.SIG_NAME;
import static org.sistemavotacion.Contexto.VOTE_SIGN_MECHANISM;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestLauncherWorker extends SwingWorker<Respuesta, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(AccessRequestLauncherWorker.class);

    private Evento evento;
    private VotingSystemWorkerListener workerListener;
    private SMIMEMessageWrapper solicitudAcceso;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private Integer id = null;
    private Respuesta respuesta = null;
    private X509Certificate accesRequestServerCert = null;
    
    public AccessRequestLauncherWorker(Integer id,
            SMIMEMessageWrapper solicitudAcceso,
            Evento evento, X509Certificate accesRequestServerCert, 
            VotingSystemWorkerListener workerListener) throws Exception {
        this.id = id;
        this.solicitudAcceso = solicitudAcceso;
        this.workerListener = workerListener;
        this.evento = evento;
        this.accesRequestServerCert = accesRequestServerCert;
        this.pkcs10WrapperClient = new PKCS10WrapperClient(
                KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM, PROVIDER, 
                evento.getControlAcceso().getServerURL(), 
                evento.getEventoId().toString(), 
                evento.getHashCertificadoVotoHex());
    }
    
    @Override protected void done() {//on the EDT
        try {
            respuesta = get();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        } 
        workerListener.showResult(this);
    }
    
    @Override protected Respuesta doInBackground() throws Exception {
        logger.debug("doInBackground - urlSolicitudAcceso: " + 
                evento.getUrlSolicitudAcceso());
        File csrEncryptedFile = File.createTempFile("csrEncryptedFile", ".p7m");
        csrEncryptedFile.deleteOnExit();
        Encryptor.encryptMessage(pkcs10WrapperClient.getPEMEncodedRequestCSR(), 
        			csrEncryptedFile, accesRequestServerCert);
        
        MimeMessage mimeMessage = Encryptor.encryptSMIME(
                solicitudAcceso, accesRequestServerCert);
        File accessrequestEncryptedFile = File.createTempFile("accessRequest", ".p7m");
        accessrequestEncryptedFile.deleteOnExit();
        mimeMessage.writeTo(new FileOutputStream(accessrequestEncryptedFile));
        
        String csrFileName = Contexto.CSR_FILE_NAME + ":" + 
                Contexto.ENCRYPTED_CONTENT_TYPE;
        
        String accessRequestFileName = Contexto.ACCESS_REQUEST_FILE_NAME + ":" + 
                Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
        Map<String, Object> mapToSend = new HashMap<String, Object>();
        mapToSend.put(csrFileName, csrEncryptedFile);
        mapToSend.put(accessRequestFileName, accessrequestEncryptedFile);
        
        respuesta = Contexto.getHttpHelper().sendObjectMap(
                mapToSend, evento.getUrlSolicitudAcceso());
        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            byte[] encryptedData = respuesta.getBytesArchivo();
            
            byte[] decryptedData = Encryptor.decryptFile(encryptedData, 
                    pkcs10WrapperClient.getPublicKey(), 
                    pkcs10WrapperClient.getPrivateKey());
            //logger.debug("decryptedData: " + new String(decryptedData));
            pkcs10WrapperClient.initSigner(decryptedData);
        }
        return respuesta;
    }
    
    public byte[] decryptFile(byte[] encrypted, PrivateKey receiverPrivateKey) 
        throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/None/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, receiverPrivateKey);

        byte[] decodedBytes =Base64.decode(encrypted);

        byte[] result = cipher.doFinal(decodedBytes);
        return result;
    }
    
    public String getCsrJSON(byte[] csrBytes, Key secretKey) {
        logger.debug("getCsrJSON");
        String csr = new String(csrBytes);
        String secretKeyStr = new String(Base64.encode(secretKey.getEncoded()));
        Map map = new HashMap();
        map.put("csr", csr);
        map.put("key", secretKeyStr);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }
    
    public PKCS10WrapperClient getPKCS10WrapperClient() {
        return pkcs10WrapperClient;
    }

    @Override public String getMessage() {
        if(respuesta != null) return respuesta.getMensaje();
        else return null;
    }

    @Override public int getId() {
        return this.id;
    }

    @Override  public int getStatusCode() {
        if(respuesta == null) return Respuesta.SC_ERROR;
        else return respuesta.getCodigoEstado();
    }
    
    @Override public Respuesta getRespuesta() {
        return respuesta;
    }
    
}