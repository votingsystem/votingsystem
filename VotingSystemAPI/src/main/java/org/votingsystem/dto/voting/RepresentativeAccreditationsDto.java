package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.TypeVS;

import java.util.Date;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepresentativeAccreditationsDto {

    private TypeVS operation;
    private Date selectedDate;
    private String representativeNif;
    private String email;
    @JsonProperty("UUID")
    private String UUID;

    public RepresentativeAccreditationsDto() {}

    public void validate() throws ExceptionVS {
        if(TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST != getOperation()) throw new ValidationExceptionVS(
                format("ERROR - operation missmatch - expected: {0} - found: {1}",
                        TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST, getOperation()));
        setRepresentativeNif(NifUtils.validate(getRepresentativeNif()));
        if(getEmail() == null) throw new ValidationExceptionVS("missing param 'email'");
        if(getSelectedDate() == null) throw new ValidationExceptionVS("missing param 'selectedDate'");
        if(getUUID() == null) throw new ValidationExceptionVS("missing param 'UUID'");
    }

    public TypeVS getOperation() {
        return operation;
    }

    public String getRepresentativeNif() {
        return representativeNif;
    }

    public String getEmail() {
        return email;
    }

    public Date getSelectedDate() {
        return selectedDate;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public void setSelectedDate(Date selectedDate) {
        this.selectedDate = selectedDate;
    }

    public void setRepresentativeNif(String representativeNif) {
        this.representativeNif = representativeNif;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
