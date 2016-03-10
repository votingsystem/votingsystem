package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.dto.voting.VoteCancelerDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.KeyStoreExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.crypto.VoteHelper;

import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CancelVoteTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(CancelVoteTask.class.getName());

    private char[] password;
    private OperationVS operationVS;
    private String message;

    public CancelVoteTask(OperationVS operationVS, String message, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
        this.message = message;
    }

    @Override protected ResponseVS call() throws Exception {
        log.info("cancelVote");
        ResponseVS responseVS = null;
        try {
            updateMessage(message);
            ResponseVS voteResponse = ContextVS.getInstance().getHashCertVSData(operationVS.getMessage());
            VoteHelper voteHelper = (VoteHelper) voteResponse.getData();
            VoteCancelerDto cancelerDto = voteHelper.getVoteCanceler();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future future = executor.submit(new SendCMSTask(operationVS, JSON.getMapper().writeValueAsString(cancelerDto),
                    message, password));
            responseVS = (ResponseVS) future.get();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                responseVS.setMessage(Base64.getEncoder().encodeToString(responseVS.getCMS().toPEM()));
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
        return responseVS;
    }
}
