package org.votingsystem.test.callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class ClaimSignedSender implements Callable<ResponseVS> {

    private String nif;
    private Long eventId = null;

    public ClaimSignedSender(String nif, Long eventId)  throws Exception {
        this.nif = nif;
        this.eventId = eventId;
    }
    
    @Override public ResponseVS call() throws Exception {
        String subject = "claimMsgSubject";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(this.nif);
        String contentStr = new ObjectMapper().writeValueAsString(getRequestMap(eventId));
        SMIMEMessage smimeMessage = signatureService.getSMIME(nif, ContextVS.getInstance().getAccessControl().getName(),
                contentStr, subject);
        X509Certificate destinationCert = ContextVS.getInstance().getAccessControl().getX509Certificate();
        SMIMESignedSender worker = new SMIMESignedSender(smimeMessage,
                ContextVS.getInstance().getAccessControl().getClaimServiceURL(),
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED, null, destinationCert);
        ResponseVS responseVS = worker.call();
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) responseVS.setMessage(nif);
        return responseVS;
    }
    
    private Map getRequestMap(Long eventId) {
        Map map = new HashMap();
        map.put("operation", TypeVS.SMIME_CLAIM_SIGNATURE.toString());
        map.put("id", eventId);
        map.put("URL", ContextVS.getInstance().getAccessControl().getEventURL(eventId.toString()));
        map.put("UUID", UUID.randomUUID().toString());
        return map;
    }

}