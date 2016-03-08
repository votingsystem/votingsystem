package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnonymousDelegationTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(AnonymousDelegationTask.class.getName());

    private char[] password;
    private OperationVS operationVS;
    private String message;

    public AnonymousDelegationTask(OperationVS operationVS, String message, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
        this.message = message;
    }

    @Override protected ResponseVS call() throws Exception {
        updateMessage(message);
        ResponseVS responseVS = null;
        String caption = operationVS.getCaption();
        if(caption.length() > 50) caption = caption.substring(0, 50) + "...";
        RepresentativeDelegationDto anonymousDelegation = operationVS.getData(RepresentativeDelegationDto.class);
        anonymousDelegation.setServerURL(ContextVS.getInstance().getAccessControl().getServerURL());
        RepresentativeDelegationDto anonymousCertRequest = anonymousDelegation.getAnonymousCertRequest();
        RepresentativeDelegationDto anonymousDelegationRequest = anonymousDelegation.getDelegation();
        try {
            CMSSignedMessage cmsMessage = BrowserSessionService.getCMS(null, ContextVS.getInstance().getAccessControl().
                            getName(), JSON.getMapper().writeValueAsString(anonymousCertRequest), password,
                    operationVS.getSignedMessageSubject());
            anonymousDelegation.setAnonymousDelegationRequestBase64ContentDigest(cmsMessage.getContentDigestStr());
            updateMessage(operationVS.getSignedMessageSubject());
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CSR_FILE_NAME, anonymousDelegation.getCertificationRequest().getCsrPEM());
            mapToSend.put(ContextVS.CMS_FILE_NAME, cmsMessage.toPEM());
            responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ContextVS.getInstance().getAccessControl().getAnonymousDelegationRequestServiceURL());
            if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            anonymousDelegation.getCertificationRequest().initSigner(responseVS.getMessageBytes());
            updateProgress(60, 100);
            //this is the delegation request signed with anonymous cert
            cmsMessage = anonymousDelegation.getCertificationRequest().signData(
                    JSON.getMapper().writeValueAsString(anonymousDelegationRequest));
            cmsMessage = new MessageTimeStamper(
                    cmsMessage, ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
            responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                    ContextVS.getInstance().getAccessControl().getAnonymousDelegationServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                anonymousDelegation.setDelegationReceipt(responseVS.getCMS(),
                        ContextVS.getInstance().getAccessControl().getX509Certificate());
                BrowserSessionService.getInstance().setAnonymousDelegationDto(anonymousDelegation);
                responseVS = ResponseVS.OK();
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
        operationVS.processResult(responseVS);
        return responseVS;
    }
}
