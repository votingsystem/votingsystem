package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.util.SystemOperation;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaInfDto {


    public enum System {ELECTIONS, CURRENCY}

    private SystemOperation operation;
    private String subject;
    private String reason;
    private String data;
    private System system;

    public MetaInfDto() { }

    public MetaInfDto(String reason, String data) {
        this.reason = reason;
        this.data = data;
    }

    public String getReason() {
        return reason;
    }

    public MetaInfDto setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getData() {
        return data;
    }

    public MetaInfDto setData(String data) {
        this.data = data;
        return this;
    }

    public System getSystem() {
        return system;
    }

    public MetaInfDto setSystem(System system) {
        this.system = system;
        return this;
    }

    public SystemOperation getOperation() {
        return operation;
    }

    public MetaInfDto setOperation(SystemOperation operation) {
        this.operation = operation;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

}
