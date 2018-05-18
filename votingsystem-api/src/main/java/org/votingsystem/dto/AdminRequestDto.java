package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.dto.metadata.KeyDto;
import org.votingsystem.util.OperationType;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "AdminRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminRequestDto {

    @JacksonXmlProperty(localName = "OperationType", isAttribute = true)
    private OperationType operationType;

    @JacksonXmlProperty(localName = "Key")
    private KeyDto key;

    @JacksonXmlProperty(localName = "EntityId")
    private String entityId;


    public AdminRequestDto(){}

    public AdminRequestDto(OperationType operationType){
        this.operationType = operationType;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public KeyDto getKey() {
        return key;
    }

    public AdminRequestDto setKey(KeyDto key) {
        this.key = key;
        return this;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}
