package org.sistemavotacion.worker;

import java.util.List;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface VotingSystemWorkerListener {
    
    public void process(List<String> messages);
    public void showResult(VotingSystemWorker worker);
    
}