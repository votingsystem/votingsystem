package org.votingsystem.test.callable;

import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class RepresentativeDelegationDataSender implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(RepresentativeDelegationDataSender.class.getName());
    
    private String userNIF;
    private String representativeNIF;
    
    public RepresentativeDelegationDataSender(String userNIF, String representativeNIF) throws Exception {
        this.userNIF = userNIF;
        this.representativeNIF = representativeNIF;
    }
    
    @Override public ResponseVS call() throws Exception {
        String subject = "representativeDelegationMsgSubject";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(userNIF);
        String toUser = ContextVS.getInstance().getAccessControl().getName();
        String serviceURL = ContextVS.getInstance().getAccessControl().getAnonymousDelegationServiceURL();
        RepresentativeDelegationDto anonymousDelegation = getDelegationDto(representativeNIF);
        RepresentativeDelegationDto anonymousCertRequest = anonymousDelegation.getAnonymousCertRequest();
        RepresentativeDelegationDto anonymousDelegationRequest = anonymousDelegation.getDelegation();
        ResponseVS responseVS = null;
        try {
            SMIMEMessage smimeMessage = signatureService.getSMIME(userNIF, toUser, JSON.getMapper().writeValueAsString(
                    anonymousCertRequest), subject);
            smimeMessage = new MessageTimeStamper(smimeMessage,
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
            anonymousDelegation.setAnonymousDelegationRequestBase64ContentDigest(smimeMessage.getContentDigestStr());
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
            //this is the delegation request signed with anonymous cert
            smimeMessage = anonymousDelegation.getCertificationRequest().getSMIME(
                    anonymousDelegation.getHashCertVSBase64(),
                    ContextVS.getInstance().getAccessControl().getName(),
                    JSON.getMapper().writeValueAsString(anonymousDelegationRequest), subject, null);
            smimeMessage = new MessageTimeStamper(
                    smimeMessage, ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
            responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    ContextVS.getInstance().getAccessControl().getAnonymousDelegationServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                anonymousDelegation.setDelegationReceipt(responseVS.getSMIME(),
                        ContextVS.getInstance().getAccessControl().getX509Certificate());
                //BrowserSessionService.getInstance().setAnonymousDelegationDto(anonymousDelegation);
                responseVS = ResponseVS.OK();
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }

        return responseVS;
    }

    private RepresentativeDelegationDto getDelegationDto(String representativeNIF) {
        UserVSDto representative = new UserVSDto();
        representative.setNIF(representativeNIF);
        RepresentativeDelegationDto delegationDto = new RepresentativeDelegationDto();
        delegationDto.setServerURL(ContextVS.getInstance().getAccessControl().getServerURL());
        delegationDto.setRepresentative(representative);
        delegationDto.setOperation(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
        delegationDto.setUUID(UUID.randomUUID().toString());
        return delegationDto;
    }

}