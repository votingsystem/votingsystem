package org.votingsystem.dto.indentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "Publickey")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublickeyDto {

    private String publicKeyPEM;
    private String sessionUUID;

    public PublickeyDto() { }

    public PublickeyDto(String publicKeyPEM, String sessionUUID) {
        this.publicKeyPEM = publicKeyPEM;
        this.sessionUUID = sessionUUID;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public PublickeyDto setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
        return this;
    }

    public String getSessionUUID() {
        return sessionUUID;
    }

    public void setSessionUUID(String sessionUUID) {
        this.sessionUUID = sessionUUID;
    }
}