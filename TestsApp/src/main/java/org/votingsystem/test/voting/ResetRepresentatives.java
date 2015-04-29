package org.votingsystem.test.voting;


import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class ResetRepresentatives {

    private static final Logger log = Logger.getLogger(ResetRepresentatives.class.getSimpleName());

    public static void main(String[] args) throws Exception {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        ActorVSDto actorVSDto = HttpHelper.getInstance().getData(ActorVSDto.class,
                ActorVS.getServerInfoURL("http://localhost:8080/AccessControl"), MediaTypeVS.JSON);
        AccessControlVS accessControlVS = (AccessControlVS) actorVSDto.getActorVS();
        ContextVS.getInstance().setAccessControl(accessControlVS);
        SignatureService superUserSignatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER);
        UserVS fromUserVS = superUserSignatureService.getUserVS();
        String messageSubject = "TEST_RESET_REPRESENTATIVES";
        Map dataMap = new HashMap<>();
        dataMap.put("UUID", UUID.randomUUID().toString());
        SMIMEMessage smimeMessage = superUserSignatureService.getSMIMETimeStamped(fromUserVS.getNif(),
                accessControlVS.getName(), JSON.getMapper().writeValueAsString(dataMap), messageSubject);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                "http://localhost:8080/AccessControl/rest/development/resetRepresentatives");
        log.info("result - responseVS: " + responseVS.getMessage());
        System.exit(0);
    }
}
