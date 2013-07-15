package org.sistemavotacion.herramientavalidacion.modelo;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MetaInf {
    
    private static Logger logger = LoggerFactory.getLogger(MetaInf.class);

    private Long id;
    private Date dateFinish;
    private Date dateInit;
    private Tipo type;
    private String subject;
    private String serverURL;
    private RepresentativesData representativesData;

    private Long numSignatures;
    private Long numAccessRequest;    
    private Long numVotes;
    private Long numRepresentativesWithAccessRequest;
    
    private List<String> errorsList;
    private List<OpcionEvento> optionList;
    
    
    public String getFormattedInfo() {
        StringBuilder result = new StringBuilder("");
        result.append("\n id: " + getId())
              .append("\n subject: " + getSubject())
              .append("\n type: " + getType().toString());
        if(numSignatures != null ) result.append("\n numSignatures: " + 
                numSignatures );
        if(numAccessRequest != null ) result.append("\n numAccessRequest: " + 
                numAccessRequest);
        if(numVotes != null ) result.append("\n numVotes: " + numVotes);
        if(getRepresentativesData() != null ) result.append(
                "\n numRepresentatives: " + getRepresentativesData().getNumRepresentatives()); 
        if(numRepresentativesWithAccessRequest != null ) result.append(
                "\n numRepresentativesWithAccessRequest: " + 
                numRepresentativesWithAccessRequest);  
        if(representativesData != null){
            if(representativesData.getNumRepresentativesWithVote() != null ) 
                    result.append( "\n numRepresentativesWithVote: " + 
                    representativesData.getNumRepresentativesWithVote());
        }
 
        if(representativesData.getNumRepresented() != null ) result.append(
                "\n numRepresented: " + representativesData.getNumRepresented()); 
        if(representativesData.getNumRepresentedWithAccessRequest() != null ) result.append(
                "\n numRepresentedWithAccessRequest: " + 
                representativesData.getNumRepresentedWithAccessRequest());         
        return result.toString();
    }
			

    public static MetaInf parse(String metaInfo) throws ParseException {
        logger.debug("parse");
        JSONObject metaInfoJSON = (JSONObject)JSONSerializer.toJSON(metaInfo);
        MetaInf metaInf = new MetaInf();
        if (metaInfoJSON.containsKey("id")) 
                metaInf.setId(metaInfoJSON.getLong("id")); 
        if (metaInfoJSON.containsKey("subject")) 
            metaInf.setSubject(metaInfoJSON.getString("subject"));   
        if (metaInfoJSON.containsKey("type")) 
            metaInf.setType(Tipo.valueOf(metaInfoJSON.getString("type")));
        if(metaInfoJSON.containsKey("serverURL")) 
            metaInf.setServerURL(metaInfoJSON.getString("serverURL"));
        if(metaInfoJSON.containsKey("BACKUP")) {
            JSONObject backupJSON = metaInfoJSON.getJSONObject("BACKUP");
            if(backupJSON.containsKey("numSignatures")) {
                 metaInf.setNumSignatures(backupJSON.getLong("numSignatures"));
            }
            if (backupJSON.containsKey("numVotes")) {
                metaInf.setNumVotes(backupJSON.getLong("numVotes")); 
            } 
            if (backupJSON.containsKey("numAccessRequest")) 
                metaInf.setNumAccessRequest(backupJSON.getLong("numAccessRequest")); 
        }
        if(metaInfoJSON.containsKey("dateFinish")) {
            Date dateFinish = DateUtils.getDateFromString(
                    metaInfoJSON.getString("dateFinish"));
            metaInf.setDateFinish(dateFinish);
        }
        if(metaInfoJSON.containsKey("dateInit")) {
            Date dateInit = DateUtils.getDateFromString(
                    metaInfoJSON.getString("dateInit"));
            metaInf.setDateInit(dateInit);
        }
        if(metaInfoJSON.containsKey("REPRESENTATIVE_DATA")) {
            RepresentativesData representativesData = new RepresentativesData();
            JSONObject repJSON = metaInfoJSON.getJSONObject(
                    "REPRESENTATIVE_DATA");
            if(repJSON.containsKey("numRepresentatives")) {
                representativesData.setNumRepresentatives(
                        repJSON.getLong("numRepresentatives"));
            }
            if(repJSON.containsKey("numRepresented")) {
                representativesData.setNumRepresented(
                        repJSON.getLong("numRepresented"));
            }
            if(repJSON.containsKey("numRepresentedWithAccessRequest")) {
                representativesData.setNumRepresentedWithAccessRequest(
                        repJSON.getLong("numRepresentedWithAccessRequest"));
            }
            if(repJSON.containsKey("numRepresentativesWithAccessRequest")) {
                representativesData.setNumRepresentativesWithAccessRequest(
                        repJSON.getLong("numRepresentativesWithAccessRequest"));
            }
            if(repJSON.containsKey("numRepresentativesWithVote")) {
                representativesData.setNumRepresentativesWithVote(
                        repJSON.getLong("numRepresentativesWithVote"));
            } 
            if(repJSON.containsKey("numVotesRepresentedByRepresentatives")) {
                representativesData.setNumVotesRepresentedByRepresentatives(
                        repJSON.getLong("numVotesRepresentedByRepresentatives"));
            } 
            if(repJSON.containsKey("options")) {
                JSONObject options = repJSON.getJSONObject("options");
                Set<String> keySet = options.keySet();
                List<OpcionEvento> optionList = new ArrayList<OpcionEvento>();
                for(String key:keySet) {
                    OpcionEvento option = new OpcionEvento();
                    JSONObject optionJSON = options.getJSONObject(key);
                    option.setId(Long.valueOf(key));
                    option.setContent(optionJSON.getString("content"));
                    option.setNumVoteRequests(optionJSON.getLong("numVoteRequests"));
                    option.setNumUsersWithVote(optionJSON.getLong("numUsersWithVote"));
                    option.setNumRepresentativesWithVote(optionJSON.getLong(
                            "numRepresentativesWithVote"));
                    option.setNumVotesResult(optionJSON.getLong(
                            "numVotesResult"));
                    optionList.add(option);
                }
                metaInf.setOptionList(optionList);
            }
            if(repJSON.containsKey("representatives")) {
                JSONObject representatives = repJSON.getJSONObject("representatives");
                Set<String> keySet = representatives.keySet();
                Map<String, RepresentativeData> representativeMap = 
                        new HashMap<String, RepresentativeData>();
                for(String key:keySet) {
                    JSONObject representativeJSON = representatives.getJSONObject(key);
                    RepresentativeData repData = new RepresentativeData();
                    if(representativeJSON.containsKey("id"))
                        repData.setId(representativeJSON.getLong("id"));
                    repData.setNif(key);
                    if(representativeJSON.containsKey("optionSelectedId") &&
                            !JSONNull.getInstance().equals(
                            representativeJSON.get("optionSelectedId"))){
                        repData.setOptionSelectedId(representativeJSON.getLong(
                            "optionSelectedId"));
                    }
                    repData.setNumRepresentedWithVote(representativeJSON.getLong(
                            "numRepresentedWithVote"));
                    repData.setNumRepresentations(representativeJSON.getLong(
                            "numRepresentations"));
                    repData.setNumVotesRepresented(representativeJSON.getLong(
                            "numVotesRepresented"));
                    representativeMap.put(key, repData);
                }
                representativesData.setRepresentativeMap(representativeMap);
            }       
            metaInf.setRepresentativesData(representativesData);
        }
        return metaInf;
    }
        
    public String getOptionsHTML() {
        StringBuilder result = new StringBuilder("<HTML>");
        if(Tipo.EVENTO_VOTACION == type) {
            result.append("<ul>");
            for(OpcionEvento option : optionList) {
                result.append("<li><b>Opcion:</b> " + option.getContent() + "<br/>" +
                        "id:" + option.getId() + "<br/>" +
                        "numVoteRequests:" + option.getNumVoteRequests() + "<br/>" +
                        "numUsersWithVote:" + option.getNumUsersWithVote() + "<br/>" +
                        "numRepresentativesWithVote:" + option.getNumRepresentativesWithVote() + "<br/>" +
                        "numVotesResult:" + option.getNumVotesResult() +
                        "</li>");
            }
            result.append("</ul>");
        }
        result.append("</HTML>");
        return result.toString();
    }
    
    public String getEventURL() {
        if(serverURL == null) return null;
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        if(Tipo.EVENTO_VOTACION == type) {
            return serverURL + "eventoVotacion/" + id;
        } else if (Tipo.EVENTO_RECLAMACION == type) {
            return serverURL + "eventoReclamacion/" + id;
        } else if (Tipo.EVENTO_FIRMA == type) {
            return serverURL + "eventoFirma/" + id;
        } else return null;
    }
        
    public String getRepresentativesHTML() {
        StringBuilder result = new StringBuilder("<HTML>");
        if(Tipo.EVENTO_VOTACION == type) {
            result.append("<ul>");
            Map<String, RepresentativeData> representativesMap = 
                    representativesData.getRepresentativeMap();
            Collection<RepresentativeData> representativeCollection = 
                    representativesMap.values();
            for(RepresentativeData repData : representativeCollection) {
                String url = serverURL + "/representative/" + repData.getId();
                result.append("<li><b>Representative:</b> " + repData.getNif() + "<br/>" +
                        "URL: <a href=\"" + url + "\">" + url + "</a><br/>" +
                        "optionSelectedId:" + repData.getOptionSelectedId() + "<br/>" +
                        "numRepresentedWithVote:" + repData.getNumRepresentedWithVote() + "<br/>" +
                        "numRepresentations:" + repData.getNumRepresentations()+ "<br/>" +
                        "numVotesRepresented:" + repData.getNumVotesRepresented() + 
                        "</li><br/>");
            }
            result.append("</ul>");
        }
        result.append("</HTML>");
        return result.toString();
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
    
    public String getOptionContent(Long optionId) {
        if(optionId == null) return null;
        if(optionList == null) return null;
        for(OpcionEvento option:optionList) {
            if(option.getId() == optionId) return option.getContent();
        }
        return null;
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
     * @param dateFinish the dateFinish to set
     */
    public void setDateInit(Date dateInit) {
        this.dateInit = dateInit;
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
     * @return the optionList
     */
    public List<OpcionEvento> getOptionList() {
        return optionList;
    }

    /**
     * @param optionList the optionList to set
     */
    public void setOptionList(List<OpcionEvento> optionList) {
        this.optionList = optionList;
    }
    
    /**
     * @return the representativesData
     */
    public RepresentativeData getRepresentativeData(String representativeNif) {
        if(representativesData == null) return null;
        Map<String, RepresentativeData> representativesMap = 
                representativesData.getRepresentativeMap();
        return representativesMap.get(representativeNif);
    }

    /**
     * @return the representativesData
     */
    public RepresentativesData getRepresentativesData() {
        return representativesData;
    }

    /**
     * @param representativesData the representativesData to set
     */
    public void setRepresentativesData(RepresentativesData representativesData) {
        this.representativesData = representativesData;
    }
    
    public Date getDateInit() {
        return dateInit;
    }
}
