package org.votingsystem.signature.util;

import org.bouncycastle.util.encoders.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**

 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AESParams {

    private Key key;
    private IvParameterSpec iv;

    public AESParams() throws NoSuchAlgorithmException {
        SecureRandom random = new SecureRandom();
        iv = new IvParameterSpec(random.generateSeed(16));
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(random);
        kg.init(256);
        key = kg.generateKey();
    }

    public Key getKey() {
        return key;
    }

    public IvParameterSpec getIV() {
        return iv;
    }

    public Map<String, String> toMap() throws UnsupportedEncodingException {
        Map jsonObject = new HashMap<>();
        jsonObject.put("key", new String(Base64.encode(key.getEncoded()), "UTF-8"));
        jsonObject.put("iv", new String(Base64.encode(iv.getIV()), "UTF-8"));
        return jsonObject;
    }

    public static AESParams load(Map<String, String> jsonObject) throws NoSuchAlgorithmException {
        AESParams aesParams = new AESParams();
        byte[] decodeKeyBytes = Base64.decode(jsonObject.get("key").getBytes());
        aesParams.key = new SecretKeySpec(decodeKeyBytes, 0, decodeKeyBytes.length, "AES");
        byte[] iv = Base64.decode(jsonObject.get("iv").getBytes());
        aesParams.iv = new IvParameterSpec(iv);
        return aesParams;
    }

}
