package org.votingsystem.test.ui;

import javafx.concurrent.Task;
import org.votingsystem.test.util.SimulationData;

import java.util.logging.Logger;

;
;

/**
 * Created by jgzornoza on 19/10/14.
 */
public class SimulationDataTask extends Task {

    Logger log =  Logger.getLogger(SimulationDataTask.class.getSimpleName());

    SimulationData simulationData;

    public SimulationDataTask(SimulationData simulationData) {
        this.simulationData = simulationData;
    }

    @Override  protected Object call() throws Exception {
        while(simulationData.hasPendingRequest()) {
            updateProgress(simulationData.getNumRequestsCollected(), simulationData.getNumRequestsProjected());
            updateMessage(simulationData.getNumRequestsCollected() + "/" + simulationData.getNumRequestsProjected());
            Thread.sleep(1000);
        }
        return null;
    }
}
