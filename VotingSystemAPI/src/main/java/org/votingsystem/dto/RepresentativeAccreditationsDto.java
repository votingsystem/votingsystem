package org.votingsystem.dto;

import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.TypeVS;

import java.util.Date;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeAccreditationsDto {

    private TypeVS operation;
    private Date selectedDate;
    private String representativeNif;
    private String email;
    private String UUID;

    public RepresentativeAccreditationsDto() {}

    public void validate() throws ExceptionVS {
        if(TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST != operation) throw new ValidationExceptionVS(
                format("ERROR - operation missmatch - expected: {0} - found: {1}",
                        TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST, operation));
        representativeNif =  NifUtils.validate(representativeNif);
        if(email == null) throw new ValidationExceptionVS("missing param 'email'");
        if(selectedDate == null) throw new ValidationExceptionVS("missing param 'selectedDate'");
        if(UUID == null) throw new ValidationExceptionVS("missing param 'UUID'");
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
}
