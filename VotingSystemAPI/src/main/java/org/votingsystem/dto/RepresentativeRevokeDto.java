package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepresentativeRevokeDto {

    private TypeVS operation;

    public RepresentativeRevokeDto() {}

    public void validate(UserVS userVS) throws ExceptionVS {
        if(TypeVS.REPRESENTATIVE_REVOKE != operation) throw new ValidationExceptionVS(
                format("ERROR - operation missmatch - expected: {0} - found: {1}",
                        TypeVS.REPRESENTATIVE_REVOKE, operation));
        if(UserVS.Type.REPRESENTATIVE != userVS.getType()) {
            throw new ExceptionVS("ERROR - unsubscribeRepresentativeUserErrorMsg - user nif: " + userVS.getNif());
        }
    }

}
