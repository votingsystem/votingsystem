package org.votingsystem.model;

import org.votingsystem.model.OperationVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface AppHostVS {
    
    public void sendMessageToHost(OperationVS operacion);
    public OperationVS getPendingOperation();
}
