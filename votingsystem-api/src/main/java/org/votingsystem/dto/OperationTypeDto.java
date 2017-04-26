package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.SystemOperation;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "Operation")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationTypeDto<T> {

    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private SystemOperation type;
    @JacksonXmlProperty(localName = "EntityId", isAttribute = true)
    private String entityId;

    public OperationTypeDto() {}

    public OperationTypeDto(SystemOperation type, String entityId) {
        this.type = type;
        this.entityId = entityId;
    }

    public SystemOperation getType() {
        return type;
    }

    @JsonIgnore
    public CurrencyOperation getCurrencyOperationType() {
        return CurrencyOperation.valueOf(type.toString());
    }

    public OperationTypeDto setType(SystemOperation type) {
        this.type = type;
        return this;
    }

    @JsonIgnore
    public <T> T getValue() {
        return (T) this.type;
    }

    @JsonIgnore
    public boolean isCurrencyOperation() {
        return (this.type instanceof CurrencyOperation);
    }

    @JsonIgnore
    public boolean isOperationType() {
        return (this.type instanceof OperationType);
    }

    public String getEntityId() {
        return entityId;
    }

    public OperationTypeDto setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public void validate(SystemOperation type, String entityId) throws ValidationException {
        if(this.entityId == null || this.type == null || !this.entityId.equals(entityId) ||
                !this.type.toString().equals(type.toString())) {
            throw new ValidationException("Expected Operation - type: " + type + " - entityId: " + entityId +
                    " - found: " + this.type + " - " + this.entityId);
        }
    }

}