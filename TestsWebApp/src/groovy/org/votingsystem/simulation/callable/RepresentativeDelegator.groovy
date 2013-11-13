package org.votingsystem.simulation.callable;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;

import org.votingsystem.model.ActorVS
import org.votingsystem.model.ResponseVS;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.web.json.JSONObject

import org.votingsystem.util.StringUtils;
import org.votingsystem.simulation.ApplicationContextHolder as ACH;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeDelegator implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(
            RepresentativeDelegator.class);
    
    private String userNIF;
    private String representativeNIF;
    private String urlService = null;
    private ResponseVS respuesta;
    
    public RepresentativeDelegator (String userNIF, 
            String representativeNIF) throws Exception {
        this.userNIF = userNIF;
        this.representativeNIF = representativeNIF;
        log.debug("userNIF: " + userNIF + " - representativeNIF: " + representativeNIF);
        urlService = SimulationContext.INSTANCE.getUrlrepresentativeDelegation();        
    }
    
    
    @Override public ResponseVS call() throws Exception {
        KeyStore mockDnie = SimulationContext.INSTANCE.crearMockDNIe(userNIF);
        String delegationDataJSON = getDelegationDataJSON(representativeNIF);

        ActorVS controlAcceso = SimulationContext.INSTANCE.getAccessControl();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                SimulationContext.DEFAULTS.END_ENTITY_ALIAS, 
                SimulationContext.PASSWORD.toCharArray(),
                SimulationContext.DNIe_SIGN_MECHANISM);
        
        String msgSubject = SimulationContext.INSTANCE.getString("representativeDelegationMsgSubject");
                
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(
                userNIF, toUser, delegationDataJSON, msgSubject, null);        
        
        X509Certificate destinationCert = SimulationContext.INSTANCE.
                    getAccessControl().getCertificate();
        SMIMESignedSender senderSender = new SMIMESignedSender(null, 
                smimeMessage, urlService, null, destinationCert);
        respuesta = senderSender.call();
        if (ResponseVS.SC_OK == respuesta.getStatusCode()) {
            respuesta.setMensaje(userNIF);
        } else {
            log.debug(respuesta.getMensaje());
        }
        return respuesta;
    }

    public static String getDelegationDataJSON(String representativeNif) {
        log.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_SELECTION");
        map.put("representativeNif", representativeNif);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }

}