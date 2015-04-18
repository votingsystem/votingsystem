package org.votingsystem.client.backup;

import org.votingsystem.model.ResponseVS;

/**
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public interface ValidatorListener<T> {
    
    public void processValidationEvent(ResponseVS<T> validationEventResponse);
    
}
