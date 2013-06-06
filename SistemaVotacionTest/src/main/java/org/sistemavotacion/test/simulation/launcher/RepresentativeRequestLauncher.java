package org.sistemavotacion.test.simulation.launcher;

import java.io.File;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.RepresentativeRequestWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.sistemavotacion.worker.VotingSystemWorkerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeRequestLauncher implements Callable<Respuesta>, 
        VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(RepresentativeRequestLauncher.class);

    public enum Worker implements VotingSystemWorkerType{
        REPRESENTATIVE_REQUEST}
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private String representativeNIF;
    private File selectedImage;
    private Respuesta respuesta;
        
    public RepresentativeRequestLauncher (String representativeNIF) 
            throws Exception {
        this.representativeNIF = representativeNIF;
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

        ActorConIP controlAcceso = Contexto.INSTANCE.getAccessControl();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                mockDnie, ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        
        String subject = ContextoPruebas.INSTANCE.
                getString("representativeRequestMsgSubject");

        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(
                representativeNIF, toUser, representativeDataJSON, subject , null);
        
        String urlService = ContextoPruebas.INSTANCE.getUrlRepresentativeService();
        
        new RepresentativeRequestWorker(Worker.REPRESENTATIVE_REQUEST, smimeMessage,
                selectedImage, urlService, ContextoPruebas.INSTANCE.
                getAccessControl().getCertificate(),this).execute();
               
        countDownLatch.await();
        return getResult();
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

    @Override public void processVotingSystemWorkerMsg(List<String> messages) {
        for(String message : messages)  {
            logger.debug("process -> " + message);
        }
    }

    @Override
    public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
        " - representativeNIF: " + representativeNIF + " - worker: " + worker);
        respuesta = worker.getRespuesta();
        String msg = null;
        switch((Worker)worker.getType()) {
            case REPRESENTATIVE_REQUEST:
                if (Respuesta.SC_OK == worker.getStatusCode()) {
                    respuesta.setMensaje(representativeNIF);
                } else {
                    msg = "### ERROR - " + worker.getType() + " - msg: " + 
                        worker.getMessage();
                    respuesta.appendErrorMessage(msg);
                } 
                break;
            default:
                logger.debug("*** UNKNOWN WORKER: " + worker);
        }
        countDownLatch.countDown();
    }

    private Respuesta getResult() {
        return respuesta;
    }
    
}