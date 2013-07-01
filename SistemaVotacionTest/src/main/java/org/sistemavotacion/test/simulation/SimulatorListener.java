package org.sistemavotacion.test.simulation;

import org.sistemavotacion.modelo.Respuesta;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface SimulatorListener<T> {
    
    public void processResponse(Respuesta<T> respuesta);
    
}
