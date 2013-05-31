package org.sistemavotacion.test.modelo;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SimulationData {
        
    private static Logger logger = LoggerFactory.getLogger(SimulationData.class);
    
    private String accessControlURL = null;
    private String controlCenterURL = null;
    private String timeStampServerURL = null;
    
    private Integer numberOfRequests = null;
    private Integer maxPendingResponses = null;
    
    private String htmlContent = null;
    
    private UserBaseData userBaseData = null;

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
        
    public static SimulationData parse (String dataStr) {
        logger.debug("- parse");
        if(dataStr == null) return null;
        JSONObject dataJSON = (JSONObject)JSONSerializer.toJSON(dataStr);
        return parse(dataJSON);
    }
   
    
    public static SimulationData parse (JSONObject dataJSON) {
        logger.debug("- parse - json ");
        if(dataJSON == null) return null;
        SimulationData simulationData = new SimulationData();      
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
        if (dataJSON.containsKey("timeStampServerURL")) {
            simulationData.setTimeStampServerURL(dataJSON.getString("timeStampServerURL"));
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
     * @return the timeStampServerURL
     */
    public String getTimeStampServerURL() {
        return timeStampServerURL;
    }

    /**
     * @param timeStampServerURL the timeStampServerURL to set
     */
    public void setTimeStampServerURL(String timeStampServerURL) {
        this.timeStampServerURL = timeStampServerURL;
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
    
    
}
