package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.simulation.ContextService
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.StringUtils

import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.Callable

import static org.votingsystem.simulation.ContextService.*
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeDelegatorDataSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(RepresentativeDelegatorDataSender.class);
    
    private String userNIF;
    private String representativeNIF;
    private String serviceURL = null;
    private ResponseVS reponseVS;
    private ContextService contextService;
    
    public RepresentativeDelegatorDataSender(String userNIF, String representativeNIF, String serviceURL) throws Exception {
        this.contextService = contextService;
        this.userNIF = userNIF;
        this.serviceURL = serviceURL;
        this.representativeNIF = representativeNIF;
        this.contextService  = ApplicationContextHolder.getSimulationContext();
        logger.debug("userNIF: " + userNIF + " - representativeNIF: " + representativeNIF +
                " - serviceURL: " + serviceURL);
    }
    
    @Override public ResponseVS call() throws Exception {
        KeyStore mockDnie = contextService.generateTestDNIe(userNIF);
        String delegationDataJSON = getDelegationDataJSON(representativeNIF);

        ActorVS accessRequest = contextService.getAccessControl();
        String toUser = StringUtils.getCadenaNormalizada(accessRequest.getName());
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie,
                END_ENTITY_ALIAS, PASSWORD.toCharArray(), DNIe_SIGN_MECHANISM);

        String msgSubject = contextService.getMessage("representativeDelegationMsgSubject", null);
                
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(
                userNIF, toUser, delegationDataJSON, msgSubject, null);        
        
        X509Certificate destinationCert = contextService.getAccessControl().getX509Certificate();
        SMIMESignedSender senderSender = new SMIMESignedSender(smimeMessage, serviceURL, null, destinationCert);
        reponseVS = senderSender.call();
        if (ResponseVS.SC_OK == reponseVS.getStatusCode()) {
            reponseVS.setMessage(userNIF);
        }
        return reponseVS;
    }

    public static String getDelegationDataJSON(String representativeNif) {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_SELECTION");
        map.put("representativeNif", representativeNif);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }

}