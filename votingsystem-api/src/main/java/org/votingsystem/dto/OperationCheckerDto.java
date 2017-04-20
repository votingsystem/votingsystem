package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationCheckerDto {

    @JacksonXmlProperty(localName = "Operation")
    private OperationTypeDto operation;

    public OperationCheckerDto() {}

    public OperationTypeDto getOperation() {
        return operation;
    }

    public OperationCheckerDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

}
