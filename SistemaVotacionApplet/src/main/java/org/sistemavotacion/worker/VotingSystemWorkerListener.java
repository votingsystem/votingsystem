package org.sistemavotacion.worker;

import java.util.List;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public interface VotingSystemWorkerListener {
    
    public void process(List<String> messages);
    public void showResult(VotingSystemWorker worker);
    
}