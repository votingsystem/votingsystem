package org.votingsystem.simulation;

import org.votingsystem.model.ResponseVS;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public interface SimulatorListener<T> {
    
    public void processResponse(ResponseVS<T> responseVS);
    
}
