package org.votingsystem.test;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.votingsystem.dto.OperationTypeDto;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
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
