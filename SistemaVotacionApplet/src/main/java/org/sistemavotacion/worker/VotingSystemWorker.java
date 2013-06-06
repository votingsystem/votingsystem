package org.sistemavotacion.worker;

import org.sistemavotacion.modelo.Respuesta;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface VotingSystemWorker {
    
    public String getMessage();
    public String getErrorMessage();
    public int getStatusCode();
    public Respuesta getRespuesta();
    public VotingSystemWorkerType getType();
}
