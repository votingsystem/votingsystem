package org.votingsystem.dto.indentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.util.OperationType;

import java.time.ZonedDateTime;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "IdentityRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class  IdentityRequestDto {

    @JacksonXmlProperty(localName = "Date", isAttribute = true)
    private ZonedDateTime date;
    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private OperationType type;
    @JacksonXmlProperty(localName = "RevocationHash")
    private String revocationHash;;
    @JacksonXmlProperty(localName = "IndentityServiceEntity")
    private SystemEntityDto indentityServiceEntity;
    @JacksonXmlProperty(localName = "CallbackServiceEntity")
    private SystemEntityDto callbackServiceEntityId;
    @JacksonXmlProperty(localName = "UUID")
    private String UUID;

    public  IdentityRequestDto() {}

    public IdentityRequestDto(OperationType type, String UUID, SystemEntityDto indentityServiceEntity) {
        this.type = type;
        this.UUID = UUID;
        this.indentityServiceEntity = indentityServiceEntity;
    }

    public OperationType getType() {
        return type;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public IdentityRequestDto setDate(ZonedDateTime date) {
        this.date = date;
        return this;
    }

    public SystemEntityDto getIndentityServiceEntity() {
        return indentityServiceEntity;
    }

    public IdentityRequestDto setIndentityServiceEntity(SystemEntityDto indentityServiceEntity) {
        this.indentityServiceEntity = indentityServiceEntity;
        return this;
    }

    public IdentityRequestDto setType(OperationType type) {
        this.type = type;
        return this;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public IdentityRequestDto setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
        return this;
    }

    public SystemEntityDto getCallbackServiceEntityId() {
        return callbackServiceEntityId;
    }

    public IdentityRequestDto setCallbackServiceEntityId(SystemEntityDto callbackServiceEntityId) {
        this.callbackServiceEntityId = callbackServiceEntityId;
        return this;
    }

    @JsonIgnore
    public String getUUID() {
        return UUID;
    }

    public IdentityRequestDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

}