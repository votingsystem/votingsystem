package org.sistemavotacion.test.simulation.launcher;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.SMIMESignedSenderWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.sistemavotacion.worker.VotingSystemWorkerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignatureClaimLauncher implements Callable<Respuesta>, 
        VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(SignatureClaimLauncher.class);

    public enum Worker implements VotingSystemWorkerType{
        SEND_DOCUMENT}
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); 
    
    private SMIMEMessageWrapper smimeMessage;
    private String nif;
    private String submitClaimsURL = null;
    private Long eventId = null;
    private Respuesta respuesta;
        
    public SignatureClaimLauncher (String nif, Long eventId)  throws Exception {
        this.nif = nif;
        this.eventId = eventId;
        submitClaimsURL = ContextoPruebas.INSTANCE.getUrlSubmitClaims();
    }
    
    
    @Override public Respuesta call() throws Exception {
        try {
            KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(nif);

            ActorConIP controlAcceso = Contexto.INSTANCE.getAccessControl();
            String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());

            String claimDataStr = getClaimDataJSON(eventId);
            String subject = ContextoPruebas.INSTANCE.getString("claimMsgSubject");
            
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    mockDnie, ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.PASSWORD.toCharArray(),
                    ContextoPruebas.DNIe_SIGN_MECHANISM);
            smimeMessage = signedMailGenerator.genMimeMessage(
                    nif, toUser, claimDataStr, subject, null);
            X509Certificate destinationCert = ContextoPruebas.INSTANCE.
                    getAccessControl().getCertificate();  
            new SMIMESignedSenderWorker(Worker.SEND_DOCUMENT, 
                smimeMessage, submitClaimsURL, null, destinationCert,this).execute();

            countDownLatch.await();
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta.appendErrorMessage(ex.getMessage());
        }
        return getResult();
    }
    
    public static String getClaimDataJSON(Long eventId) {
        Map map = new HashMap();
        map.put("operation", Operacion.Tipo.FIRMA_RECLAMACION_SMIME.toString());
        map.put("id", eventId);
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
        " - nif: " + nif + " - worker: " + worker.getClass().getSimpleName());
        respuesta = worker.getRespuesta();
        switch((Worker)worker.getType()) {
            case SEND_DOCUMENT:
                if (Respuesta.SC_OK == worker.getStatusCode()) {
                    respuesta.setMensaje(nif);
                } else respuesta.appendErrorMessage(nif);
                break;
            default:
                logger.debug("*** UNKNOWN WORKER:" + worker);
        }
        countDownLatch.countDown();
    }
    
    private Respuesta getResult() {
        return respuesta;
    }

}