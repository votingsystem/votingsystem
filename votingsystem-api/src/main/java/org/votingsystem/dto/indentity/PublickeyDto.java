package org.votingsystem.dto.indentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.dto.AddressDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.UserDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "Publickey")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublickeyDto {

    private String publicKeyPEM;
    private String userUUID;

    public PublickeyDto() { }

    public PublickeyDto(String publicKeyPEM, String userUUID) {
        this.publicKeyPEM = publicKeyPEM;
        this.userUUID = userUUID;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public PublickeyDto setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
        return this;
    }

    public String getUserUUID() {
        return userUUID;
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }
}