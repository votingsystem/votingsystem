package org.votingsystem.test.callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.callable.AccessRequestDataSender;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.test.dto.VoteVSDto;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class VoteSender implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(VoteSender.class.getSimpleName());
   
    private VoteVS voteVS;
    private String electorNIF;
        
    public VoteSender(VoteVS voteVS, String electorNIF) throws Exception {
        this.voteVS = voteVS;
        this.electorNIF = electorNIF;
    }
    
    @Override public ResponseVS<VoteVSDto> call() throws Exception {
        String smimeMessageSubject = "VoteSender Test - accessRequestMsgSubject";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(electorNIF);
        String toUser = StringUtils.getNormalized(ContextVS.getInstance().getAccessControl().getName());
        String contentStr = new ObjectMapper().writeValueAsString(voteVS.getAccessRequestDataMap());
        SMIMEMessage smimeMessage = signatureService.getSMIME(electorNIF, toUser, contentStr, smimeMessageSubject);

        AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(smimeMessage, voteVS);
        ResponseVS responseVS = accessRequestDataSender.call();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            CertificationRequestVS certificationRequest = (CertificationRequestVS) responseVS.getData();
            String voteDataStr = new ObjectMapper().writeValueAsString(voteVS.getVoteDataMap());
            smimeMessage = certificationRequest.getSMIME(voteVS.getHashCertVSBase64(), toUser, voteDataStr, "voteVSMsgSubject", null);
            SMIMESignedSender sender = new SMIMESignedSender(smimeMessage,
                    ContextVS.getInstance().getControlCenter().getVoteServiceURL(),
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                    ContentTypeVS.VOTE, certificationRequest.getKeyPair(),
                    ContextVS.getInstance().getControlCenter().getX509Certificate());
            responseVS = sender.call();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessage voteReceipt = responseVS.getSMIME();
                voteVS.setReceipt(voteReceipt);
                //_ TODO _ validate receipt
            }
        }
        responseVS.setData(new VoteVSDto(voteVS, electorNIF));
        return responseVS;
    }

}