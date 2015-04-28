package org.votingsystem.test.callable;

import org.votingsystem.callable.AccessRequestDataSender;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.VoteVSHelper;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.StringUtils;

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

        AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(smimeMessage,
                accessRequestDto, voteVSHelper.getHashCertVSBase64());
        ResponseVS responseVS = accessRequestDataSender.call();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            CertificationRequestVS certificationRequest = (CertificationRequestVS) responseVS.getData();
            String voteDataStr = JSON.getMapper().writeValueAsString(voteVSHelper.getVote());
            smimeMessage = certificationRequest.getSMIME(voteVSHelper.getHashCertVSBase64(), toUser, voteDataStr,
                    "voteVSMsgSubject", null);
            SMIMESignedSender sender = new SMIMESignedSender(smimeMessage,
                    ContextVS.getInstance().getControlCenter().getVoteServiceURL(),
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                    ContentTypeVS.VOTE, certificationRequest.getKeyPair(),
                    ContextVS.getInstance().getControlCenter().getX509Certificate());
            responseVS = sender.call();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessage voteReceipt = responseVS.getSMIME();
                voteVSHelper.setValidatedVote(voteReceipt);
                //_ TODO _ validate receipt
            }
        }
        return responseVS.setData(voteVSHelper);
    }

}