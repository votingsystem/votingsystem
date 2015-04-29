package org.votingsystem.dto.voting;

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

    private TypeVS operation = TypeVS.REPRESENTATIVE_REVOKE;
    private String representativeNIF;

    public RepresentativeRevokeDto() {}

    public void validate() throws ExceptionVS {
        if(TypeVS.REPRESENTATIVE_REVOKE != operation) throw new ValidationExceptionVS(
                "ERROR - operation missmatch - expected: 'TypeVS.REPRESENTATIVE_REVOKE' - found:" + operation);
        if(representativeNIF == null) throw new ValidationExceptionVS("missing representative NIF");
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }


    public String getRepresentativeNIF() {
        return representativeNIF;
    }

    public void setRepresentativeNIF(String representativeNIF) {
        this.representativeNIF = representativeNIF;
    }

}
