package org.votingsystem.util;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyGeneratorVS;

import javax.mail.Header;
import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
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
        if(headers != null) encryptedDataMap.put("headers", Arrays.asList(headers));
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        encryptedDataMap.put("publicKey", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        byte[] encryptedRequestBytes = Encryptor.encryptToCMS(
                JSONSerializer.toJSON(encryptedDataMap).toString().getBytes(), deviceToCert);
        messageToDevice.put("encryptedMessage", new String(encryptedRequestBytes,"UTF-8"));
        JSONObject messageToServiceJSON = (JSONObject) JSONSerializer.toJSON(messageToDevice);
        return new RequestBundle(keyPair, messageToServiceJSON);
    }

    public static SMIMEMessage getSignResponse(RequestBundle request, JSONObject response) throws Exception {
        byte[] decryptedBytes = Encryptor.decryptMessage(response.getString("encryptedMessage").getBytes(),
                request.getKeyPair().getPrivate());
        JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(new String(decryptedBytes, "UTF-8"));
        if(ResponseVS.SC_OK == responseJSON.getInt("statusCode")) {
            byte[] smimeMessageBytes = Base64.getDecoder().decode(responseJSON.getString("smime").getBytes());
            SMIMEMessage smimeMessage = new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
            return smimeMessage;
        } else throw new ExceptionVS(responseJSON.getString("message"));
    }

    public static JSONObject getResponse(String sessionId, ResponseVS responseVS){
        JSONObject result = new JSONObject();
        result.put("operation", TypeVS.WEB_SOCKET_MESSAGE.toString());
        result.put("sessionId", sessionId);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("statusCode", responseVS.getStatusCode());
        jsonObject.put("message", responseVS.getMessage());
        if(responseVS.getType() != null) jsonObject.put("operation", responseVS.getType().toString());
        result.put("message", jsonObject);
        return result;
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
