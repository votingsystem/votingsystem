package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.TypeVS;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepresentativeDelegationDto {

    private TypeVS operation;
    private String representativeNif;
    private String representativeName;
    private Integer weeksOperationActive;
    private String UUID;

    public RepresentativeDelegationDto() {}

    public RepresentativeDelegationDto(String representativeNif) {
        this.operation = TypeVS.REPRESENTATIVE_SELECTION;
        this.representativeNif = representativeNif;
        this.UUID = java.util.UUID.randomUUID().toString();
    }


    public void validate() throws ExceptionVS {
        if(TypeVS.REPRESENTATIVE_SELECTION != operation) throw new ValidationExceptionVS(
                format("ERROR - operation missmatch - expected: {0} - found: {1}",
                        TypeVS.REPRESENTATIVE_SELECTION, operation));
        representativeNif =  NifUtils.validate(representativeNif);
    }

    public TypeVS getOperation() {
        return operation;
    }


    public String getRepresentativeNif() {
        return representativeNif;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public void setRepresentativeNif(String representativeNif) {
        this.representativeNif = representativeNif;
    }

    public Integer getWeeksOperationActive() {
        return weeksOperationActive;
    }

    public void setWeeksOperationActive(Integer weeksOperationActive) {
        this.weeksOperationActive = weeksOperationActive;
    }

    public String getRepresentativeName() {
        return representativeName;
    }

    public void setRepresentativeName(String representativeName) {
        this.representativeName = representativeName;
    }
}
