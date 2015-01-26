package org.votingsystem.client.model;

import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.StringUtils;

import java.text.ParseException;
import java.util.*;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class MetaInf {
    
    private static Logger log = Logger.getLogger(MetaInf.class);

    private Long id;
    private Date dateFinish;
    private Date dateBegin;
    private TypeVS type;
    private String subject;
    private String serverURL;
    private RepresentativesData representativesData;

    private Long numSignatures;
    private Long numAccessRequest;    
    private Long numVotes;
    private Long numRepresentativesWithAccessRequest;
    
    private List<String> errorsList;
    private List<FieldEventVS> optionList;
    
    
    public String getFormattedInfo() {
        StringBuilder result = new StringBuilder("<html>");
        result.append("<b>" + ContextVS.getMessage("eventIdLbl") + "</b>:" + getId() + "<br/>");
        String eventURL = EventVS.getURL(type, serverURL, id);
        result.append("<b>" + ContextVS.getMessage("eventURLLbl") +": </b>");
        result.append("<a href='" + eventURL + "'>" + eventURL +"</a><br/>");
        switch (type) {
            case MANIFEST_EVENT:
                result.append("<b>" + ContextVS.getMessage("numSignaturesLbl") +": </b>" +
                        String.valueOf(numSignatures) + "<br/>");
                break;
            case CLAIM_EVENT:
                result.append("<b>" + ContextVS.getMessage("numClaimsLbl") +": </b>" +
                        String.valueOf(numSignatures) + "<br/>");
                break;
            case VOTING_EVENT:
                result.append("<b>" + ContextVS.getMessage("accessRequestLbl") +": </b>");
                result.append( numAccessRequest + "<br/>");
                result.append("<b>" + ContextVS.getMessage("numVotesVSLbl") +": </b>" +
                        String.valueOf(numVotes) + "<br/>");
                if(representativesData != null) {
                    result.append("<b>" + ContextVS.getMessage("numRepresentativesLbl") +": </b>" +
                            representativesData.getNumRepresentatives() + "<br/>");
                    result.append("<b>" + ContextVS.getMessage("numRepresentativesWithVoteLbl") +": </b>" +
                            representativesData.getNumRepresentativesWithVote() + "<br/>");
                    result.append("<b>" + ContextVS.getMessage("numUsersRepresentedLbl") +": </b>" +
                            representativesData.getNumRepresented() + "<br/>");
                    result.append("<b>" + ContextVS.getMessage("numUsersRepresentedWithAcessRequestLbl") +": </b>" +
                            representativesData.getNumRepresentedWithAccessRequest() + "<br/>");
                }
                if(optionList != null) {
                    result.append("<div style='font-size:1.1em;'><b><u>" + ContextVS.getMessage("votingResultLbl") +
                            "</u><b></div>");
                    for(FieldEventVS option: optionList) {
                        result.append("<div style='margin:10px 0 0 40px;'>");
                        result.append("<div style='font-size:1.1em;'><b>" + option.getContent() +"</b></div>");
                        result.append("<b>" + ContextVS.getMessage("numVoteRequestsLbl") +": </b>" +
                                option.getNumVoteRequests() + "<br/>");
                        result.append("<b>" + ContextVS.getMessage("numUsersWithVoteLbl") +": </b>" +
                                option.getNumUsersWithVote() + "<br/>");
                        result.append("<b>" + ContextVS.getMessage("numRepresentativesWithVote1Lbl") +": </b>" +
                                option.getNumRepresentativesWithVote() + "<br/>");
                        result.append("<b>" + ContextVS.getMessage("numVotesResultLbl") +": </b>" +
                                option.getNumVotesResult() + "<br/>");
                        result.append("</div>");
                    }
                }
                break;
        }
        return result.append("</html>").toString();
    }
			

    public static MetaInf parse(String metaInfo) throws ParseException {
        log.debug("parse");
        JSONObject metaInfoJSON = (JSONObject)JSONSerializer.toJSON(metaInfo);
        MetaInf metaInf = new MetaInf();
        if (metaInfoJSON.containsKey("id")) 
                metaInf.setId(metaInfoJSON.getLong("id")); 
        if (metaInfoJSON.containsKey("subject")) 
            metaInf.setSubject(metaInfoJSON.getString("subject"));   
        if (metaInfoJSON.containsKey("type")) 
            metaInf.setType(TypeVS.valueOf(metaInfoJSON.getString("type")));
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
            metaInf.setDateFinish(DateUtils.getDateFromString(metaInfoJSON.getString("dateFinish")));
        }
        if(metaInfoJSON.containsKey("dateBegin")) {
            metaInf.setDateBegin(DateUtils.getDateFromString(metaInfoJSON.getString("dateBegin")));
        }
        if(metaInfoJSON.containsKey("REPRESENTATIVE_DATA")) {
            RepresentativesData representativesData = new RepresentativesData();
            JSONObject repJSON = metaInfoJSON.getJSONObject("REPRESENTATIVE_DATA");
            if(repJSON.containsKey("numRepresentatives")) {
                representativesData.setNumRepresentatives(repJSON.getLong("numRepresentatives"));
            }
            if(repJSON.containsKey("numRepresented")) {
                representativesData.setNumRepresented(repJSON.getLong("numRepresented"));
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
                representativesData.setNumRepresentativesWithVote(repJSON.getLong("numRepresentativesWithVote"));
            } 
            if(repJSON.containsKey("numVotesRepresentedByRepresentatives")) {
                representativesData.setNumVotesRepresentedByRepresentatives(
                        repJSON.getLong("numVotesRepresentedByRepresentatives"));
            } 
            if(repJSON.containsKey("options")) {
                JSONObject options = repJSON.getJSONObject("options");
                Set<String> keySet = options.keySet();
                List<FieldEventVS> optionList = new ArrayList<FieldEventVS>();
                for(String key:keySet) {
                    FieldEventVS option = new FieldEventVS();
                    JSONObject optionJSON = options.getJSONObject(key);
                    option.setId(Long.valueOf(key));
                    option.setContent(optionJSON.getString("content"));
                    option.setNumVoteRequests(optionJSON.getLong("numVoteRequests"));
                    option.setNumUsersWithVote(optionJSON.getLong("numUsersWithVote"));
                    option.setNumRepresentativesWithVote(optionJSON.getLong("numRepresentativesWithVote"));
                    option.setNumVotesResult(optionJSON.getLong("numVotesResult"));
                    optionList.add(option);
                }
                metaInf.setOptionList(optionList);
            }
            if(repJSON.containsKey("representatives")) {
                JSONObject representatives = repJSON.getJSONObject("representatives");
                Set<String> keySet = representatives.keySet();
                Map<String, RepresentativeData> representativeMap = new HashMap<String, RepresentativeData>();
                for(String key:keySet) {
                    JSONObject representativeJSON = representatives.getJSONObject(key);
                    RepresentativeData repData = new RepresentativeData();
                    if(representativeJSON.containsKey("id")) repData.setId(representativeJSON.getLong("id"));
                    repData.setNif(key);
                    if(representativeJSON.containsKey("optionSelectedId") && !JSONNull.getInstance().equals(
                            representativeJSON.get("optionSelectedId"))){
                        repData.setOptionSelectedId(representativeJSON.getLong("optionSelectedId"));
                    }
                    repData.setNumRepresentedWithVote(representativeJSON.getLong("numRepresentedWithVote"));
                    repData.setNumRepresentations(representativeJSON.getLong("numRepresentations"));
                    repData.setNumVotesRepresented(representativeJSON.getLong("numVotesRepresented"));
                    representativeMap.put(key, repData);
                }
                representativesData.setRepresentativeMap(representativeMap);
            }       
            metaInf.setRepresentativesData(representativesData);
        }
        return metaInf;
    }
        
    public String getOptionsHTML() {
        StringBuilder result = new StringBuilder("<html>");
        if(TypeVS.VOTING_EVENT == type) {
            result.append("<ul>");
            for(FieldEventVS option : optionList) {
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
        result.append("</html>");
        return result.toString();
    }
    
    public String getEventURL() {
        if(serverURL == null) return null;
        if(TypeVS.VOTING_EVENT == type) {
            return serverURL + "/eventVSElection/" + id;
        } else if (TypeVS.CLAIM_EVENT == type) {
            return serverURL + "/eventVSClaim/" + id;
        } else if (TypeVS.MANIFEST_EVENT == type) {
            return serverURL + "/eventVSManifest/" + id;
        } else {
            log.error("Unknown server type: " + type);
            return null;
        }
    }
        
    public String getRepresentativesHTML() {
        StringBuilder result = new StringBuilder("<HTML>");
        if(TypeVS.VOTING_EVENT == type) {
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
        for(FieldEventVS option:optionList) {
            if(option.getId() == optionId) return option.getContent();
        }
        return null;
    }

    public Long getNumSignatures() {
        return numSignatures;
    }

    public void setNumSignatures(Long numSignatures) {
        this.numSignatures = numSignatures;
    }

    public Long getNumVotes() {
        return numVotes;
    }

    public void setNumVotes(Long numVotes) {
        this.numVotes = numVotes;
    }

    public Long getNumAccessRequest() {
        return numAccessRequest;
    }

    public void setNumAccessRequest(Long numAccessRequest) {
        this.numAccessRequest = numAccessRequest;
    }

    public TypeVS getType() {
        return type;
    }

    public void setType(TypeVS type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<String> getErrorsList() {
        return errorsList;
    }

    public void setErrorsList(List<String> aErrorsList) {
        errorsList = aErrorsList;
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public void setDateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
    }

    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = StringUtils.checkURL(serverURL);
    }

    public Long getNumRepresentativesWithAccessRequest() {
        return numRepresentativesWithAccessRequest;
    }

    public void setNumRepresentativesWithAccessRequest(Long numRepresentativesWithAccessRequest) {
        this.numRepresentativesWithAccessRequest = numRepresentativesWithAccessRequest;
    }

    public List<FieldEventVS> getOptionList() {
        return optionList;
    }

    public void setOptionList(List<FieldEventVS> optionList) {
        this.optionList = optionList;
    }

    public RepresentativeData getRepresentativeData(String representativeNif) {
        if(representativesData == null) return null;
        Map<String, RepresentativeData> representativesMap = 
                representativesData.getRepresentativeMap();
        return representativesMap.get(representativeNif);
    }

    public RepresentativesData getRepresentativesData() {
        return representativesData;
    }

    public void setRepresentativesData(RepresentativesData representativesData) {
        this.representativesData = representativesData;
    }
    
    public Date getDateBegin() {
        return dateBegin;
    }
}
