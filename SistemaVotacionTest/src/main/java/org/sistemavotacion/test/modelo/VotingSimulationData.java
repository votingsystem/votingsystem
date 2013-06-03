package org.sistemavotacion.test.modelo;

import java.util.concurrent.atomic.AtomicLong;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.simulation.SimulationData;
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
    
    private Integer numberOfElectors = null;
    private Integer numRequestsProjected = null;
    private Integer maxPendingResponses = null;
    
    private String htmlContent = null;
    
    private UserBaseSimulationData userBaseData = null;
    private Evento evento = null;

    private static AtomicLong accessRequestERROR = new AtomicLong(0);
    private static AtomicLong accessRequestOK = new AtomicLong(0);
    
    private static AtomicLong votingRequestERROR = new AtomicLong(0);
    private static AtomicLong votingRequestOK = new AtomicLong(0);
    
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
     * @return the numberOfElectors
     */
    public Integer getNumberOfElectors() {
        return numberOfElectors;
    }

    /**
     * @param numberOfElectors the numberOfElectors to set
     */
    public void setNumberOfElectors(Integer numberOfElectors) {
        this.numberOfElectors = numberOfElectors;
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
     * @return the htmlContent
     */
    public String getHtmlContent() {
        return htmlContent;
    }

    /**
     * @param htmlContent the htmlContent to set
     */
    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    /**
     * @return the numberOfRequests
     */
    public Integer getNumberOfRequestsProjected() {
        return numRequestsProjected;
    }

    /**
     * @param numberOfRequests the numberOfRequests to set
     */
    public void setNumberOfRequestsProjected(Integer numRequestsProjected) {
        this.numRequestsProjected = numRequestsProjected;
    }

    /**
     * @return the maxPendingResponses
     */
    public Integer getMaxPendingResponses() {
        return maxPendingResponses;
    }

    /**
     * @param maxPendingResponses the maxPendingResponses to set
     */
    public void setMaxPendingResponses(Integer maxPendingResponses) {
        this.maxPendingResponses = maxPendingResponses;
    }

    /**
     * @return the evento
     */
    public Evento getEvento() {
        return evento;
    }

    /**
     * @param evento the evento to set
     */
    public void setEvento(Evento evento) {
        this.evento = evento;
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
               
    public static VotingSimulationData parse (String dataStr) {
        logger.debug("- parse");
        if(dataStr == null) return null;
        JSONObject dataJSON = (JSONObject)JSONSerializer.toJSON(dataStr);
        return parse(dataJSON);
    }
   
    public static VotingSimulationData parse (JSONObject dataJSON) {
        logger.debug("- parse - json ");
        if(dataJSON == null) return null;
        VotingSimulationData simulationData = new VotingSimulationData();      
        if (dataJSON.containsKey("userBaseData")) {
            UserBaseSimulationData userBaseData = UserBaseSimulationData.parse(dataJSON.getJSONObject("userBaseData"));
            simulationData.setUserBaseData(userBaseData);
        }  
        if (dataJSON.containsKey("accessControlURL")) {
            simulationData.setAccessControlURL(dataJSON.getString("accessControlURL"));
        }  
        if (dataJSON.containsKey("controlCenterURL")) {
            simulationData.setControlCenterURL(dataJSON.getString("controlCenterURL"));
        }  
        if (dataJSON.containsKey("htmlContent")) {
            simulationData.setHtmlContent(dataJSON.getString("htmlContent"));
        }
        if (dataJSON.containsKey("numRequestsProjected")) {
            simulationData.setNumberOfRequestsProjected(
                    dataJSON.getInt("numRequestsProjected"));
        }
        if (dataJSON.containsKey("maxPendingResponses")) {
            simulationData.setMaxPendingResponses(dataJSON.getInt("maxPendingResponses"));
        }
        return simulationData;
    }
}
