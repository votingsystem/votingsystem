package org.votingsystem.simulation.callable;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.votingsystem.simulation.ContextService;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.TypeVS;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.simulation.ApplicationContextHolder;
import org.votingsystem.util.StringUtils;

import org.votingsystem.simulation.ApplicationContextHolder as ACH;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ClaimSigner implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(
            ClaimSigner.class);
    
    private SMIMEMessageWrapper smimeMessage;
    private String nif;
    private String submitClaimsURL = null;
    private Long eventId = null;
    private ResponseVS responseVS;
	private ContextService contextService = null;
        
    public ClaimSigner (String nif, Long eventId)  throws Exception {
        this.nif = nif;
        this.eventId = eventId;
		contextService = ACH.getSimulationContext();
        submitClaimsURL = contextService.getAccessControl().getServerURL() + "/recolectorReclamacion";
    }
    
    @Override public ResponseVS call() throws Exception {
        try {
            KeyStore mockDnie = contextService.generateTestDNIe(nif);
            ActorVS controlAcceso = contextService.getAccessControl();
            String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
            String claimDataStr = getClaimDataJSON(eventId);
            String subject = contextService.getMessage("claimMsgSubject");
      
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    mockDnie, contextService.END_ENTITY_ALIAS, 
                    contextService.PASSWORD.toCharArray(),
                    contextService.DNIe_SIGN_MECHANISM);
            smimeMessage = signedMailGenerator.genMimeMessage(
                    nif, toUser, claimDataStr, subject, null);
            X509Certificate destinationCert = contextService.
                    getAccessControl().getCertificate();  
            SMIMESignedSender worker = new SMIMESignedSender(
                    null, smimeMessage, submitClaimsURL, 
                    null, destinationCert);
            responseVS = worker.call();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                responseVS.setMessage(nif);
            } else responseVS.appendErrorMessage(nif);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
			return new ResponseVS(ex.getMessage())
        }
        return responseVS;
    }
    
    public String getClaimDataJSON(Long eventId) {
        Map map = new HashMap();
        map.put("operation", TypeVS.SMIME_CLAIM_SIGNATURE.toString());
        map.put("id", eventId);
        map.put("URL", contextService.getAccessControl().getServerURL() + "/evento/" + eventId);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }

}