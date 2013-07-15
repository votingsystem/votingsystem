package org.sistemavotacion.herramientavalidacion.backup;

import org.sistemavotacion.modelo.Respuesta;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface ValidatorListener<T> {
    
    public void processValidationEvent(Respuesta<T> validationEventResponse);
    
}
