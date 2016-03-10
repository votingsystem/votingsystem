package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.bouncycastle.util.encoders.Hex;
import org.votingsystem.callable.AccessRequestDataSender;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.KeyStoreExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.crypto.CertificationRequestVS;
import org.votingsystem.util.crypto.VoteHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VoteTask extends Task<Void> {

    private static Logger log = Logger.getLogger(VoteTask.class.getName());

    private char[] password;
    private OperationVS operationVS;
    private String message;

    public VoteTask(OperationVS operationVS, String message, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
        this.message = message;
    }

    @Override protected Void call() throws Exception {
        log.info("sendVote");
        ResponseVS responseVS = null;
        try {
            updateMessage(message);
            String fromUser = ContextVS.getInstance().getMessage("electorLbl");
            VoteHelper voteHelper = VoteHelper.load(operationVS.getVote());
            VoteDto vote = voteHelper.getVote();
            String toUser = vote.getEventURL();
            String msgSubject = ContextVS.getInstance().getMessage("accessRequestMsgSubject")  + vote.getEventId();
            AccessRequestDto accessRequestDto = voteHelper.getAccessRequest();
            CMSSignedMessage cmsMessage = BrowserSessionService.getCMS(fromUser, toUser,
                    JSON.getMapper().writeValueAsString(accessRequestDto), password, msgSubject);
            responseVS = new AccessRequestDataSender(cmsMessage,
                    accessRequestDto, vote.getHashCertVSBase64()).call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {

                operationVS.processResult(responseVS);
                return null;
            }
            updateProgress(60, 100);
            CertificationRequestVS certificationRequest = (CertificationRequestVS) responseVS.getData();
            String textToSign = JSON.getMapper().writeValueAsString(vote); ;
            cmsMessage = certificationRequest.signData(textToSign);
            updateProgress(70, 100);
            cmsMessage = new MessageTimeStamper(cmsMessage,
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
            responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.VOTE,
                    ContextVS.getInstance().getControlCenter().getVoteServiceURL());
            updateProgress(90, 100);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                voteHelper.setValidatedVote(responseVS.getCMS());
                ResponseVS voteResponse = new ResponseVS(ResponseVS.SC_OK);
                voteResponse.setData(voteHelper);
                ContextVS.getInstance().addHashCertVSData(vote.getHashCertVSBase64(), voteResponse);
                String hashCertVSHex = new String(Hex.encode(vote.getHashCertVSBase64().getBytes()));
                Map responseMap = new HashMap<>();
                responseMap.put("statusCode", ResponseVS.SC_OK);
                responseMap.put("hashCertVSBase64", vote.getHashCertVSBase64());
                responseMap.put("hashCertVSHex", hashCertVSHex);
                responseMap.put("voteURL", ContextVS.getInstance().getAccessControl().getVoteStateServiceURL(hashCertVSHex));
                responseMap.put("voteReceipt", voteHelper.getValidatedVote().toPEMStr());
                responseVS.setMessage(JSON.getMapper().writeValueAsString(responseMap));
            }

        } catch (KeyStoreExceptionVS ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS = ResponseVS.ERROR(ex.getMessage());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS = ResponseVS.ERROR(ex.getMessage());
        } finally {
            operationVS.processResult(responseVS);
        }
        return null;
    }
}
