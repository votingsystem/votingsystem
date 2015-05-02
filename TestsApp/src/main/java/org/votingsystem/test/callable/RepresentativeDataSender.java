package org.votingsystem.test.callable;


import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.*;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class RepresentativeDataSender implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(RepresentativeDataSender.class.getSimpleName());

    private String representativeNIF;
    private byte[] imageBytes;

    public RepresentativeDataSender(String representativeNIF, byte[] imageBytes) throws Exception {
        this.representativeNIF = representativeNIF;
        this.imageBytes = imageBytes;
    }
    
    @Override  public ResponseVS call() throws Exception {
        String subject = "Message from RepresentativeTestDataSender";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(representativeNIF);
        SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(representativeNIF,
                ContextVS.getInstance().getAccessControl().getName(), JSON.getMapper().writeValueAsString(
                        getRequest(representativeNIF, imageBytes)), subject);
        ResponseVS responseVS =  HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                ContextVS.getInstance().getAccessControl().getRepresentativeServiceURL());
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) responseVS.setMessage(representativeNIF);
        return responseVS;
    }
    
    private UserVSDto getRequest(String representativeNIF, byte[] imageBytes) {
        UserVSDto dto = new UserVSDto();
        dto.setOperation(TypeVS.NEW_REPRESENTATIVE);
        String representativeDescription = " --- data about the representative -" + representativeNIF;
        dto.setDescription(Base64.getEncoder().encodeToString(representativeDescription.getBytes()));
        dto.setBase64Image(Base64.getEncoder().encodeToString(imageBytes));
        dto.setUUID(UUID.randomUUID().toString());
        return dto;
    }

}