package org.sistemavotacion.test.simulacion;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public interface SimulationListener<T> {
    
    public void setSimulationMessage(String message);
    public void setSimulationErrorMessage(String message);
    public void setSimulationResult(Simulator simulator, T data);
    
}
