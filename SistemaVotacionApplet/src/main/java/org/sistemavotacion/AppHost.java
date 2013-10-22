package org.sistemavotacion;

import org.sistemavotacion.modelo.Operacion;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface AppHost {
    
    public void sendMessageToHost(Operacion operacion);
    public Operacion getPendingOperation();
}
