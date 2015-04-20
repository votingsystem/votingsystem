package org.votingsystem.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.signature.util.EncryptedBundle;

import java.util.Base64;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptedBundleDto {

    private Long id;
    private String iv;
    private String salt;
    private String cipherText;

    public EncryptedBundleDto() {}

    public EncryptedBundleDto(EncryptedBundle encryptedBundle) {
        iv = Base64.getEncoder().encodeToString(encryptedBundle.getIV());
        salt = Base64.getEncoder().encodeToString(encryptedBundle.getSalt());
        cipherText = Base64.getEncoder().encodeToString(encryptedBundle.getCipherText());
    }

    public EncryptedBundle getEncryptedBundle() {
        byte[] iv = Base64.getDecoder().decode(this.iv.getBytes());
        byte[] cipherText = Base64.getDecoder().decode(this.cipherText.getBytes());
        byte[] salt = Base64.getDecoder().decode(this.salt.getBytes());
        return new EncryptedBundle(cipherText, iv, salt);
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getCipherText() {
        return cipherText;
    }

    public void setCipherText(String cipherText) {
        this.cipherText = cipherText;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
