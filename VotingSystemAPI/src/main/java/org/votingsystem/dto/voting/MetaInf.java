package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.FieldEvent;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaInf {
    
    private static Logger log = Logger.getLogger(MetaInf.class.getName());

    private Long id;
    private Date dateFinish;
    private Date dateBegin;
    private TypeVS type;
    private String subject;
    private String serverURL;
    private RepresentativesData representativesData;
    private BackupData BACKUP;

    private List<String> errorsList;
    private List<FieldEvent> optionList;

    @JsonIgnore
    public String getFormattedInfo() {
        StringBuilder result = new StringBuilder(
                "<html style='font-family: arial, helvetica, sans-serif; color: #555; padding:5px 15px 0 15px;'>");
        result.append("<b>" + ContextVS.getMessage("eventIdLbl") + "</b>:" + getId() + "<br/>");
        String eventURL = EventVS.getURL(type, serverURL, id);
        result.append("<b>" + ContextVS.getMessage("eventURLLbl") +": </b>");
        result.append("<a href='" + eventURL + "'>" + eventURL +"</a><br/>");
        switch (type) {
            case VOTING_EVENT:
                result.append("<b>" + ContextVS.getMessage("accessRequestLbl") +": </b>");
                result.append( BACKUP.getNumAccessRequest() + "<br/>");
                result.append("<b>" + ContextVS.getMessage("numVotesLbl") +": </b>" +
                        String.valueOf(BACKUP.getNumVotes()) + "<br/>");
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
                    result.append("<br/><br/><div style='font-size:1.1em;'><b><u>" +
                            ContextVS.getMessage("votingResultLbl") + "</u><b></div>");
                    for(FieldEvent option: optionList) {
                        result.append("<div style='margin:10px 0 0 40px;'>");
                        result.append("<div style='font-size:1.1em;'><b> - " + option.getContent() +"</b></div>");
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

    public void loadRepresentativeData(Map metaInfMap) throws ParseException {
        RepresentativesData representativesData = new RepresentativesData();
        if(metaInfMap.containsKey("numRepresentatives")) {
            representativesData.setNumRepresentatives(((Number)metaInfMap.get("numRepresentatives")).longValue());
        }
        if(metaInfMap.containsKey("numRepresented")) {
            representativesData.setNumRepresented(((Number)metaInfMap.get("numRepresented")).longValue());
        }
        if(metaInfMap.containsKey("numRepresentedWithAccessRequest")) {
            representativesData.setNumRepresentedWithAccessRequest(((Number)
                    metaInfMap.get("numRepresentedWithAccessRequest")).longValue());
        }
        if(metaInfMap.containsKey("numRepresentativesWithAccessRequest")) {
            representativesData.setNumRepresentativesWithAccessRequest(
                    ((Number)metaInfMap.get("numRepresentativesWithAccessRequest")).longValue());
        }
        if(metaInfMap.containsKey("numRepresentativesWithVote")) {
            representativesData.setNumRepresentativesWithVote(
                    ((Number)metaInfMap.get("numRepresentativesWithVote")).longValue());
        }
        if(metaInfMap.containsKey("numVotesRepresentedByRepresentatives")) {
            representativesData.setNumVotesRepresentedByRepresentatives(
                    ((Number)metaInfMap.get("numVotesRepresentedByRepresentatives")).longValue());
        }
        if(metaInfMap.containsKey("options")) {
            Map options = (Map) metaInfMap.get("options");
            Set<String> keySet = options.keySet();
            List<FieldEvent> optionList = new ArrayList<FieldEvent>();
            for(String key:keySet) {
                FieldEvent option = new FieldEvent();
                Map optionJSON = (Map) options.get(key);
                option.setId(Long.valueOf(key));
                option.setContent((String) optionJSON.get("content"));
                option.setNumVoteRequests(((Number)optionJSON.get("numVoteRequests")).longValue());
                option.setNumUsersWithVote(((Number)optionJSON.get("numUsersWithVote")).longValue());
                option.setNumRepresentativesWithVote(((Number)optionJSON.get("numRepresentativesWithVote")).longValue());
                option.setNumVotesResult(((Number)optionJSON.get("numVotesResult")).longValue());
                optionList.add(option);
            }
            setOptionList(optionList);
        }
        if(metaInfMap.containsKey("representatives")) {
            Map representatives = (Map) metaInfMap.get("representatives");
            Set<String> keySet = representatives.keySet();
            Map<String, RepresentativeData> representativeMap = new HashMap<String, RepresentativeData>();
            for(String key:keySet) {
                Map representativeJSON = (Map) representatives.get(key);
                RepresentativeData repData = new RepresentativeData();
                if(representativeJSON.containsKey("id")) repData.setId(((Number)representativeJSON.get("id")).longValue());
                repData.setNif(key);
                if(representativeJSON.containsKey("optionSelectedId")){
                    repData.setOptionSelectedId(((Number)representativeJSON.get("optionSelectedId")).longValue());
                }
                repData.setNumRepresentedWithVote(((Number)representativeJSON.get("numRepresentedWithVote")).longValue());
                repData.setNumRepresentations(((Number)representativeJSON.get("numRepresentations")).longValue());
                repData.setNumVotesRepresented(((Number)representativeJSON.get("numVotesRepresented")).longValue());
                representativeMap.put(key, repData);
            }
            representativesData.setRepresentativeMap(representativeMap);
        }
        setRepresentativesData(representativesData);
    }

    @JsonIgnore
    public String getOptionsHTML() {
        StringBuilder result = new StringBuilder("<html>");
        if(TypeVS.VOTING_EVENT == type) {
            result.append("<ul>");
            for(FieldEvent option : optionList) {
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

    @JsonIgnore
    public Long getNumFilesToProcess() {
        return BACKUP.getNumAccessRequest() + BACKUP.getNumVotes() + representativesData.getNumRepresented() +
                representativesData.getNumRepresentedWithAccessRequest();
    }

    @JsonIgnore
    public int getNumVotes() {
        return BACKUP.getNumVotes().intValue();
    }

    @JsonIgnore
    public int getNumAccessRequest() {
        return BACKUP.getNumAccessRequest().intValue();
    }

    public BackupData getBACKUP() {
        return BACKUP;
    }

    public void setBACKUP(BackupData BACKUP) {
        this.BACKUP = BACKUP;
    }

    public String getEventURL() {
        if(serverURL == null) return null;
        return EventVS.getURL(type, serverURL, id);
    }

    @JsonIgnore
    public String getRepresentativesHTML() {
        StringBuilder result = new StringBuilder("<HTML style='font-family: arial, helvetica, sans-serif; color: #555; margin:10px 0 0 0;'>");
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
        for(FieldEvent option:optionList) {
            if(option.getId() == optionId) return option.getContent();
        }
        return null;
    }

    public TypeVS getType() {
        return type;
    }

    public MetaInf setType(TypeVS type) {
        this.type = type;
        return this;
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

    public List<FieldEvent> getOptionList() {
        return optionList;
    }

    public void setOptionList(List<FieldEvent> optionList) {
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


    public static class BackupData {
        private Long numSignatures;
        private Long numAccessRequest;
        private Long numVotes;
        private Long numRepresentativesWithAccessRequest;

        public BackupData() {}

        public Long getNumSignatures() {
            return numSignatures;
        }

        public void setNumSignatures(Long numSignatures) {
            this.numSignatures = numSignatures;
        }

        public Long getNumAccessRequest() {
            return numAccessRequest;
        }

        public void setNumAccessRequest(Long numAccessRequest) {
            this.numAccessRequest = numAccessRequest;
        }

        public Long getNumVotes() {
            return numVotes;
        }

        public void setNumVotes(Long numVotes) {
            this.numVotes = numVotes;
        }

        public Long getNumRepresentativesWithAccessRequest() {
            return numRepresentativesWithAccessRequest;
        }

        public void setNumRepresentativesWithAccessRequest(Long numRepresentativesWithAccessRequest) {
            this.numRepresentativesWithAccessRequest = numRepresentativesWithAccessRequest;
        }
    }

}
