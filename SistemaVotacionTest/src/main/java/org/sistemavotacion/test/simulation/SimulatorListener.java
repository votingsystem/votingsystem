package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.modelo.SimulationData;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface SimulatorListener<T extends SimulationData> {
    
    public void updateSimulationData(T data);
    public void setSimulationResult(Simulator<T> simulator);
    
}
