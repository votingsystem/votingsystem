package org.votingsystem.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyGeneratorVS;

import javax.mail.Header;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketUtils {

    private static Logger log = Logger.getLogger(WebSocketUtils.class);

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

    public static RequestBundle getSignRequest(Long deviceToId, String deviceToName, String deviceFromName,
            String toUser, String textToSign, String subject, String locale, X509Certificate deviceToCert,
            Header... headers) throws Exception {
        Map messageToDevice = new HashMap<>();
        messageToDevice.put("operation", TypeVS.MESSAGEVS_TO_DEVICE.toString());
        messageToDevice.put("deviceToId", deviceToId);
        messageToDevice.put("deviceToName", deviceToName);
        messageToDevice.put("locale", locale);
        Map encryptedDataMap =  new HashMap<>();
        encryptedDataMap.put("operation", TypeVS.MESSAGEVS_SIGN.toString());
        encryptedDataMap.put("deviceFromName", deviceFromName);
        encryptedDataMap.put("toUser", toUser);
        encryptedDataMap.put("textToSign", textToSign);
        encryptedDataMap.put("subject", subject);
        if(headers != null) {
            JSONArray headersArray = new JSONArray();
            for(Header header : headers) {
                if (header != null) {
                    JSONObject headerJSON = new JSONObject();
                    headerJSON.put("name", header.getName());
                    headerJSON.put("value", header.getValue());
                    headersArray.add(headerJSON);
                }
            }
            encryptedDataMap.put("headers", headersArray);
        }
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        encryptedDataMap.put("publicKey", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        byte[] encryptedRequestBytes = Encryptor.encryptToCMS(
                JSONSerializer.toJSON(encryptedDataMap).toString().getBytes(), deviceToCert);
        messageToDevice.put("encryptedMessage", new String(encryptedRequestBytes,"UTF-8"));
        JSONObject messageToServiceJSON = (JSONObject) JSONSerializer.toJSON(messageToDevice);
        return new RequestBundle(keyPair, messageToServiceJSON);
    }

    public static class RequestBundle {
        KeyPair keyPair;
        JSONObject messageToDevice;
        public RequestBundle(KeyPair keyPair, JSONObject messageToDevice) {
            this.keyPair = keyPair;
            this.messageToDevice = messageToDevice;
        }
        public KeyPair getKeyPair() {
            return keyPair;
        }
        public JSONObject getRequest() {
            return messageToDevice;
        }
    }

}
