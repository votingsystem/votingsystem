package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AESParamsDto {

    private String key;
    private String iv;
    @JsonIgnore private Key aesKey;
    @JsonIgnore private IvParameterSpec ivParam;

    public AESParamsDto() { }

    public AESParamsDto(Key aesKey, IvParameterSpec ivParam) {
        this.aesKey = aesKey;
        this.ivParam = ivParam;
    }

    public static AESParamsDto CREATE() throws NoSuchAlgorithmException {
        AESParamsDto result = new AESParamsDto();
        SecureRandom random = new SecureRandom();
        result.ivParam = new IvParameterSpec(random.generateSeed(16));
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(random);
        kg.init(256);
        result.aesKey = kg.generateKey();
        return result;
    }

    @JsonIgnore public Key getAesKey() {
        if(aesKey == null && key != null) {
            byte[] decodeKeyBytes = Base64.getDecoder().decode(key.getBytes());
            aesKey = new SecretKeySpec(decodeKeyBytes, 0, decodeKeyBytes.length, "AES");
        }
        return aesKey;
    }

    @JsonIgnore public IvParameterSpec getIvParam() {
        if(ivParam == null && iv != null) ivParam = new IvParameterSpec(Base64.getDecoder().decode(iv.getBytes()));
        return ivParam;
    }

    public String getKey() {
        if(key == null && aesKey != null) key = Base64.getEncoder().encodeToString(aesKey.getEncoded());
        return key;
    }

    public String getIv() {
        if(iv == null && ivParam != null) iv = Base64.getEncoder().encodeToString(ivParam.getIV());
        return iv;
    }

}