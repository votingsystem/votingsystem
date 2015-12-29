package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.dto.voting.VoteVSCancelerDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.VoteVSHelper;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class CancelVoteTask extends Task<Void> {

    private static Logger log = Logger.getLogger(CancelVoteTask.class.getSimpleName());

    private char[] password;
    private OperationVS operationVS;

    public CancelVoteTask(OperationVS operationVS, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
    }

    @Override protected Void call() throws Exception {
        log.info("cancelVote");
        ResponseVS voteResponse = ContextVS.getInstance().getHashCertVSData(operationVS.getMessage());
        VoteVSHelper voteVSHelper = (VoteVSHelper) voteResponse.getData();
        VoteVSCancelerDto cancelerDto = voteVSHelper.getVoteCanceler();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(new SendSMIMETask(operationVS, JSON.getMapper().writeValueAsString(cancelerDto), password));
        ResponseVS responseVS = (ResponseVS) future.get();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            responseVS.setMessage(Base64.getEncoder().encodeToString(responseVS.getSMIME().getBytes()));
        }
        return null;
    }
}
