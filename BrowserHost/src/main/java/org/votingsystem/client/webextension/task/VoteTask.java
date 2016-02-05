package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.bouncycastle.util.encoders.Hex;
import org.votingsystem.callable.AccessRequestDataSender;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.VoteVSHelper;
import org.votingsystem.throwable.KeyStoreExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.Base64;
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
            VoteVSHelper voteVSHelper = VoteVSHelper.load(operationVS.getVoteVS());
            VoteVSDto voteVS = voteVSHelper.getVote();
            String toUser = voteVS.getEventURL();
            String msgSubject = ContextVS.getInstance().getMessage("accessRequestMsgSubject")  + voteVS.getEventVSId();
            AccessRequestDto accessRequestDto = voteVSHelper.getAccessRequest();
            SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(fromUser, toUser,
                    JSON.getMapper().writeValueAsString(accessRequestDto), password, msgSubject);
            responseVS = new AccessRequestDataSender(smimeMessage,
                    accessRequestDto, voteVS.getHashCertVSBase64()).call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {

                operationVS.processResult(responseVS);
                return null;
            }
            updateProgress(60, 100);
            CertificationRequestVS certificationRequest = (CertificationRequestVS) responseVS.getData();
            String textToSign = JSON.getMapper().writeValueAsString(voteVS); ;
            fromUser = voteVS.getHashCertVSBase64();
            msgSubject = ContextVS.getInstance().getMessage("voteVSSubject");
            smimeMessage = certificationRequest.getSMIME(fromUser, toUser, textToSign, msgSubject, null);
            updateProgress(70, 100);
            smimeMessage = new MessageTimeStamper(smimeMessage,
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
            responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.VOTE,
                    ContextVS.getInstance().getControlCenter().getVoteServiceURL());
            updateProgress(90, 100);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                voteVSHelper.setValidatedVote(responseVS.getSMIME());
                ResponseVS voteResponse = new ResponseVS(ResponseVS.SC_OK);
                voteResponse.setData(voteVSHelper);
                ContextVS.getInstance().addHashCertVSData(voteVS.getHashCertVSBase64(), voteResponse);
                String hashCertVSHex = new String(Hex.encode(voteVS.getHashCertVSBase64().getBytes()));
                Map responseMap = new HashMap<>();
                responseMap.put("statusCode", ResponseVS.SC_OK);
                responseMap.put("hashCertVSBase64", voteVS.getHashCertVSBase64());
                responseMap.put("hashCertVSHex", hashCertVSHex);
                responseMap.put("voteURL", ContextVS.getInstance().getAccessControl().getVoteStateServiceURL(hashCertVSHex));
                responseMap.put("voteVSReceipt", Base64.getEncoder().encodeToString(voteVSHelper.getValidatedVote().getBytes()));
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
