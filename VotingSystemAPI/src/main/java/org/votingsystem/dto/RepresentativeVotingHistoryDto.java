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
public class RepresentativeVotingHistoryDto {

    private TypeVS operation;
    private Date dateFrom;
    private Date dateTo;
    private String representativeNif;
    private String email;

    public RepresentativeVotingHistoryDto() {}


    public void validate() throws ExceptionVS {
        if(TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST != operation) throw new ValidationExceptionVS(
                format("ERROR - operation missmatch - expected: {0} - found: {1}",
                        TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST, operation));
        if(dateFrom.after(dateTo)) throw new ValidationExceptionVS(
                format("dateFrom '{0}' can no be after dateTo '{1}'", dateFrom, dateTo));
        representativeNif =  NifUtils.validate(representativeNif);
    }

    public TypeVS getOperation() {
        return operation;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public String getRepresentativeNif() {
        return representativeNif;
    }

    public String getEmail() {
        return email;
    }
}
