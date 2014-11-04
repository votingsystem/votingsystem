package org.votingsystem.util;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketUtils {

    public static JSONObject getSignRequest(Long deviceIdTo, String deviceIdFrom, String deviceName,
            byte[] encryptedBytes, String locale) {
        Map messageToServiceMap = new HashMap<>();
        messageToServiceMap.put("locale", locale);
        messageToServiceMap.put("operation", TypeVS.MESSAGEVS_SIGN.toString());
        messageToServiceMap.put("deviceIdFrom", deviceIdFrom);
        messageToServiceMap.put("deviceIdTo", deviceIdTo);
        messageToServiceMap.put("deviceName", deviceName);
        messageToServiceMap.put("encryptedMessage", Base64.getEncoder().encodeToString(encryptedBytes));
        JSONObject messageToServiceJSON = (JSONObject) JSONSerializer.toJSON(messageToServiceMap);
        return messageToServiceJSON;
    }

    public static JSONObject getSignResponse(ResponseVS responseVS, JSONObject requestJSON){
        if(requestJSON.has("encryptedMessage")) requestJSON.remove("encryptedMessage");
        requestJSON.put("statusCode", responseVS.getStatusCode());
        requestJSON.put("message", responseVS.getMessage());
        return requestJSON;
    }

    public static JSONObject getResponse(ResponseVS responseVS){
        JSONObject result = new JSONObject();
        result.put("operation", TypeVS.WEB_SOCKET_MESSAGE.toString());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("statusCode", responseVS.getStatusCode());
        jsonObject.put("message", responseVS.getMessage());
        if(responseVS.getType() != null) jsonObject.put("operation", responseVS.getType().toString());
        result.put("message", jsonObject);
        return result;
    }


}
