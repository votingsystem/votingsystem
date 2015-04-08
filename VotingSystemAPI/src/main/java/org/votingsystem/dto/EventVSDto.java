package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.*;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.MapUtils;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventVSDto {

    private Long id;
    private Long numSignatures;
    private Date dateCreated;
    private Date dateBegin;
    private Date dateFinish;
    private String URL;
    private String publishRequestURL;
    private String voteVSInfoURL;
    private String eventCACertificateURL;
    private String subject;
    private String content;
    private String duration;
    private String userVS;
    private EventVS.Cardinality cardinality;
    private EventVS.State state;
    private Set<TagVS> tags;
    private Set<FieldEventVS> fieldsEventVS;
    private Map<String, String> accessControl;
    private Map<String, String> controlCenter;
    private boolean backupAvailable;

    public EventVSDto() {}

    public EventVSDto(EventVS eventVS, String serverName, String contextURL) {
        this.id = eventVS.getId();
        this.dateCreated = eventVS.getDateCreated();
        this.subject = eventVS.getSubject();
        this.content = eventVS.getContent();
        this.userVS = eventVS.getUserVS().getName();
        this.cardinality = eventVS.getCardinality();
        this.tags = eventVS.getTagVSSet();
        this.backupAvailable = eventVS.getBackupAvailable();
        this.state = eventVS.getState();
        this.dateBegin = eventVS.getDateBegin();
        this.dateFinish = eventVS.getDateFinish();
        this.duration = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(
                eventVS.getDateBegin().getTime() - eventVS.getDateFinish().getTime());
        if(eventVS instanceof EventVSElection) {
            this.URL = contextURL + "/eventVSElection/id/" + eventVS.getId();
            this.publishRequestURL = contextURL + "/eventVSElection/id/" + eventVS.getId() + "/publishRequest";
            this.eventCACertificateURL = contextURL + "/certificateVS/eventVS/id/" + eventVS.getId() + "/CACertificate";
            this.voteVSInfoURL = contextURL + "/eventVSElection/id/" + eventVS.getId() + "/voteVSInfo";
            ControlCenterVS controlCenterVS = eventVS.getControlCenterVS();
            if(controlCenterVS != null) this.controlCenter = MapUtils.getActorVSMap(
                    controlCenterVS.getServerURL(), controlCenterVS.getName());
        } else if (eventVS instanceof EventVSClaim) {
            this.URL = contextURL + "/eventVSClaim/id/" + eventVS.getId();
            this.publishRequestURL = contextURL + "/eventVSClaim/id/" + eventVS.getId() + "/publishRequest";
        }
        this.fieldsEventVS = eventVS.getFieldsEventVS();
        this.accessControl = MapUtils.getActorVSMap(contextURL, serverName);
    }

    public EventVSElection getEventVSElection() {
        EventVSElection result = new EventVSElection();
        result.setId(id);
        result.setDateCreated(dateCreated);
        result.setSubject(subject);
        result.setDateBegin(dateBegin);
        result.setFieldsEventVS(fieldsEventVS);
        return result;
    }

    public void setNumSignatures(Long numSignatures) {
        this.numSignatures = numSignatures;
    }

    public Long getId() {
        return id;
    }

    public Long getNumSignatures() {
        return numSignatures;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Date getDateBegin() {
        return dateBegin;
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public String getURL() {
        return URL;
    }

    public String getPublishRequestURL() {
        return publishRequestURL;
    }

    public String getVoteVSInfoURL() {
        return voteVSInfoURL;
    }

    public String getEventCACertificateURL() {
        return eventCACertificateURL;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getDuration() {
        return duration;
    }

    public String getUserVS() {
        return userVS;
    }

    public EventVS.Cardinality getCardinality() {
        return cardinality;
    }

    public EventVS.State getState() {
        return state;
    }

    public Set<TagVS> getTags() {
        return tags;
    }

    public Set<FieldEventVS> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public Map<String, String> getAccessControl() {
        return accessControl;
    }

    public Map<String, String> getControlCenter() {
        return controlCenter;
    }

    public boolean isBackupAvailable() {
        return backupAvailable;
    }

    public static Object getJSON(EventVS eventVS) {
        if(eventVS instanceof EventVSElection) return new EventVSElectionDto(eventVS);
        else return new EventVSDto(eventVS, null, null);
    }
}