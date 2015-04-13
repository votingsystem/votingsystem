package org.votingsystem.test.callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class RepresentativeDelegatorDataSender implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(RepresentativeDelegatorDataSender.class.getSimpleName());
    
    private String userNIF;
    private String representativeNIF;
    private String serviceURL = null;
    
    public RepresentativeDelegatorDataSender(String userNIF, String representativeNIF, String serviceURL) throws Exception {
        this.userNIF = userNIF;
        this.serviceURL = serviceURL;
        this.representativeNIF = representativeNIF;
    }
    
    @Override public ResponseVS call() throws Exception {
        String subject = "representativeDelegationMsgSubject";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(userNIF);
        String toUser = ContextVS.getInstance().getAccessControl().getName();
        SMIMEMessage smimeMessage = signatureService.getSMIME(userNIF, toUser, new ObjectMapper().writeValueAsString(
                new RepresentativeDelegationDto(representativeNIF)), subject);
        SMIMESignedSender senderSender = new SMIMESignedSender(smimeMessage, serviceURL,
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED, null, null);
        ResponseVS reponseVS = senderSender.call();
        if (ResponseVS.SC_OK == reponseVS.getStatusCode()) reponseVS.setMessage(userNIF);
        return reponseVS;
    }


}