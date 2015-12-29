package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnonymousDelegationTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(AnonymousDelegationTask.class.getSimpleName());

    private char[] password;
    private OperationVS operationVS;

    public AnonymousDelegationTask(OperationVS operationVS, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
    }

    @Override protected ResponseVS call() throws Exception {
        ResponseVS responseVS = null;
        String caption = operationVS.getCaption();
        if(caption.length() > 50) caption = caption.substring(0, 50) + "...";
        RepresentativeDelegationDto anonymousDelegation = operationVS.getData(RepresentativeDelegationDto.class);
        anonymousDelegation.setServerURL(ContextVS.getInstance().getAccessControl().getServerURL());
        RepresentativeDelegationDto anonymousCertRequest = anonymousDelegation.getAnonymousCertRequest();
        RepresentativeDelegationDto anonymousDelegationRequest = anonymousDelegation.getDelegation();
        try {
            SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null, ContextVS.getInstance().getAccessControl().
                            getName(), JSON.getMapper().writeValueAsString(anonymousCertRequest), password,
                    operationVS.getSignedMessageSubject());
            anonymousDelegation.setAnonymousDelegationRequestBase64ContentDigest(smimeMessage.getContentDigestStr());
            updateMessage(operationVS.getSignedMessageSubject());
            //byte[] encryptedCSRBytes = Encryptor.encryptMessage(certificationRequest.getCsrPEM(),destinationCert);
            //byte[] delegationEncryptedBytes = Encryptor.encryptSMIME(smimeMessage, destinationCert);
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CSR_FILE_NAME, anonymousDelegation.getCertificationRequest().getCsrPEM());
            mapToSend.put(ContextVS.SMIME_FILE_NAME, smimeMessage.getBytes());
            responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ContextVS.getInstance().getAccessControl().getAnonymousDelegationRequestServiceURL());
            if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            //byte[] decryptedData = Encryptor.decryptFile(responseVS.getMessageBytes(),
            // certificationRequest.getPublicKey(), certificationRequest.getPrivateKey());
            anonymousDelegation.getCertificationRequest().initSigner(responseVS.getMessageBytes());
            updateProgress(60, 100);
            //this is the delegation request signed with anonymous cert
            smimeMessage = anonymousDelegation.getCertificationRequest().getSMIME(
                    anonymousDelegation.getHashCertVSBase64(),
                    ContextVS.getInstance().getAccessControl().getName(),
                    JSON.getMapper().writeValueAsString(anonymousDelegationRequest),
                    operationVS.getSignedMessageSubject(), null);
            smimeMessage = new MessageTimeStamper(
                    smimeMessage, ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
            responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    ContextVS.getInstance().getAccessControl().getAnonymousDelegationServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                anonymousDelegation.setDelegationReceipt(responseVS.getSMIME(),
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
