package org.votingsystem.test.voting;


import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessControlVS;
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
        ActorVSDto actorVSDto = HttpHelper.getInstance().getData(ActorVSDto.class,
                ActorVS.getServerInfoURL("https://192.168.1.5/AccessControl"), MediaTypeVS.JSON);
        AccessControlVS accessControlVS = (AccessControlVS) actorVSDto.getActorVS();
        ContextVS.getInstance().setAccessControl(accessControlVS);
        SignatureService superUserSignatureService = SignatureService.getUserVSSignatureService(
                "AccessControl_07553172H", UserVS.Type.USER);
        Map dataMap = new HashMap<>();
        dataMap.put("UUID", UUID.randomUUID().toString());
        CMSSignedMessage cmsMessage = superUserSignatureService.signDataWithTimeStamp(
                JSON.getMapper().writeValueAsBytes(dataMap));
        ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                "https://192.168.1.5/AccessControl/rest/development/resetRepresentatives");
        log.info("result - responseVS: " + responseVS.getMessage());
        System.exit(0);
    }
}
