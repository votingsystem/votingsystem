package org.sistemavotacion.worker;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface VotingSystemWorker {
    
    public String getMessage();
    public String getErrorMessage();
    public int getStatusCode();
    public VotingSystemWorkerType getType();
}
