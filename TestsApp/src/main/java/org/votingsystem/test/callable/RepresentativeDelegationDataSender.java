package org.votingsystem.test.callable;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.PEMUtils;

import java.security.cert.X509Certificate;
import java.util.Collection;
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
        SignatureService signatureService = SignatureService.load(userNIF);
        RepresentativeDelegationDto anonymousDelegation = getDelegationDto(representativeNIF);
        RepresentativeDelegationDto anonymousCertRequest = anonymousDelegation.getAnonymousCertRequest();
        RepresentativeDelegationDto anonymousDelegationRequest = anonymousDelegation.getDelegation();
        ResponseVS responseVS = null;
        try {
            CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(
                    JSON.getMapper().writeValueAsBytes(anonymousCertRequest));
            anonymousDelegation.setAnonymousDelegationRequestBase64ContentDigest(cmsMessage.getContentDigestStr());

            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CSR_FILE_NAME, anonymousDelegation.getCertificationRequest().getCsrPEM());
            mapToSend.put(ContextVS.CMS_FILE_NAME, cmsMessage.toPEM());
            responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ContextVS.getInstance().getAccessControl().getAnonymousDelegationRequestServiceURL());
            if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;

            anonymousDelegation.getCertificationRequest().initSigner(responseVS.getMessageBytes());
            //this is the delegation request signed with anonymous cert

            Collection<X509Certificate> certificates = PEMUtils.fromPEMToX509CertCollection(responseVS.getMessageBytes());
            cmsMessage = anonymousDelegation.getCertificationRequest().signDataWithTimeStamp(
                    JSON.getMapper().writeValueAsBytes(anonymousDelegationRequest));
            responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                    ContextVS.getInstance().getAccessControl().getAnonymousDelegationServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                anonymousDelegation.setDelegationReceipt(responseVS.getCMS(),
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
        UserDto representative = new UserDto();
        representative.setNIF(representativeNIF);
        RepresentativeDelegationDto delegationDto = new RepresentativeDelegationDto();
        delegationDto.setServerURL(ContextVS.getInstance().getAccessControl().getServerURL());
        delegationDto.setRepresentative(representative);
        delegationDto.setWeeksOperationActive(1);
        delegationDto.setOperation(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
        delegationDto.setUUID(UUID.randomUUID().toString());
        return delegationDto;
    }

}