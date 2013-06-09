package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.modelo.SimulationData;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public abstract class Simulator<T extends SimulationData> 
        implements Callable<T> {
    
    private static Logger logger = LoggerFactory.getLogger(Simulator.class);
    
    protected List<String> errorList;
    
    public abstract T call() throws Exception;
        
    protected T simulationData = null;
    public Timer timer = null;
    
    public Simulator(T simulationData) {
        this.simulationData = simulationData;
    }
    
    public void startTimer(ActionListener actionListener) throws Exception {
        if(simulationData.isTimerBased()) {
            Long hoursMillis = 1000 * 60 * 60 * new Long(
                    simulationData.getNumHoursProjected());
            Long minutesMillis = 1000 * 60 * new Long(
                    simulationData.getNumMinutesProjected()); 
            Long secondMillis = 1000 * new Long(
                    simulationData.getNumSecondsProjected());      
            Long totalMillis = hoursMillis + minutesMillis + secondMillis;
            Long interval = totalMillis/simulationData.getNumRequestsProjected();
            logger.debug("starting timer - interval between requests: " 
                    + interval + " milliseconds");
            timer = new Timer(interval.intValue(), actionListener);
            timer.setRepeats(true);
            timer.start();
        } else throw new Exception("simulationData is not Timer based");
    }
    
    protected void addErrorMsg(String msg) {
        if(errorList == null) errorList = new ArrayList<String>();
        errorList.add(msg);
    }
    
    protected void addErrorList(List<String> errors) {
        if(errors == null || errors.isEmpty()) return;
        if(errorList == null) errorList = new ArrayList<String>();
        errorList.addAll(errors);
    }
    
    protected String getFormattedErrorList() {
        if(errorList == null || errorList.isEmpty()) return null;
        else {
            StringBuilder result = new StringBuilder("");
            for(String error:errorList) {
                result.append(error + "\n");
            }
            return result.toString();
        }
    }
           
    public String getFormattedErrorList(List<String> errorList) {
        if(errorList == null || errorList.isEmpty()) return null;
        else {
            StringBuilder result = new StringBuilder("");
            for(String error:errorList) {
                result.append(error + "\n");
            }
            return result.toString();
        }
    }
        
    public List<String> geterrorList() {
        return errorList;
    }
    
    
}
