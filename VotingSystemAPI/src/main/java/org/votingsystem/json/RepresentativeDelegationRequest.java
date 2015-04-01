package org.votingsystem.json;

import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.TypeVS;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeDelegationRequest {

    private TypeVS operation;
    private String representativeNif;

    public RepresentativeDelegationRequest() {}


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
}
