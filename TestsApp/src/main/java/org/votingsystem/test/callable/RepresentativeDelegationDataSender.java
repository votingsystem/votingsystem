package org.votingsystem.test.callable;

import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class RepresentativeDelegationDataSender implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(RepresentativeDelegationDataSender.class.getSimpleName());
    
    private String userNIF;
    private String representativeNIF;
    
    public RepresentativeDelegationDataSender(String userNIF, String representativeNIF) throws Exception {
        this.userNIF = userNIF;
        this.representativeNIF = representativeNIF;
    }
    
    @Override public ResponseVS call() throws Exception {
        String subject = "representativeDelegationMsgSubject";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(userNIF);
        String toUser = ContextVS.getInstance().getAccessControl().getName();
        String serviceURL = ContextVS.getInstance().getAccessControl().getDelegationServiceURL();
        SMIMEMessage smimeMessage = signatureService.getSMIME(userNIF, toUser, JSON.getMapper().writeValueAsString(
                getDelegationDto(representativeNIF)), subject);
        SMIMESignedSender senderSender = new SMIMESignedSender(smimeMessage, serviceURL,
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED, null, null);
        ResponseVS reponseVS = senderSender.call();
        if (ResponseVS.SC_OK == reponseVS.getStatusCode()) reponseVS.setMessage(userNIF);
        return reponseVS;
    }

    private RepresentativeDelegationDto getDelegationDto(String representativeNIF) {
        UserVSDto representative = new UserVSDto();
        representative.setNIF(representativeNIF);
        RepresentativeDelegationDto delegationDto = new RepresentativeDelegationDto();
        delegationDto.setRepresentative(representative);
        delegationDto.setOperation(TypeVS.REPRESENTATIVE_SELECTION);
        return delegationDto;
    }

}