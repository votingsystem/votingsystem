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
public class RepresentativeDelegationLauncher implements Callable<Respuesta>, 
        VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(RepresentativeDelegationLauncher.class);

    private static final int ENVIAR_DOCUMENTO_FIRMADO_WORKER = 0;
    private static final int TIME_STAMP_WORKER = 1;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    
    private SMIMEMessageWrapper representativeDelegationSMIME;
    private String userNIF;
    private String representativeNIF;
    
    private String urlTimeStampServer = null;
    private String urlDelegacionRepresentante = null;

    private Respuesta respuesta;
            
    
    public RepresentativeDelegationLauncher (String userNIF, 
            String representativeNIF) throws Exception {
        this.userNIF = userNIF;
        this.representativeNIF = representativeNIF;
        logger.info("userNIF: " + userNIF + " - representativeNIF: " + representativeNIF);
        urlTimeStampServer = ContextoPruebas.getUrlTimeStampServer(
                ContextoPruebas.getControlAcceso().getServerURL());
        urlDelegacionRepresentante = ContextoPruebas.
            getUrlrepresentativeDelegation(ContextoPruebas.getControlAcceso().getServerURL());        
    }
    
    
    @Override
    public Respuesta call() throws Exception {
        Respuesta respuesta = null;
        File file = new File(ContextoPruebas.getUserKeyStorePath(userNIF));
        KeyStore mockDnie = KeyStoreHelper.crearMockDNIe(userNIF, file,
                ContextoPruebas.getPrivateCredentialRaizAutoridad());
        logger.info("userNIF: " + userNIF + " mockDnie:" + file.getAbsolutePath());
        String delegationDataJSON = getDelegationDataJSON(representativeNIF);

        ActorConIP controlAcceso = ContextoPruebas.getControlAcceso();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        representativeDelegationSMIME = signedMailGenerator.genMimeMessage(
                userNIF, toUser, delegationDataJSON, ContextoPruebas.
                ASUNTO_MENSAJE_DELEGACION_REPRESENTANTE , null);        
        new TimeStampWorker(TIME_STAMP_WORKER, urlTimeStampServer,
                    this, representativeDelegationSMIME.getTimeStampRequest(TIMESTAMP_DNIe_HASH),
                    ContextoPruebas.getControlAcceso().getTimeStampCert()).execute();
        countDownLatch.await();
        return getResult();
    }
    
   private void processDocument(SMIMEMessageWrapper smimeDocument) {
        if(smimeDocument == null) return;
        try {
            X509Certificate serverCert = ContextoPruebas.getControlAcceso().getCertificate();
            
            MimeMessage mimeMessage = Encryptor.encryptSMIME(smimeDocument, serverCert);
            File document = File.createTempFile("EncryptedRepresentativeDelegation", "p7s");
            document.deleteOnExit();
            mimeMessage.writeTo(new FileOutputStream(document));
  
            new DocumentSenderWorker(ENVIAR_DOCUMENTO_FIRMADO_WORKER, document, 
                    Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE,
                    urlDelegacionRepresentante, this).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            countDownLatch.countDown();
        }
    }
    
    /**
     *{"operation":"", "representativeNif":"", "representativeName":"NombreDe11B ApellidoDe11B"}, "urlEnvioDocumento":"http://localhost:8080/SistemaVotacionControlAcceso/representative/userSelection", "nombreDestinatarioFirma":"Primer ControlAcceso", "asuntoMensajeFirmado":"Datos del representante seleccionado", "respuestaConRecibo":true, "urlTimeStampServer":"http://localhost:8080/SistemaVotacionControlAcceso/timeStamp", "urlServer":"http://192.168.2.45:8080/SistemaVotacionControlAcceso"}
     */
    public static String getDelegationDataJSON(String representativeNif) {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_SELECTION");
        map.put("representativeNif", representativeNif);
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
         " - user:" + userNIF + "' -> representative: '" + representativeNIF +"'" +
         " - worker: " + worker.getClass().getSimpleName()) ;
        switch(worker.getId()) {
            case ENVIAR_DOCUMENTO_FIRMADO_WORKER:
                respuesta = new Respuesta();
                respuesta.setCodigoEstado(worker.getStatusCode());
                if (Respuesta.SC_OK == worker.getStatusCode()) {
                    respuesta.setMensaje(userNIF);
                } else {
                    logger.debug("- showResult - ERROR DELEGANDO EN REPRESENTANTE");
                    respuesta.setMensaje(worker.getMessage());
                }
                countDownLatch.countDown();
                break;
            case TIME_STAMP_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        representativeDelegationSMIME.setTimeStampToken((TimeStampWorker)worker);
                        processDocument(representativeDelegationSMIME);
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


    private Respuesta getResult() {
        return respuesta;
    }
}