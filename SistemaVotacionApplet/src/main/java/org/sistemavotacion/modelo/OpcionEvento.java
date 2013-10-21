package org.sistemavotacion.modelo;

import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class OpcionEvento {
        
    private static Logger logger = LoggerFactory.getLogger(OpcionEvento.class);
    
    private Long id;
    private String content;
    private String value;
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

    public static OpcionEvento parse (String opcionStr) {
        if(opcionStr == null) return null;
        JSONObject eventoJSON = (JSONObject)JSONSerializer.toJSON(opcionStr);
        return parse(eventoJSON);
    }
    
    public static OpcionEvento parse (JSONObject opcionJSON) {
        if(opcionJSON == null) return null;
        OpcionEvento opcion = new OpcionEvento();       
        if(opcionJSON.containsKey("id") && 
                !JSONNull.getInstance().equals(opcionJSON.get("id"))) {
            opcion.setId(opcionJSON.getLong("id"));
        }
        if(opcionJSON.containsKey("contenido")) opcion.setContent(opcionJSON.getString("contenido"));
        return opcion;
    }
    
    public JSONObject toJSON() {
        logger.debug("toJSON");
        Map map = new HashMap();
        map.put("id", id);
        map.put("contenido", content);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);        
        return jsonObject;
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

}