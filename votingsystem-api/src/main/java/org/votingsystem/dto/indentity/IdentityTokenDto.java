package org.votingsystem.dto.indentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.util.OperationType;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "IndentityToken")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentityTokenDto {

    public enum State {
        @JsonProperty("ok")
        ok,
        @JsonProperty("error")
        error
    }

    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private OperationType type;
    @JacksonXmlProperty(localName = "State", isAttribute = true)
    private State state;
    @JacksonXmlProperty(localName = "User")
    private UserDto user;
    @JacksonXmlProperty(localName = "IndentityServiceEntity")
    private SystemEntityDto indentityServiceEntity;
    @JacksonXmlProperty(localName = "RevocationHash")
    private String revocationHash;
    @JacksonXmlProperty(localName = "UUID")
    private String UUID;
    @JacksonXmlProperty(localName = "Base64Data")
    private String base64Data;

    public IdentityTokenDto() {}

    public IdentityTokenDto(OperationType type, SystemEntityDto indentityServiceEntity, String UUID) {
        this.type = type;
        this.indentityServiceEntity = indentityServiceEntity;
        this.UUID = UUID;
    }

    public OperationType getType() {
        return type;
    }

    public IdentityTokenDto setType(OperationType type) {
        this.type = type;
        return this;
    }

    @JsonIgnore
    public String getUUID() {
        return UUID;
    }

    public IdentityTokenDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public UserDto getUser() {
        return user;
    }

    public IdentityTokenDto setUser(UserDto user) {
        this.user = user;
        return this;
    }

    public SystemEntityDto getIndentityServiceEntity() {
        return indentityServiceEntity;
    }

    public IdentityTokenDto setIndentityServiceEntity(SystemEntityDto indentityServiceEntity) {
        this.indentityServiceEntity = indentityServiceEntity;
        return this;
    }

    public State getState() {
        return state;
    }

    public IdentityTokenDto setState(State state) {
        this.state = state;
        return this;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public IdentityTokenDto setBase64Data(String base64Data) {
        this.base64Data = base64Data;
        return this;
    }
}
