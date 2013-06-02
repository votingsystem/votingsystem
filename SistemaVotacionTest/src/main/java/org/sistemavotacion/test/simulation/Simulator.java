package org.sistemavotacion.test.simulation;

import java.util.List;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface Simulator<T>{
    
    public T getData();
    public T finish() throws Exception;
    public List<String> getErrorsList();
    
}
