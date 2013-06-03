package org.sistemavotacion.test.simulation.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
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
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.FileMapLauncherWorker;
import org.sistemavotacion.worker.TimeStampWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentingRequestLauncher implements Callable<Respuesta>, 
        VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(RepresentingRequestLauncher.class);

    private static final int ENVIAR_DOCUMENTO_FIRMADO_WORKER = 0;
    private static final int TIME_STAMP_WORKER = 1;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    
    private SMIMEMessageWrapper representativeRequestSMIME;
    private String representativeNIF;
    
    private String urlTimeStampServer = null;
    private String urlAltaRepresentante = null;
    
    private File selectedImage;
    private Respuesta respuesta;
        
    public RepresentingRequestLauncher (String representativeNIF) 
            throws Exception {
        this.representativeNIF = representativeNIF;
        urlTimeStampServer = ContextoPruebas.INSTANCE.getUrlTimeStampServer();
        urlAltaRepresentante = ContextoPruebas.INSTANCE.getUrlRepresentativeService();
    }
    
    
    @Override
    public Respuesta call() throws Exception {
        Respuesta respuesta = null;
        KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(representativeNIF);
        selectedImage = File.createTempFile("representativeImage", ".png");
        logger.info(" - selectedImage.getAbsolutePath(): " + selectedImage.getAbsolutePath());
        selectedImage.deleteOnExit();
        selectedImage =   FileUtils.copyStreamToFile(getClass().
                getResourceAsStream("/images/Group_128x128.png"), selectedImage);
        byte[] imageFileBytes = FileUtils.getBytesFromFile(selectedImage);
        MessageDigest messageDigest = MessageDigest.getInstance(
                            Contexto.VOTING_DATA_DIGEST);
        byte[] resultDigest =  messageDigest.digest(imageFileBytes);
        String base64ResultDigestStr = new String(Base64.encode(resultDigest));
        String representativeDataJSON = getRepresentativeDataJSON(
                representativeNIF, base64ResultDigestStr);

        ActorConIP controlAcceso = ContextoPruebas.INSTANCE.getControlAcceso();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        
        String subject = ContextoPruebas.INSTANCE.getString("representativeRequestMsgSubject");

        representativeRequestSMIME = signedMailGenerator.genMimeMessage(
                representativeNIF, toUser, representativeDataJSON, subject , null);
        new TimeStampWorker(TIME_STAMP_WORKER, urlTimeStampServer,
                    this, representativeRequestSMIME.getTimeStampRequest(),
                    ContextoPruebas.INSTANCE.getControlAcceso().getTimeStampCert()).execute();
        countDownLatch.await();
        return getResult();
    }
    
   private void processDocument(SMIMEMessageWrapper smimeDocument) {
        if(smimeDocument == null) return;
        try {
            X509Certificate serverCert = ContextoPruebas.INSTANCE.getControlAcceso().getCertificate();
            Encryptor.encryptSMIME(smimeDocument, serverCert);
            Map<String, Object> fileMap = new HashMap<String, Object>();
            String representativeDataFileName = 
                    Contexto.REPRESENTATIVE_DATA_FILE_NAME + ":" + 
                    Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
            
            MimeMessage mimeMessage = Encryptor.encryptSMIME(smimeDocument, serverCert);
            File document = File.createTempFile("EncryptedRepresentativeRequest", "p7m");
            document.deleteOnExit();
            mimeMessage.writeTo(new FileOutputStream(document));
            
            fileMap.put(representativeDataFileName, document);
            fileMap.put(Contexto.IMAGE_FILE_NAME, selectedImage);
            new FileMapLauncherWorker(ENVIAR_DOCUMENTO_FIRMADO_WORKER, 
                    fileMap,urlAltaRepresentante, this).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            countDownLatch.countDown();
        }
    }

        
    public static String getRepresentativeDataJSON(String representativeNIF,
            String imageDigestStr) {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_DATA");
        map.put("representativeInfo", " --- contenido del representante -" + representativeNIF);
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
        " - representativeNIF: " + representativeNIF +
        " - worker: " + worker.getClass().getSimpleName());
        switch(worker.getId()) {
            case ENVIAR_DOCUMENTO_FIRMADO_WORKER:
                respuesta = new Respuesta();
                respuesta.setCodigoEstado(worker.getStatusCode());
                if (Respuesta.SC_OK == worker.getStatusCode()) {
                    respuesta.setMensaje(representativeNIF);
                } else {
                    logger.debug("showResult - ERROR DANDO DE ALTA REPRESENTANTE");
                    respuesta.setMensaje(worker.getMessage());
                }
                countDownLatch.countDown();
                break;
            case TIME_STAMP_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        representativeRequestSMIME.setTimeStampToken((TimeStampWorker)worker);
                        processDocument(representativeRequestSMIME);
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