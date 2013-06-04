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
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.SMIMESignedSenderWorker;
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
    
    private static Logger logger = LoggerFactory.getLogger(
            RepresentativeDelegationLauncher.class);

    private static final int SEND_DOCUMENT_WORKER = 0;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    private SMIMEMessageWrapper smimeMessage;
    private String userNIF;
    private String representativeNIF;
    private String urlService = null;
    private Respuesta respuesta;
    
    public RepresentativeDelegationLauncher (String userNIF, 
            String representativeNIF) throws Exception {
        this.userNIF = userNIF;
        this.representativeNIF = representativeNIF;
        logger.debug("userNIF: " + userNIF + " - representativeNIF: " + representativeNIF);
        urlService = ContextoPruebas.INSTANCE.getUrlrepresentativeDelegation();        
    }
    
    
    @Override public Respuesta call() throws Exception {
        KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(userNIF);
        String delegationDataJSON = getDelegationDataJSON(representativeNIF);

        ActorConIP controlAcceso = ContextoPruebas.INSTANCE.getAccessControl();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        
        String msgSubject = ContextoPruebas.INSTANCE.getString("representativeDelegationMsgSubject");
                
        smimeMessage = signedMailGenerator.genMimeMessage(
                userNIF, toUser, delegationDataJSON, msgSubject, null);        
        
        X509Certificate destinationCert = ContextoPruebas.INSTANCE.
                    getAccessControl().getCertificate();
        new SMIMESignedSenderWorker(SEND_DOCUMENT_WORKER, 
                smimeMessage, urlService, null, destinationCert,this).execute();

        countDownLatch.await();
        return getResult();
    }

    public static String getDelegationDataJSON(String representativeNif) {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_SELECTION");
        map.put("representativeNif", representativeNif);
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
         " - user:" + userNIF + "' -> representative: '" + representativeNIF +"'" +
         " - worker: " + worker.getClass().getSimpleName()) ;
        respuesta = worker.getRespuesta();
        switch(worker.getId()) {
            case SEND_DOCUMENT_WORKER:
                if (Respuesta.SC_OK == worker.getStatusCode()) {
                    respuesta.setMensaje(userNIF);
                } else {
                    logger.debug("- showResult - ERROR DELEGANDO EN REPRESENTANTE");
                    respuesta.appendErrorMessage("### ERROR - SEND_DOCUMENT_WORKER");
                }
                countDownLatch.countDown();
                break;
            default:
                logger.debug("*** UNKNOWN WORKER ID: '" + worker.getId() + "'");
        }
    }


    private Respuesta getResult() {
        return respuesta;
    }
}