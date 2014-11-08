package org.votingsystem.test.callable

import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.callable.AccessRequestDataSender
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CertificationRequestVS
import org.votingsystem.test.util.SignatureService
import org.votingsystem.util.StringUtils

import java.util.concurrent.Callable

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class VoteSender implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(VoteSender.class);
   
    private VoteVS voteVS;
    private String electorNIF;
        
    public VoteSender(VoteVS voteVS, String electorNIF) throws Exception {
        this.voteVS = voteVS;
        this.electorNIF = electorNIF;
    }
    
    @Override public ResponseVS call() {
        String smimeMessageSubject = "VoteSender Test - accessRequestMsgSubject";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(electorNIF)
        String toUser = StringUtils.getNormalized(ContextVS.getInstance().getAccessControl().getName());
        SMIMEMessage smimeMessage = signatureService.getSMIME(electorNIF, toUser,
                JSONSerializer.toJSON(voteVS.getAccessRequestDataMap()).toString(), smimeMessageSubject)

        AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(smimeMessage, voteVS);
        ResponseVS responseVS = accessRequestDataSender.call();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            CertificationRequestVS certificationRequest = responseVS.getData();
            smimeMessage = certificationRequest.getSMIME(voteVS.getHashCertVSBase64(), toUser,
                    JSONSerializer.toJSON(voteVS.getVoteDataMap()).toString(), "voteVSMsgSubject", null);
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
        responseVS.setData([voteVS:voteVS, userVS:new UserVS(electorNIF)])
        return responseVS;
    }

}