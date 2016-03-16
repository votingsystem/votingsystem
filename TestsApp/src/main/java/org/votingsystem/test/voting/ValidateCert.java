package org.votingsystem.test.voting;


import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.dto.CertValidationDto;
import org.votingsystem.model.Actor;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.AccessControl;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.logging.Logger;

public class ValidateCert {

    private static Logger log =  Logger.getLogger(ValidateCert.class.getName());


    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        ResponseVS responseVS = HttpHelper.getInstance().getData(Actor.getServerInfoURL(
                "https://192.168.1.5/AccessControl"), ContentType.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        Actor actor = ((ActorDto)responseVS.getMessage(ActorDto.class)).getActor();
        if(!(actor instanceof AccessControl)) throw new ExceptionVS("Expected access control but found " +
                actor.getType().toString());
        AccessControl accessControl = (AccessControl) actor;
        ContextVS.getInstance().setAccessControl((AccessControl) actor);
        CertValidationDto certValidationDto = CertValidationDto.validationRequest("1234s", "015d3c26550c160e");
        SignatureService superUserSignatureService = SignatureService.getUserSignatureService(
                "AccessControl_07553172H", User.Type.USER);
        CMSSignedMessage cmsMessage = superUserSignatureService.signDataWithTimeStamp(
                JSON.getMapper().writeValueAsBytes(certValidationDto));
        responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentType.JSON_SIGNED,
                accessControl.getUserCSRValidationServiceURL());
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        System.exit(0);
    }

}
