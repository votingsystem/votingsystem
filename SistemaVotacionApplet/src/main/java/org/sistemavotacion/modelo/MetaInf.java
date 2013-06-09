package org.sistemavotacion.modelo;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.util.DateUtils;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MetaInf {

    private Long id;
    private Date dateFinish;
    private Tipo type;
    private String subject;
    private String serverURL;
    private Long numSignatures;
    private Long numAccessRequest;    
    private Long numVotes;
    private Long numRepresented;
    private Long numRepresentedWithAccessRequest;
    private Long numRepresentatives;    
    private Long numRepresentativesWithAccessRequest;
    private Long numRepresentativesWithVote;
    
    private List<String> errorsList;
    
    
    public String getFormattedInfo() {
        StringBuilder result = new StringBuilder("");
        result.append("\n type: " + getType().toString())
              .append("\n subject: " + getSubject())
              .append("\n id: " + getId());
        if(numSignatures != null ) result.append("\n numSignatures: " + 
                numSignatures );
        if(numAccessRequest != null ) result.append("\n numAccessRequest: " + 
                numAccessRequest);
        if(numVotes != null ) result.append("\n numVotes: " + numVotes);
        if(numRepresentatives != null ) result.append(
                "\n numRepresentatives: " + numRepresentatives); 
        if(numRepresentativesWithAccessRequest != null ) result.append(
                "\n numRepresentativesWithAccessRequest: " + 
                numRepresentativesWithAccessRequest);  
        if(numRepresentativesWithVote != null ) result.append(
                "\n numRepresentativesWithVote: " + 
                numRepresentativesWithVote); 
        if(numRepresented != null ) result.append(
                "\n numRepresented: " + numRepresented); 
        if(numRepresentedWithAccessRequest != null ) result.append(
                "\n numRepresentedWithAccessRequest: " + numRepresentedWithAccessRequest);         
        return result.toString();
    }
			

    public static MetaInf parse(String metaInfo) throws ParseException {
        JSONObject metaInfoJSON = (JSONObject)JSONSerializer.toJSON(metaInfo);
        MetaInf metaInfoDeEvento = new MetaInf();
        if (metaInfoJSON.containsKey("id")) 
                metaInfoDeEvento.setId(metaInfoJSON.getLong("id")); 
        if (metaInfoJSON.containsKey("subject")) 
            metaInfoDeEvento.setSubject(metaInfoJSON.getString("subject"));   
        if (metaInfoJSON.containsKey("type")) 
            metaInfoDeEvento.setType(Tipo.valueOf(metaInfoJSON.getString("type")));
        if(metaInfoJSON.containsKey("serverURL")) 
            metaInfoDeEvento.setServerURL(metaInfoJSON.getString("serverURL"));
        if(metaInfoJSON.containsKey("BACKUP")) {
            JSONObject backupJSON = metaInfoJSON.getJSONObject("BACKUP");
            if(backupJSON.containsKey("numSignatures")) {
                 metaInfoDeEvento.setNumSignatures(backupJSON.getLong("numSignatures"));
            }
            if (backupJSON.containsKey("numVotes")) {
                metaInfoDeEvento.setNumVotes(backupJSON.getLong("numVotes")); 
            } 
            if (backupJSON.containsKey("numAccessRequest")) 
                metaInfoDeEvento.setNumAccessRequest(backupJSON.getLong("numAccessRequest")); 
        }
        if(metaInfoJSON.containsKey("dateFinish")) {
            Date dateFinish = DateUtils.getDateFromString(
                    metaInfoJSON.getString("dateFinish"));
            metaInfoDeEvento.setDateFinish(dateFinish);
        }
        if(metaInfoJSON.containsKey("REPRESENTATIVE_ACCREDITATIONS")) {
            JSONObject repJSON = metaInfoJSON.getJSONObject(
                    "REPRESENTATIVE_ACCREDITATIONS");
            if(repJSON.containsKey("numRepresentatives")) {
                metaInfoDeEvento.setNumRepresentatives(repJSON.getLong("numRepresentatives"));
            }
            if(repJSON.containsKey("numRepresented")) {
                metaInfoDeEvento.setNumRepresented(
                        repJSON.getLong("numRepresented"));
            }
            if(repJSON.containsKey("numRepresentedWithAccessRequest")) {
                metaInfoDeEvento.setNumRepresentedWithAccessRequest(
                        repJSON.getLong("numRepresentedWithAccessRequest"));
            }
            if(repJSON.containsKey("numRepresentativesWithAccessRequest")) {
                metaInfoDeEvento.setNumRepresentativesWithAccessRequest(
                        repJSON.getLong("numRepresentativesWithAccessRequest"));
            }
            if(repJSON.containsKey("numRepresentativesWithVote")) {
                metaInfoDeEvento.setNumRepresentativesWithVote(
                        repJSON.getLong("numRepresentativesWithVote"));
            }            
        }
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

    /**
     * @return the dateFinish
     */
    public Date getDateFinish() {
        return dateFinish;
    }

    /**
     * @param dateFinish the dateFinish to set
     */
    public void setDateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
    }

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

    /**
     * @return the numRepresentatives
     */
    public Long getNumRepresentatives() {
        return numRepresentatives;
    }

    /**
     * @param numRepresentatives the numRepresentatives to set
     */
    public void setNumRepresentatives(Long numRepresentatives) {
        this.numRepresentatives = numRepresentatives;
    }

    /**
     * @return the numRepresented
     */
    public Long getNumRepresented() {
        return numRepresented;
    }

    /**
     * @param numRepresented the numRepresented to set
     */
    public void setNumRepresented(Long numRepresented) {
        this.numRepresented = numRepresented;
    }

    /**
     * @return the serverURL
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * @param serverURL the serverURL to set
     */
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    /**
     * @return the numRepresentativesWithAccessRequest
     */
    public Long getNumRepresentativesWithAccessRequest() {
        return numRepresentativesWithAccessRequest;
    }

    /**
     * @param numRepresentativesWithAccessRequest the numRepresentativesWithAccessRequest to set
     */
    public void setNumRepresentativesWithAccessRequest(Long numRepresentativesWithAccessRequest) {
        this.numRepresentativesWithAccessRequest = numRepresentativesWithAccessRequest;
    }

    /**
     * @return the numRepresentedWithAccessRequest
     */
    public Long getNumRepresentedWithAccessRequest() {
        return numRepresentedWithAccessRequest;
    }

    /**
     * @param numRepresentedWithAccessRequest the numRepresentedWithAccessRequest to set
     */
    public void setNumRepresentedWithAccessRequest(Long numRepresentedWithAccessRequest) {
        this.numRepresentedWithAccessRequest = numRepresentedWithAccessRequest;
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
    
}
