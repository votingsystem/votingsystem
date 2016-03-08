package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.KeyStoreExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnonymousDelegationCancelTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(AnonymousDelegationCancelTask.class.getName());

    private char[] password;
    private OperationVS operationVS;
    private String message;

    public AnonymousDelegationCancelTask(OperationVS operationVS, String message, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
        this.message = message;
    }

    @Override protected ResponseVS call() throws Exception {
        ResponseVS responseVS = null;
        try {
            updateMessage(message);
            RepresentativeDelegationDto delegation = BrowserSessionService.getInstance().getAnonymousDelegationDto();
            if(delegation == null) return new ResponseVS(ResponseVS.SC_ERROR,
                    ContextVS.getMessage("anonymousDelegationDataMissingMsg"));
            RepresentativeDelegationDto anonymousCancelationRequest = delegation.getAnonymousCancelationRequest();
            RepresentativeDelegationDto anonymousRepresentationDocumentCancelationRequest =
                    delegation.getAnonymousRepresentationDocumentCancelationRequest();
            CMSSignedMessage cmsMessage = BrowserSessionService.getCMS(null,
                    operationVS.getReceiverName(), JSON.getMapper().writeValueAsString(anonymousCancelationRequest),
                    password, operationVS.getSignedMessageSubject());
            CMSSignedMessage anonymousCMSMessage = delegation.getCertificationRequest().signData(
                    JSON.getMapper().writeValueAsString(anonymousRepresentationDocumentCancelationRequest));
            MessageTimeStamper timeStamper = new MessageTimeStamper(anonymousCMSMessage,
                    ContextVS.getInstance().getDefaultServer().getTimeStampServiceURL());
            anonymousCMSMessage = timeStamper.call();

            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CMS_FILE_NAME, cmsMessage.toPEM());
            mapToSend.put(ContextVS.CMS_ANONYMOUS_FILE_NAME, anonymousCMSMessage.toPEM());
            updateMessage(operationVS.getSignedMessageSubject());
            responseVS =  HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ContextVS.getInstance().getAccessControl().getAnonymousDelegationCancelerServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                CMSSignedMessage delegationReceipt = responseVS.getCMS();
                Collection matches = delegationReceipt.checkSignerCert(
                        ContextVS.getInstance().getAccessControl().getX509Certificate());
                if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
                responseVS.setCMS(delegationReceipt);
                responseVS.setMessage(ContextVS.getMessage("cancelAnonymousRepresentationOkMsg"));
            } else responseVS = new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("errorLbl"));
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
