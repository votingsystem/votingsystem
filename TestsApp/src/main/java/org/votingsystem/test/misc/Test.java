package org.votingsystem.test.misc;

import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.CertValidationDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
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

    public static void main(String[] args) throws Exception {
        sendSMIME();
    }

    private static void sendSMIME() throws Exception {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                "http://localhost:8080/CurrencyServer"), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        ActorVS actorVS = ((ActorVSDto)responseVS.getMessage(ActorVSDto.class)).getActorVS();
        ContextVS.getInstance().setDefaultServer(actorVS);
        CertValidationDto certValidationDto = CertValidationDto.validationRequest("7553172H", "C8-0A-A9-AB-47-BE");
        SignatureService superUserSignatureService = SignatureService.getUserVSSignatureService(
                "07553172H", UserVS.Type.USER);
        UserVS fromUserVS = superUserSignatureService.getUserVS();
        String messageSubject = "ValidateCert test " + certValidationDto.getUUID();
        SMIMEMessage smimeMessage = superUserSignatureService.getSMIMETimeStamped(fromUserVS.getNif(),
                actorVS.getName(), JSON.getMapper().writeValueAsString(certValidationDto), messageSubject);
        responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                "http://currency:8080/CurrencyServer/rest/test/testSMIME");
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage() +
                " - ContentDigestStr: " + smimeMessage.getContentDigestStr());
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        //System.exit(0);
    }

}
