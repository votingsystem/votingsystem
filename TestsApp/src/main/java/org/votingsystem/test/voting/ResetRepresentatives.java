package org.votingsystem.test.voting;


import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.model.Actor;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.AccessControl;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class ResetRepresentatives {

    private static final Logger log = Logger.getLogger(ResetRepresentatives.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        ActorDto actorDto = HttpHelper.getInstance().getData(ActorDto.class,
                Actor.getServerInfoURL("https://192.168.1.5/AccessControl"), MediaType.JSON);
        AccessControl accessControl = (AccessControl) actorDto.getActor();
        ContextVS.getInstance().setAccessControl(accessControl);
        SignatureService superUserSignatureService = SignatureService.getUserSignatureService(
                "AccessControl_07553172H", User.Type.USER);
        Map dataMap = new HashMap<>();
        dataMap.put("UUID", UUID.randomUUID().toString());
        CMSSignedMessage cmsMessage = superUserSignatureService.signDataWithTimeStamp(
                JSON.getMapper().writeValueAsBytes(dataMap));
        ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentType.JSON_SIGNED,
                "https://192.168.1.5/AccessControl/rest/development/resetRepresentatives");
        log.info("result - responseVS: " + responseVS.getMessage());
        System.exit(0);
    }
}
