package org.sistemavotacion.test.simulation.callable;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.SMIMESignedSenderWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeDelegator implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(
            RepresentativeDelegator.class);
    
    private String userNIF;
    private String representativeNIF;
    private String urlService = null;
    private Respuesta respuesta;
    
    public RepresentativeDelegator (String userNIF, 
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
                
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(
                userNIF, toUser, delegationDataJSON, msgSubject, null);        
        
        X509Certificate destinationCert = ContextoPruebas.INSTANCE.
                    getAccessControl().getCertificate();
        SMIMESignedSenderWorker senderWorker = new SMIMESignedSenderWorker(null, 
                smimeMessage, urlService, null, destinationCert,null);
        senderWorker.execute();
        respuesta = senderWorker.get();
        if (Respuesta.SC_OK == senderWorker.getStatusCode()) {
            respuesta.setMensaje(userNIF);
        } else {
            logger.debug(senderWorker.getErrorMessage());
            respuesta.appendErrorMessage(senderWorker.getErrorMessage());
        }
        return respuesta;
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

}