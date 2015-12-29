package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AnonymousDelegationCancelTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(AnonymousDelegationCancelTask.class.getSimpleName());

    private char[] password;
    private OperationVS operationVS;

    public AnonymousDelegationCancelTask(OperationVS operationVS, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
    }

    @Override protected ResponseVS call() throws Exception {
        RepresentativeDelegationDto delegation = BrowserSessionService.getInstance().getAnonymousDelegationDto();
        if(delegation == null) return new ResponseVS(ResponseVS.SC_ERROR,
                ContextVS.getMessage("anonymousDelegationDataMissingMsg"));
        RepresentativeDelegationDto anonymousCancelationRequest = delegation.getAnonymousCancelationRequest();
        RepresentativeDelegationDto anonymousRepresentationDocumentCancelationRequest =
                delegation.getAnonymousRepresentationDocumentCancelationRequest();
        SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null,
                operationVS.getReceiverName(), JSON.getMapper().writeValueAsString(anonymousCancelationRequest),
                password, operationVS.getSignedMessageSubject());
        SMIMEMessage anonymousSmimeMessage = delegation.getCertificationRequest().getSMIME(delegation.getHashCertVSBase64(),
                ContextVS.getInstance().getAccessControl().getName(),
                JSON.getMapper().writeValueAsString(anonymousRepresentationDocumentCancelationRequest),
                operationVS.getSignedMessageSubject(), null);
        MessageTimeStamper timeStamper = new MessageTimeStamper(anonymousSmimeMessage,
                ContextVS.getInstance().getDefaultServer().getTimeStampServiceURL());
        anonymousSmimeMessage = timeStamper.call();

        Map<String, Object> mapToSend = new HashMap<>();
        mapToSend.put(ContextVS.SMIME_FILE_NAME, smimeMessage.getBytes());
        mapToSend.put(ContextVS.SMIME_ANONYMOUS_FILE_NAME, anonymousSmimeMessage.getBytes());
        updateMessage(operationVS.getSignedMessageSubject());
        ResponseVS responseVS =  HttpHelper.getInstance().sendObjectMap(mapToSend,
                ContextVS.getInstance().getAccessControl().getAnonymousDelegationCancelerServiceURL());
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            SMIMEMessage delegationReceipt = responseVS.getSMIME();
            Collection matches = delegationReceipt.checkSignerCert(
                    ContextVS.getInstance().getAccessControl().getX509Certificate());
            if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
            responseVS.setSMIME(delegationReceipt);
            responseVS.setMessage(ContextVS.getMessage("cancelAnonymousRepresentationOkMsg"));
        } else responseVS = new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("errorLbl"));
        operationVS.processResult(responseVS);
        return responseVS;
    }
}
