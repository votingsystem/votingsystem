package org.votingsystem.test.voting;


import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.voting.CertValidationDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.logging.Logger;

public class ValidateCert {

    private static Logger log =  Logger.getLogger(ValidateCert.class.getName());


    public static void main(String[] args) throws Exception {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                "http://localhost:8080/AccessControl"), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        ActorVS actorVS = ((ActorVSDto)responseVS.getMessage(ActorVSDto.class)).getActorVS();
        if(!(actorVS instanceof AccessControlVS)) throw new ExceptionVS("Expected access control but found " +
                actorVS.getType().toString());
        AccessControlVS accessControlVS = (AccessControlVS)actorVS;
        ContextVS.getInstance().setAccessControl((AccessControlVS)actorVS);
        CertValidationDto certValidationDto = CertValidationDto.validationRequest(
                "7553172H", "355136056990149");
        SignatureService superUserSignatureService = SignatureService.getUserVSSignatureService(
                "07553172H", UserVS.Type.USER);
        UserVS fromUserVS = superUserSignatureService.getUserVS();
        String messageSubject = "ValidateCert test " + certValidationDto.getUUID();
        SMIMEMessage smimeMessage = superUserSignatureService.getSMIMETimeStamped(fromUserVS.getNif(),
                accessControlVS.getName(), JSON.getMapper().writeValueAsString(certValidationDto), messageSubject);
        responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                accessControlVS.getUserCSRValidationServiceURL());
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        System.exit(0);
    }
}
