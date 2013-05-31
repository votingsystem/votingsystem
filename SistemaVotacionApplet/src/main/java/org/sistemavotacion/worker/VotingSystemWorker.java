package org.sistemavotacion.worker;

import org.sistemavotacion.modelo.Respuesta;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface VotingSystemWorker {
    
    public String getMessage();
    public int getId();
    public int getStatusCode();
    public Respuesta getRespuesta();
    
}
