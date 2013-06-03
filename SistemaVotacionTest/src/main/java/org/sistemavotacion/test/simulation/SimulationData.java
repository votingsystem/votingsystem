package org.sistemavotacion.test.simulation;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SimulationData {
    
    private static Logger logger = LoggerFactory.getLogger(SimulationData.class);

    private int statusCode = Respuesta.SC_ERROR_EJECUCION;
    private String message = null;
    private String accessControlURL = null;
    private Integer maxPendingResponses = null;
    private Integer numRequestsProjected = null;

    private AtomicLong numRequests = new AtomicLong(0);
    private AtomicLong numRequestsOK = new AtomicLong(0);
    private AtomicLong numRequestsERROR = new AtomicLong(0);
    
    private Date dateBeginDocument;
    private Date dateFinishDocument;
    
    private Long begin;
    private long finish;
    
    private Evento.Estado changeEventStateTo = null;
    
    private boolean timerBased = false;
    private Integer numHoursProjected;
    private Integer numMinutesProjected;
    private Integer numSecondsProjected;
    
    private String durationStr = null;
    private String htmlContent = null;

    public SimulationData(int status, String message) {
        this.statusCode = status;
        this.message = message;
    }
    
    public SimulationData() {}
        
    public static SimulationData parse (String dataStr) throws Exception {
        logger.debug("- parse");
        if(dataStr == null) return null;
        JSONObject dataJSON = (JSONObject)JSONSerializer.toJSON(dataStr);
        return parse(dataJSON);
    }
   
    public static SimulationData parse (JSONObject dataJSON) throws Exception {
        logger.debug("- parse - json ");
        if(dataJSON == null) return null;
        SimulationData simulationData = new SimulationData();      

        if (dataJSON.containsKey("accessControlURL")) {
            simulationData.setAccessControlURL(dataJSON.getString("accessControlURL"));
        }  
        if (dataJSON.containsKey("numRequestsProjected")) {
            simulationData.setNumRequestsProjected(dataJSON.getInt("numRequestsProjected"));
        }
        if (dataJSON.containsKey("maxPendingResponses")) {
            simulationData.setMaxPendingResponses(dataJSON.getInt("maxPendingResponses"));
        }
        if (dataJSON.containsKey("htmlContent")) {
            simulationData.setHtmlContent(dataJSON.getString("htmlContent"));
        }
        if (dataJSON.containsKey("dateBeginDocument")) {
            logger.debug(" - dateBeginDocument: " + dataJSON.getString("dateBeginDocument"));
            Date dateBegin = DateUtils.getDateFromString(dataJSON.
                    getString("dateBeginDocument"));
            simulationData.setDateBeginDocument(dateBegin);
        }
        if (dataJSON.containsKey("dateFinishDocument")) {
            Date dateFinish = DateUtils.getDateFromString(dataJSON.
                    getString("dateFinishDocument"));
            simulationData.setDateFinishDocument(dateFinish);
        }
        if(dataJSON.containsKey("timer")) {
            JSONObject timerJSONObject = dataJSON.getJSONObject("timer");
            if(timerJSONObject.containsKey("active")) {
                boolean timerBased = timerJSONObject.getBoolean("active");
                simulationData.setTimerBased(timerBased);
                if(timerBased) {
                     if(timerJSONObject.containsKey("numHoursProjected")) {
                         simulationData.setNumHoursProjected(
                                 timerJSONObject.getInt("numHoursProjected"));
                     }
                    if(timerJSONObject.containsKey("numMinutesProjected")) {
                         simulationData.setNumMinutesProjected(
                                 timerJSONObject.getInt("numMinutesProjected"));
                    }
                    if(timerJSONObject.containsKey("numSecondsProjected")) {
                         simulationData.setNumSecondsProjected(
                                 timerJSONObject.getInt("numSecondsProjected"));
                    }                    
                }
            }
        }
        if (dataJSON.containsKey("whenFinishChangeEventStateTo")) {
            String state = dataJSON.getString("whenFinishChangeEventStateTo");
            simulationData.setChangeEventStateTo(Evento.Estado.valueOf(state));
        }
        return simulationData;
    }
    
    public Long getNumRequestsColected() {
        return numRequestsOK.get() + numRequestsERROR.get();
    }
    
    public Long getNumRequests() {
        return numRequests.get();
    }
        
    public Long getAndIncrementNumRequests() {
        return numRequests.getAndIncrement();
    }
    
    public Long getNumRequestsOK() {
        return numRequestsOK.get();
    }
        
    public Long getAndIncrementNumRequestsOK() {
        return numRequestsOK.getAndIncrement();
    }
    
    public Long getNumRequestsERROR() {
        return numRequestsERROR.get();
    }
        
    public Long getAndIncrementNumRequestsERROR() {
        return numRequestsERROR.getAndIncrement();
    }
    
    
    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    
    /**
     * @return the statusCode
     */
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
    public void setFinish(long finish) throws Exception {
        if(begin == null) throw new Exception("SIMULATION BEGIN NOT SET");
        long duration = finish - begin;
        durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        this.finish = finish;
    }

    public String getDurationStr() {
        return durationStr;
    }
   

    /**
     * @return the accessControlURL
     */
    public String getAccessControlURL() {
        return accessControlURL;
    }

    /**
     * @param accessControlURL the accessControlURL to set
     */
    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
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
     * @return the numRequestsProjected
     */
    public Integer getNumRequestsProjected() {
        return numRequestsProjected;
    }

    /**
     * @param numRequestsProjected the numRequestsProjected to set
     */
    public void setNumRequestsProjected(Integer numRequestsProjected) {
        this.numRequestsProjected = numRequestsProjected;
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
     * @return the timerBased
     */
    public boolean isTimerBased() {
        return timerBased;
    }

    /**
     * @param timerBased the timerBased to set
     */
    public void setTimerBased(boolean timerBased) {
        this.timerBased = timerBased;
    }

    /**
     * @return the numHoursProjected
     */
    public Integer getNumHoursProjected() {
        return numHoursProjected;
    }

    /**
     * @param numHoursProjected the numHoursProjected to set
     */
    public void setNumHoursProjected(Integer numHoursProjected) {
        this.numHoursProjected = numHoursProjected;
    }

    /**
     * @return the numMinutesProjected
     */
    public Integer getNumMinutesProjected() {
        return numMinutesProjected;
    }

    /**
     * @param numMinutesProjected the numMinutesProjected to set
     */
    public void setNumMinutesProjected(Integer numMinutesProjected) {
        this.numMinutesProjected = numMinutesProjected;
    }

    /**
     * @return the numSecondsProjected
     */
    public Integer getNumSecondsProjected() {
        return numSecondsProjected;
    }

    /**
     * @param numSecondsProjected the numSecondsProjected to set
     */
    public void setNumSecondsProjected(Integer numSecondsProjected) {
        this.numSecondsProjected = numSecondsProjected;
    }

    /**
     * @return the dateBeginDocument
     */
    public Date getDateBeginDocument() {
        return dateBeginDocument;
    }

    /**
     * @param dateBeginDocument the dateBeginDocument to set
     */
    public void setDateBeginDocument(Date dateBeginDocument) {
        this.dateBeginDocument = dateBeginDocument;
    }

    /**
     * @return the dateFinishDocument
     */
    public Date getDateFinishDocument() {
        return dateFinishDocument;
    }

    /**
     * @param dateFinishDocument the dateFinishDocument to set
     */
    public void setDateFinishDocument(Date dateFinishDocument) {
        this.dateFinishDocument = dateFinishDocument;
    }

    /**
     * @return the changeEventStateTo
     */
    public Evento.Estado getChangeEventStateTo() {
        return changeEventStateTo;
    }

    /**
     * @param changeEventStateTo the changeEventStateTo to set
     */
    public void setChangeEventStateTo(Evento.Estado changeEventStateTo) {
        this.changeEventStateTo = changeEventStateTo;
    }

}
