package org.votingsystem.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class OptionVS {
    
	private static Logger logger = Logger.getLogger(OptionVS.class);
    
    private Long id;
    private String content;
    private String value;
    private EventVS eventVS;
    private Long numVoteRequests;
    private Long numUsersWithVote;
    private Long numRepresentativesWithVote;
    private Long numVotesResult;

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    public static OptionVS populate (Map optionMap) {
        if(optionMap == null) return null;
        OptionVS opcion = new OptionVS();       
        if(optionMap.get("id") != null && 
        		!"null".equals(optionMap.get("id").toString())) {
            opcion.setId(((Integer) optionMap.get("id")).longValue());
        }
        if(optionMap.get("contenido") != null) 
        	opcion.setContent((String) optionMap.get("contenido"));
        return opcion;
    }
    
    public Map getDataMap() {
        logger.debug("toJSON");
        Map map = new HashMap();
        map.put("id", id);
        map.put("contenido", content);  
        return map;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the numVoteRequests
     */
    public Long getNumVoteRequests() {
        return numVoteRequests;
    }

    /**
     * @param numVoteRequests the numVoteRequests to set
     */
    public void setNumVoteRequests(Long numVoteRequests) {
        this.numVoteRequests = numVoteRequests;
    }

    /**
     * @return the numUsersWithVote
     */
    public Long getNumUsersWithVote() {
        return numUsersWithVote;
    }

    /**
     * @param numUsersWithVote the numUsersWithVote to set
     */
    public void setNumUsersWithVote(Long numUsersWithVote) {
        this.numUsersWithVote = numUsersWithVote;
    }

    /**
     * @return the numRepresentativesWithVote
     */
    public Long getNumRepresentativesWithVote() {
        return numRepresentativesWithVote;
    }

    /**
     * @param numRepresentativesWithVote the numRepresentativesWithVote to set
     */
    public void setNumRepresentativesWithVote(Long numRepresentativesWithVote) {
        this.numRepresentativesWithVote = numRepresentativesWithVote;
    }

    /**
     * @return the numVotesResult
     */
    public Long getNumVotesResult() {
        return numVotesResult;
    }

    /**
     * @param numVotesResult the numVotesResult to set
     */
    public void setNumVotesResult(Long numVotesResult) {
        this.numVotesResult = numVotesResult;
    }

	public EventVS getEventVS() {
		return eventVS;
	}

	public void setEventVS(EventVS eventVS) {
		this.eventVS = eventVS;
	}

}
