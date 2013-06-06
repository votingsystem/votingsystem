package org.sistemavotacion.test.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingSimulationData extends SimulationData {
        
    private static Logger logger = LoggerFactory.getLogger(VotingSimulationData.class);
    
    public enum Error {ACCESS_REQUES, VOTING};
    
    private Error error = null;
    private String controlCenterURL = null;
    private Integer numOfElectors = null;
    private UserBaseSimulationData userBaseData = null;

    private static AtomicLong accessRequestERROR = new AtomicLong(0);
    private static AtomicLong accessRequestOK = new AtomicLong(0);
    
    private static AtomicLong votingRequestERROR = new AtomicLong(0);
    private static AtomicLong votingRequestOK = new AtomicLong(0);
    
    public VotingSimulationData() {}
    
    public VotingSimulationData(SimulationData simulData) throws Exception {
        setAccessControlURL(simulData.getAccessControlURL());
        setEvento(simulData.getEvento());
        setBegin(simulData.getBegin());
        setFinish(simulData.getFinish());
        setMaxPendingResponses(simulData.getMaxPendingResponses());
        setMessage(simulData.getMessage());
        setBackupRequestEmail(simulData.getBackupRequestEmail());
        setNumHoursProjected(simulData.getNumHoursProjected());
        setNumMinutesProjected(simulData.getNumMinutesProjected());
        setNumRequestsProjected(simulData.getNumRequestsProjected());
        setNumSecondsProjected(simulData.getNumSecondsProjected());
    }
    
    public Long getNumVotingRequests() {
        return votingRequestERROR.get() + votingRequestOK.get();
    }
       
    public Long getNumVotingRequestsERROR() {
        return votingRequestERROR.get();
    }
    
    public Long getAndIncrementNumVotingRequestsERROR() {
        return votingRequestERROR.getAndIncrement();
    }
    
    public Long getNumVotingRequestsOK() {
        return votingRequestOK.get();
    }
        
    public Long getAndIncrementNumVotingRequestsOK() {
        return votingRequestOK.getAndIncrement();
    }
    
    public Long getNumVotingRequestsColected() {
        return (votingRequestERROR.get() + votingRequestOK.get() 
                + accessRequestERROR.get());
    }
    
    public Long getNumAccessRequests() {
        return accessRequestERROR.get() + accessRequestOK.get();
    }
    
    public Long getNumAccessRequestsERROR() {
        return accessRequestERROR.get();
    }
    
    public Long getAndIncrementNumAccessRequestsERROR() {
        return accessRequestERROR.getAndIncrement();
    }
        
    public Long getNumAccessRequestsOK() {
        return accessRequestOK.get();
    }
    
    public Long getAndIncrementNumAccessRequestsOK() {
        return accessRequestOK.getAndIncrement();
    }
    
    public Long getNumAccessRequestsColected() {
        return (accessRequestERROR.get() + accessRequestOK.get());
    }

    /**
     * @return the controlCenterURL
     */
    public String getControlCenterURL() {
        return controlCenterURL;
    }

    /**
     * @param controlCenterURL the controlCenterURL to set
     */
    public void setControlCenterURL(String controlCenterURL) {
        this.controlCenterURL = controlCenterURL;
    }
    
    /**
     * @return the numOfElectors
     */
    public Integer getNumOfElectors() {
        return numOfElectors;
    }

    /**
     * @param numOfElectors the numOfElectors to set
     */
    public void setNumOfElectors(Integer numOfElectors) {
        this.numOfElectors = numOfElectors;
    }

    /**
     * @return the userBaseData
     */
    public UserBaseSimulationData getUserBaseData() {
        return userBaseData;
    }

    /**
     * @param userBaseData the userBaseData to set
     */
    public void setUserBaseData(UserBaseSimulationData userBaseData) {
        this.userBaseData = userBaseData;
    }
    
    /**
     * @return the error
     */
    public Error getError() {
        return error;
    }

    /**
     * @param error the error to set
     */
    public void setError(Error error) {
        setStatusCode(Respuesta.SC_ERROR);
        this.error = error;
    }
               
    public static VotingSimulationData parse (String dataStr) throws Exception {
        logger.debug("- parse");
        if(dataStr == null) return null;
        JSONObject dataJSON = (JSONObject)JSONSerializer.toJSON(dataStr);
        return parse(dataJSON);
    }
   
    public static VotingSimulationData parse (JSONObject dataJSON) throws Exception {
        logger.debug("- parse - json ");
        if(dataJSON == null) return null;
        VotingSimulationData simulationData = new VotingSimulationData(
                SimulationData.parse(dataJSON));   
        if (dataJSON.containsKey("userBaseData")) {
            UserBaseSimulationData userBaseData = UserBaseSimulationData.parse(
                    dataJSON.getJSONObject("userBaseData"));
            simulationData.setUserBaseData(userBaseData);
        }   
        if (dataJSON.containsKey("controlCenterURL")) {
            simulationData.setControlCenterURL(dataJSON.getString("controlCenterURL"));
        } 
        if (dataJSON.containsKey("options")) {
            List<OpcionEvento> eventOptions = new ArrayList<OpcionEvento>();
            JSONArray jsonArray = dataJSON.getJSONArray("options");
            for (int i = 0; i< jsonArray.size(); i++) {
                OpcionEvento campo = new OpcionEvento();
                campo.setContenido(jsonArray.getString(i));
                eventOptions.add(campo);
            }
            simulationData.getEvento().setOpciones(eventOptions);
        } 
        return simulationData;
    }
}
