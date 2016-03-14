package org.votingsystem.test.voting;


import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.CertValidationDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessControlVS;
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
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                "https://192.168.1.5/AccessControl"), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        ActorVS actorVS = ((ActorVSDto)responseVS.getMessage(ActorVSDto.class)).getActorVS();
        if(!(actorVS instanceof AccessControlVS)) throw new ExceptionVS("Expected access control but found " +
                actorVS.getType().toString());
        AccessControlVS accessControlVS = (AccessControlVS)actorVS;
        ContextVS.getInstance().setAccessControl((AccessControlVS)actorVS);
        CertValidationDto certValidationDto = CertValidationDto.validationRequest("1234s", "015d3c26550c160e");
        SignatureService superUserSignatureService = SignatureService.getUserVSSignatureService(
                "AccessControl_07553172H", UserVS.Type.USER);
        CMSSignedMessage cmsMessage = superUserSignatureService.signDataWithTimeStamp(
                JSON.getMapper().writeValueAsBytes(certValidationDto));
        responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                accessControlVS.getUserCSRValidationServiceURL());
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        System.exit(0);
    }
}
