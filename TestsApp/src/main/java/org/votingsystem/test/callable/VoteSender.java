package org.votingsystem.test.callable;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.crypto.CertificationRequestVS;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.util.crypto.VoteHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import static org.votingsystem.util.ContextVS.SIGNATURE_ALGORITHM;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class VoteSender implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(VoteSender.class.getName());
   
    private VoteHelper voteHelper;
        
    public VoteSender(VoteHelper voteHelper) throws Exception {
        this.voteHelper = voteHelper;
    }
    
    @Override public ResponseVS<VoteHelper> call() throws Exception {
        SignatureService signatureService = SignatureService.load(voteHelper.getNIF());
        AccessRequestDto accessRequestDto = voteHelper.getAccessRequest();
        CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(
                JSON.getMapper().writeValueAsBytes(accessRequestDto));
        CertificationRequestVS certificationRequest = CertificationRequestVS.getVoteRequest(SIGNATURE_ALGORITHM,
                ContextVS.PROVIDER, ContextVS.getInstance().getAccessControl().getServerURL(),
                accessRequestDto.getEventId(), voteHelper.getHashCertVSBase64());
        Map<String, Object> mapToSend = new HashMap<>();
        mapToSend.put(ContextVS.CSR_FILE_NAME, certificationRequest.getCsrPEM());
        mapToSend.put(ContextVS.ACCESS_REQUEST_FILE_NAME, PEMUtils.getPEMEncoded(cmsMessage.toASN1Structure()));
        ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                ContextVS.getInstance().getAccessControl().getAccessServiceURL());
        if (ResponseVS.SC_OK != responseVS.getStatusCode()) {
            return responseVS;
        } else {
            certificationRequest.initSigner(responseVS.getMessageBytes());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                cmsMessage = certificationRequest.signDataWithTimeStamp(
                        JSON.getMapper().writeValueAsBytes(voteHelper.getVote()));
                responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.VOTE,
                        ContextVS.getInstance().getControlCenter().getVoteServiceURL());
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    CMSSignedMessage voteReceipt = responseVS.getCMS();
                    voteHelper.setValidatedVote(voteReceipt);
                    //_ TODO _ validate receipt
                }
            }
            return responseVS.setData(voteHelper);
        }
    }

}