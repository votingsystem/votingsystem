package org.votingsystem.simulation;

import org.votingsystem.model.ResponseVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface SimulatorListener<T> {
    
    public void processResponse(ResponseVS<T> responseVS);
    
}
