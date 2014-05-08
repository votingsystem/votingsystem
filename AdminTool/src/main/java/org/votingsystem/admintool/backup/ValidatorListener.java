package org.votingsystem.admintool.backup;

import org.votingsystem.model.ResponseVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface ValidatorListener<T> {
    
    public void processValidationEvent(ResponseVS<T> validationEventResponse);
    
}
