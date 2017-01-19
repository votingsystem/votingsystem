package org.votingsystem.client.backup;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public interface ValidatorListener<T> {
    
    public void processValidationEvent(ValidationEvent<T> validationEvent);
    
}
