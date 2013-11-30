package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator

import org.votingsystem.util.ApplicationContextHolder as ACH

import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.Callable
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ClaimSignedSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(ClaimSignedSender.class);
    
    private SMIMEMessageWrapper smimeMessage;
    private String nif;
    private String submitClaimsURL = null;
    private Long eventId = null;
    private ResponseVS responseVS;
        
    public ClaimSignedSender(String nif, Long eventId)  throws Exception {
        this.nif = nif;
        this.eventId = eventId;
        submitClaimsURL = ContextVS.getInstance().getAccessControl().getClaimServiceURL();
    }
    
    @Override public ResponseVS call() throws Exception {
        try {
            KeyStore mockDnie = ContextVS.getInstance().generateKeyStore(nif);
            ActorVS accessControl = ContextVS.getInstance().getAccessControl();
            String toUser = accessControl.getNameNormalized();
            String claimDataStr = getClaimDataStr(eventId);
            String subject = ContextVS.getInstance().getMessage("claimMsgSubject");
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, ContextVS.END_ENTITY_ALIAS,
                    ContextVS.PASSWORD.toCharArray(), ContextVS.DNIe_SIGN_MECHANISM);
            smimeMessage = signedMailGenerator.genMimeMessage(nif, toUser, claimDataStr, subject);
            X509Certificate destinationCert = ContextVS.getInstance().getAccessControl().getX509Certificate();
            SMIMESignedSender worker = new SMIMESignedSender(smimeMessage, submitClaimsURL,null, destinationCert);
            responseVS = worker.call();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                responseVS.setMessage(nif);
            } else responseVS.appendMessage(nif);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
			return new ResponseVS(ex.getMessage())
        }
        return responseVS;
    }
    
    private String getClaimDataStr(Long eventId) {
        Map map = new HashMap();
        map.put("operation", TypeVS.SMIME_CLAIM_SIGNATURE.toString());
        map.put("id", eventId);
        map.put("URL", ContextVS.getInstance().getAccessControl().getEventURL(eventId.toString()));
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }

}