package org.sistemavotacion.test.simulation;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface SimulatorListener<T> {
    
    public void setSimulationMessage(String message);
    public void setSimulationErrorMessage(String message);
    public void setSimulationResult(Simulator simulator, T data);
    
}
