package org.votingsystem.test.misc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class SendSMIME {

    private static final String receptor = "SendSMIMEReceptor";
    private static final String receptorURL = "http://localhost:8080/AccessControl/rest/eventVS/save";

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(SendSMIME.class);

        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
               "http://localhost:8080/AccessControl"),ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        Map<String, Object> dataMap = new ObjectMapper().readValue(
                responseVS.getMessage(), new TypeReference<HashMap<String, Object>>() {});
        ContextVS.getInstance().setDefaultServer(ActorVS.parse(dataMap));

        String uuid = UUID.randomUUID().toString();
        Map requestDataMap = new HashMap<>();
        requestDataMap.put("operation", "SendSMIME");
        requestDataMap.put("statusCode", ResponseVS.SC_OK);
        requestDataMap.put("message", "message from SendSMIME");
        requestDataMap.put("UUID", uuid);

        SignatureService superUserSignatureService = SignatureService
                .getUserVSSignatureService("07553172H", UserVS.Type.USER);
        UserVS fromUserVS = superUserSignatureService.getUserVS();

        String messageSubject = "SendSMIME test " + uuid;
        String contentStr = new ObjectMapper().writeValueAsString(requestDataMap);
        SMIMEMessage smimeMessage = superUserSignatureService.getSMIMETimeStamped(fromUserVS.getNif(),
                receptor, contentStr, messageSubject);
        responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                receptorURL);
        log.info("statusCode: " + responseVS.getStatusCode() + " - message: " + responseVS.getMessage());
        System.exit(0);
    }

}






