package org.votingsystem.test.misc;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.CertValidationDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Test {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    private static void sendCMS() throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                "https://192.168.1.5/CurrencyServer"), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        ActorVS actorVS = ((ActorVSDto)responseVS.getMessage(ActorVSDto.class)).getActorVS();
        ContextVS.getInstance().setDefaultServer(actorVS);
        CertValidationDto certValidationDto = CertValidationDto.validationRequest("7553172H", "C8-0A-A9-AB-47-BE");
        SignatureService superUserSignatureService = SignatureService.getUserVSSignatureService(
                "Currency_07553172H", UserVS.Type.USER);
        CMSSignedMessage cmsMessage = superUserSignatureService.addSignatureWithTimeStamp(
                JSON.getMapper().writeValueAsString(certValidationDto));
        responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                "https://192.168.1.5/CurrencyServer/rest/test/testCMS");
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage() +
                " - ContentDigestStr: " + cmsMessage.getContentDigestStr());
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        //System.exit(0);
    }

}
