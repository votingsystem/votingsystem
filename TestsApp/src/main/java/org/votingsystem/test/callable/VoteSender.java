package org.votingsystem.test.callable;

import org.votingsystem.callable.AccessRequestDataSender;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.VoteVSHelper;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.*;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class VoteSender implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(VoteSender.class.getSimpleName());
   
    private VoteVSHelper voteVSHelper;
        
    public VoteSender(VoteVSHelper voteVSHelper) throws Exception {
        this.voteVSHelper = voteVSHelper;
    }
    
    @Override public ResponseVS<VoteVSHelper> call() throws Exception {
        String smimeMessageSubject = "VoteSender Test - accessRequestMsgSubject";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(voteVSHelper.getNIF());
        String toUser = StringUtils.getNormalized(ContextVS.getInstance().getAccessControl().getName());
        AccessRequestDto accessRequestDto = voteVSHelper.getAccessRequest();
        String contentStr = JSON.getMapper().writeValueAsString(accessRequestDto);
        SMIMEMessage smimeMessage = signatureService.getSMIME(voteVSHelper.getNIF(), toUser, contentStr, smimeMessageSubject);

        ResponseVS responseVS = new AccessRequestDataSender(smimeMessage,
                accessRequestDto, voteVSHelper.getHashCertVSBase64()).call();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            CertificationRequestVS certificationRequest = (CertificationRequestVS) responseVS.getData();
            String voteDataStr = JSON.getMapper().writeValueAsString(voteVSHelper.getVote());
            smimeMessage = certificationRequest.getSMIME(voteVSHelper.getHashCertVSBase64(), toUser, voteDataStr,
                    "voteVSMsgSubject", null);
            smimeMessage = new MessageTimeStamper(smimeMessage,
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
            responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.VOTE,
                    ContextVS.getInstance().getControlCenter().getVoteServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessage voteReceipt = responseVS.getSMIME();
                voteVSHelper.setValidatedVote(voteReceipt);
                //_ TODO _ validate receipt
            }
        }
        return responseVS.setData(voteVSHelper);
    }

}