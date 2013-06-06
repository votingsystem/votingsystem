package org.sistemavotacion.modelo;

import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MetaInf {

    private Long numSignatures;
    private Long numVotes;
    private Long numAccessRequest;
    private String URL;
    private Tipo type;
    private String subject;
    private List<String> errorsList;
    
    
    public String getFormattedInfo() {
        StringBuilder result = new StringBuilder("");
        result.append("\n type: " + getType().toString())
              .append("\n subject: " + getSubject())
              .append("\n URL: " + getURL());
        if(numSignatures != null ) result.append("\n numSignatures: " + 
                numSignatures );
        if(numAccessRequest != null ) result.append("\n numAccessRequest: " + 
                numAccessRequest);
        if(numVotes != null ) result.append("\n numVotes: " + numVotes);
        return result.toString();
    }
			

    public static MetaInf parse(String metaInfo) {
        JSONObject metaInfoJSON = (JSONObject)JSONSerializer.toJSON(metaInfo);
        MetaInf metaInfoDeEvento = new MetaInf();
        if (metaInfoJSON.containsKey("numSignatures")) 
            metaInfoDeEvento.setNumSignatures(metaInfoJSON.getLong("numSignatures"));
        if (metaInfoJSON.containsKey("numVotes")) 
            metaInfoDeEvento.setNumVotes(metaInfoJSON.getLong("numVotes"));        
        if (metaInfoJSON.containsKey("URL")) 
            metaInfoDeEvento.setURL(metaInfoJSON.getString("URL"));
        if (metaInfoJSON.containsKey("subject")) 
            metaInfoDeEvento.setSubject(metaInfoJSON.getString("subject"));   
        if (metaInfoJSON.containsKey("numAccessRequest")) 
            metaInfoDeEvento.setNumAccessRequest(metaInfoJSON.getLong("numAccessRequest")); 
        if (metaInfoJSON.containsKey("type")) 
            metaInfoDeEvento.setType(Tipo.valueOf(metaInfoJSON.getString("type")));
        return metaInfoDeEvento;
    }
        
    public void addError(String error) {
        if(errorsList == null) errorsList = new ArrayList<String>();
        errorsList.add(error);
    }
    
    public String getFormattedErrorList() {
        if(getErrorsList() == null || getErrorsList().isEmpty()) return null;
        else {
            StringBuilder result = new StringBuilder("");
            result.append("-------- META-INF ERROR LIST ----------\n");
            for(String error:getErrorsList()) {
                result.append("\n" + error);
            }
            return result.toString();
        }
    }

    /**
     * @return the numSignatures
     */
    public Long getNumSignatures() {
        return numSignatures;
    }

    /**
     * @param numSignatures the numSignatures to set
     */
    public void setNumSignatures(Long numSignatures) {
        this.numSignatures = numSignatures;
    }

    /**
     * @return the numVotes
     */
    public Long getNumVotes() {
        return numVotes;
    }

    /**
     * @param numVotes the numVotes to set
     */
    public void setNumVotes(Long numVotes) {
        this.numVotes = numVotes;
    }

    /**
     * @return the numAccessRequest
     */
    public Long getNumAccessRequest() {
        return numAccessRequest;
    }

    /**
     * @param numAccessRequest the numAccessRequest to set
     */
    public void setNumAccessRequest(Long numAccessRequest) {
        this.numAccessRequest = numAccessRequest;
    }

    /**
     * @return the URL
     */
    public String getURL() {
        return URL;
    }

    /**
     * @param URL the URL to set
     */
    public void setURL(String URL) {
        this.URL = URL;
    }

    /**
     * @return the type
     */
    public Tipo getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(Tipo type) {
        this.type = type;
    }

    /**
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @param subject the subject to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * @return the errorsList
     */
    public List<String> getErrorsList() {
        return errorsList;
    }

    /**
     * @param aErrorsList the errorsList to set
     */
    public void setErrorsList(List<String> aErrorsList) {
        errorsList = aErrorsList;
    }
    

}
