package org.votingsystem.signature.util;

import net.sf.json.JSONObject;
import org.bouncycastle.util.encoders.Base64;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
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

    public JSONObject toJSON() throws UnsupportedEncodingException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", new String(Base64.encode(key.getEncoded()), "UTF-8"));
        jsonObject.put("iv", new String(Base64.encode(iv.getIV()), "UTF-8"));
        return jsonObject;
    }

    public static AESParams load(JSONObject jsonObject) throws NoSuchAlgorithmException {
        AESParams aesParams = new AESParams();
        byte[] decodeKeyBytes = Base64.decode(jsonObject.getString("key").getBytes());
        aesParams.key = new SecretKeySpec(decodeKeyBytes, 0, decodeKeyBytes.length, "AES");
        byte[] iv = Base64.decode(jsonObject.getString("iv").getBytes());
        aesParams.iv = new IvParameterSpec(iv);
        return aesParams;
    }

}
