package org.sistemavotacion.test.simulation;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface SimulatorListener<T> {
    
    public void updateSimulationData(T data);
    public void setSimulationResult(Simulator<T> simulator);
    
}
