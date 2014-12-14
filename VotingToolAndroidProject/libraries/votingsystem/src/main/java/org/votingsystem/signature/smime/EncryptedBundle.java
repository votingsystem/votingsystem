package org.votingsystem.signature.smime;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EncryptedBundle {

    private byte[] iv;
    private byte[] cipherText;
    private byte[] salt;

    public EncryptedBundle(byte[] cipherText, byte[] iv, byte[] salt) {
        this.iv = iv;
        this.cipherText = cipherText;
        this.salt = salt;
    }
    public byte[] getIV() { return iv; }
    public byte[] getCipherText() { return cipherText; }
    public byte[] getSalt() { return salt; }

    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("iv", Base64.encodeToString(iv, Base64.DEFAULT));
        result.put("salt", Base64.encodeToString(getSalt(), Base64.DEFAULT));
        result.put("cipherText", Base64.encodeToString(getCipherText(), Base64.DEFAULT));
        return result;
    }

    public static EncryptedBundle parse(JSONObject jsonObject) throws JSONException {
        byte[] iv = Base64.decode(jsonObject.getString("iv").getBytes(), Base64.DEFAULT);
        byte[] cipherText = Base64.decode(jsonObject.getString("cipherText").getBytes(), Base64.DEFAULT);
        byte[] salt = Base64.decode(jsonObject.getString("salt").getBytes(), Base64.DEFAULT);
        return new EncryptedBundle(cipherText, iv, salt);
    }

}