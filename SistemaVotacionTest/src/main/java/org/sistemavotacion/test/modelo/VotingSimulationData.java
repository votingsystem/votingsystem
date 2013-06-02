package org.sistemavotacion.test.modelo;

import java.util.concurrent.atomic.AtomicLong;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.simulation.SimulatorData;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingSimulationData implements SimulatorData {
        
    private static Logger logger = LoggerFactory.getLogger(VotingSimulationData.class);
    
    public enum Error {ACCESS_REQUES, VOTING};
    
    private int statusCode = Respuesta.SC_ERROR_EJECUCION;
    private String message = null;
    private Error error = null;
    
    private Long begin;
    private Long finish;
    
    private String durationStr = null;
    
    private String accessControlURL = null;
    private String controlCenterURL = null;
    
    private Integer numberOfElectors = null;
    private Integer numberOfRequests = null;
    private Integer maxPendingResponses = null;
    
    private String htmlContent = null;
    
    private UserBaseData userBaseData = null;
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
     * @return the accessControl
     */
    public String getAccessControlURL() {
        return accessControlURL;
    }

    /**
     * @param accessControl the accessControl to set
     */
    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
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
        
    public static VotingSimulationData parse (String dataStr) {
        logger.debug("- parse");
        if(dataStr == null) return null;
        JSONObject dataJSON = (JSONObject)JSONSerializer.toJSON(dataStr);
        return parse(dataJSON);
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
   
    public static VotingSimulationData parse (JSONObject dataJSON) {
        logger.debug("- parse - json ");
        if(dataJSON == null) return null;
        VotingSimulationData simulationData = new VotingSimulationData();      
        if (dataJSON.containsKey("userBaseData")) {
            UserBaseData userBaseData = UserBaseData.parse(dataJSON.getJSONObject("userBaseData"));
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
        if (dataJSON.containsKey("numberOfRequests")) {
            simulationData.setNumberOfRequests(dataJSON.getInt("numberOfRequests"));
        }
        if (dataJSON.containsKey("maxPendingResponses")) {
            simulationData.setMaxPendingResponses(dataJSON.getInt("maxPendingResponses"));
        }
        return simulationData;
    }

    /**
     * @return the userBaseData
     */
    public UserBaseData getUserBaseData() {
        return userBaseData;
    }

    /**
     * @param userBaseData the userBaseData to set
     */
    public void setUserBaseData(UserBaseData userBaseData) {
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
    public Integer getNumberOfRequests() {
        return numberOfRequests;
    }

    /**
     * @param numberOfRequests the numberOfRequests to set
     */
    public void setNumberOfRequests(Integer numberOfRequests) {
        this.numberOfRequests = numberOfRequests;
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

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
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
        statusCode = Respuesta.SC_ERROR;
        this.error = error;
    }

    /**
     * @return the begin
     */
    public long getBegin() {
        return begin;
    }

    /**
     * @param begin the begin to set
     */
    public void setBegin(long begin) {
        this.begin = begin;
    }

    /**
     * @return the finish
     */
    public long getFinish() {
        return finish;
    }

    /**
     * @param finish the finish to set
     */
    public void setFinish(long finish) throws Exception{
        if(begin == null) throw new Exception("SIMULATION BEGIN NOT SET");
        long duration = System.currentTimeMillis() - begin;
        durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        this.finish = finish;
    }
    
    public String getDurationStr() {
        return durationStr;
    }
}
