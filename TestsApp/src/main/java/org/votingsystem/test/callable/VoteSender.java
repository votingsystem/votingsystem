package org.votingsystem.test.callable;

import org.votingsystem.callable.AccessRequestDataSender;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.crypto.CertificationRequestVS;
import org.votingsystem.util.crypto.VoteVSHelper;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class VoteSender implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(VoteSender.class.getName());
   
    private VoteVSHelper voteVSHelper;
        
    public VoteSender(VoteVSHelper voteVSHelper) throws Exception {
        this.voteVSHelper = voteVSHelper;
    }
    
    @Override public ResponseVS<VoteVSHelper> call() throws Exception {
        SignatureService signatureService = SignatureService.genUserVSSignatureService(voteVSHelper.getNIF());
        AccessRequestDto accessRequestDto = voteVSHelper.getAccessRequest();
        CMSSignedMessage cmsMessage = signatureService.signData(JSON.getMapper().writeValueAsString(accessRequestDto));
        ResponseVS responseVS = new AccessRequestDataSender(cmsMessage,
                accessRequestDto, voteVSHelper.getHashCertVSBase64()).call();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            CertificationRequestVS certificationRequest = (CertificationRequestVS) responseVS.getData();
            cmsMessage = certificationRequest.signData(JSON.getMapper().writeValueAsString(voteVSHelper.getVote()));
            cmsMessage = new MessageTimeStamper(cmsMessage,
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
            responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.VOTE,
                    ContextVS.getInstance().getControlCenter().getVoteServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                CMSSignedMessage voteReceipt = responseVS.getCMS();
                voteVSHelper.setValidatedVote(voteReceipt);
                //_ TODO _ validate receipt
            }
        }
        return responseVS.setData(voteVSHelper);
    }

}