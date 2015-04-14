package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.security.PublicKey;
import java.util.Base64;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptedMsgDto {

    private String publicKey;
    private String message;
    private String from;
    private String receiverCert;
    @JsonProperty("UUID")
    private String UUID;


    public EncryptedMsgDto() {}

    public static EncryptedMsgDto NEW(String from, PublicKey publicKey) {
        EncryptedMsgDto encryptedMsgDto = new EncryptedMsgDto();
        encryptedMsgDto.from = from;
        encryptedMsgDto.publicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        encryptedMsgDto.UUID = java.util.UUID.randomUUID().toString();
        return encryptedMsgDto;
    }

    public void validateResponse() {
        //PublicKey pk =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getReceiverCert() {
        return receiverCert;
    }

    public void setReceiverCert(String receiverCert) {
        this.receiverCert = receiverCert;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}

