package org.sistemavotacion.test.simulation.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import javax.mail.internet.MimeMessage;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.Contexto;
import static org.sistemavotacion.Contexto.TIMESTAMP_DNIe_HASH;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.KeyStoreHelper;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.TimeStampWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignatureClaimLauncher implements Callable<Respuesta>, 
        VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(SignatureClaimLauncher.class);

    private static final int ENVIAR_DOCUMENTO_FIRMADO_WORKER = 0;
    private static final int TIME_STAMP_WORKER               = 1;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    
    private SMIMEMessageWrapper signedClaimSMIME;
    private String nif;
    
    private String urlTimeStampServer = null;
    private String submitClaimsURL = null;
    private Long eventId = null;
    
    private Respuesta respuesta;
        
    public SignatureClaimLauncher (String nif, Long eventId)  throws Exception {
        this.nif = nif;
        this.eventId = eventId;
        urlTimeStampServer = ContextoPruebas.getUrlTimeStampServer(
                ContextoPruebas.getControlAcceso().getServerURL());
        submitClaimsURL = ContextoPruebas.getUrlSubmitClaims(
                ContextoPruebas.getControlAcceso().getServerURL());
    }
    
    
    @Override public Respuesta call() throws Exception {
        Respuesta respuesta = null;
        File file = new File(ContextoPruebas.getUserKeyStorePath(nif));
        KeyStore mockDnie = KeyStoreHelper.crearMockDNIe(nif, file,
                ContextoPruebas.getPrivateCredentialRaizAutoridad());
        logger.info("nif: " + nif + " - mockDnie: " + file.getAbsolutePath());

        ActorConIP controlAcceso = ContextoPruebas.getControlAcceso();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        
        String claimDataStr = getClaimDataJSON(eventId);
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        signedClaimSMIME = signedMailGenerator.genMimeMessage(
                nif, toUser, claimDataStr,
                ContextoPruebas.ASUNTO_MENSAJE_ALTA_REPRESENTANTE , null);
        new TimeStampWorker(TIME_STAMP_WORKER, urlTimeStampServer,
                    this, signedClaimSMIME.getTimeStampRequest(TIMESTAMP_DNIe_HASH),
                    ContextoPruebas.getControlAcceso().getTimeStampCert()).execute();
        countDownLatch.await();
        return getResult();
    }
    
   private void processDocument(SMIMEMessageWrapper smimeDocument) {
        if(smimeDocument == null) return;
        try {
            X509Certificate serverCert = ContextoPruebas.getControlAcceso().getCertificate();
            
            MimeMessage mimeMessage = Encryptor.encryptSMIME(smimeDocument, serverCert);
            File document = File.createTempFile("EncryptedAccessRequest", "p7m");
            document.deleteOnExit();
            mimeMessage.writeTo(new FileOutputStream(document));
  
            new DocumentSenderWorker(ENVIAR_DOCUMENTO_FIRMADO_WORKER, document, 
                    Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE,
                    submitClaimsURL, this).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            countDownLatch.countDown();
        }
    }

        
    public static String getRepresentativeDataJSON(String nif,
            String imageDigestStr) {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_DATA");
        map.put("representativeInfo", " --- contenido del representante -" + nif);
        map.put("base64ImageHash", imageDigestStr);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }

    @Override
    public void process(List<String> messages) {
        for(String message : messages)  {
            logger.debug("process -> " + message);
        }
    }

    @Override
    public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
        " - nif: " + nif +
        " - worker: " + worker.getClass().getSimpleName());
        switch(worker.getId()) {
            case ENVIAR_DOCUMENTO_FIRMADO_WORKER:
                respuesta = worker.getRespuesta();
                if (Respuesta.SC_OK == worker.getStatusCode()) {
                    respuesta.setMensaje(nif);
                }
                countDownLatch.countDown();
                break;
            case TIME_STAMP_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        signedClaimSMIME.setTimeStampToken((TimeStampWorker)worker);
                        processDocument(signedClaimSMIME);
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                } else {
                    String msg = "showResult - ERROR obteniendo sello de tiempo";
                    logger.debug(msg); 
                    respuesta = new Respuesta(worker.getStatusCode(), msg);
                    countDownLatch.countDown();
                }
                break;
            default:
                logger.debug("*** UNKNOWN WORKER ID: '" + worker.getId() + "'");
        }
    }

    public static String getClaimDataJSON(Long eventId) {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "FIRMA_RECLAMACION_SMIME");
        map.put("id", eventId);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }
    
    private Respuesta getResult() {
        return respuesta;
    }
}