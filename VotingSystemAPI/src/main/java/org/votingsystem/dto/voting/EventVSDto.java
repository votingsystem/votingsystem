package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.model.voting.ControlCenterVS;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.model.voting.FieldEventVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
    private List<String> tags;
    private Set<FieldEventVS> fieldsEventVS;
    private ActorVSDto accessControl;
    private ActorVSDto controlCenter;
    private boolean backupAvailable;
    private TypeVS operation;
    private EventVS.Type type;
    private Long eventId;
    private Long accessControlEventVSId;
    private String certCAVotacion;
    private String controlCenterURL;
    private String certChain;
    private String accessControlURL;
    private String serverURL;
    private VoteVSDto voteVS;
    private String UUID;

    public EventVSDto() {}

    public EventVSDto(String serverURL) {
        this.serverURL = serverURL;
    }

    public EventVSDto(EventVS eventVS) {
        this.setId(eventVS.getId());
        this.setDateCreated(eventVS.getDateCreated());
        this.setSubject(eventVS.getSubject());
        this.setContent(eventVS.getContent());
        if(eventVS.getUserVS() != null) this.setUserVS(eventVS.getUserVS().getName());
        this.setCardinality(eventVS.getCardinality());
        this.setTags(eventVS.getTagList());
        this.setBackupAvailable(eventVS.getBackupAvailable());
        this.setState(eventVS.getState());
        this.setDateBegin(eventVS.getDateBegin());
        this.setDateFinish(eventVS.getDateFinish());
        if(eventVS.getDateBegin() != null && eventVS.getDateFinish() != null) {
            this.setDuration(DateUtils.getElapsedTimeHoursMinutesFromMilliseconds( eventVS.getDateBegin().getTime() -
                    eventVS.getDateFinish().getTime()));
        }
        this.setFieldsEventVS(eventVS.getFieldsEventVS());
    }

    public EventVSDto(EventVS eventVS, String serverName, String contextURL) {
        this(eventVS);
        if(eventVS instanceof EventVSElection) {
            this.setURL(contextURL + "/rest/eventVSElection/id/" + eventVS.getId());
            this.setPublishRequestURL(contextURL + "/rest/eventVSElection/id/" + eventVS.getId() + "/publishRequest");
            ControlCenterVS controlCenterVS = eventVS.getControlCenterVS();
            if(controlCenterVS != null) this.setControlCenter(new ActorVSDto(
                    controlCenterVS.getServerURL(), controlCenterVS.getName()));
        }
        this.setAccessControl(new ActorVSDto(contextURL, serverName));
    }

    public static EventVSDto formatToSign(EventVS eventVS) {
        if(!(eventVS instanceof EventVSElection)) return new EventVSDto(eventVS, null, null);
        EventVSDto result = new EventVSDto();
        result.subject = eventVS.getSubject();
        result.content = eventVS.getContent();
        result.dateBegin = eventVS.getDateBegin();
        result.dateFinish = eventVS.getDateFinish();
        result.URL = eventVS.getUrl();
        result.setBackupAvailable(eventVS.getBackupAvailable());
        if(eventVS.getUserVS() != null) result.userVS = eventVS.getUserVS().getNif();
        if(eventVS.getVoteVS() != null) result.voteVS = new VoteVSDto(eventVS.getVoteVS(), null);
        result.accessControlEventVSId = eventVS.getAccessControlEventVSId();
        result.type = eventVS.getType();
        result.id = eventVS.getId();
        result.UUID =  java.util.UUID.randomUUID().toString();
        result.tags = eventVS.getTagList();
        if(eventVS.getControlCenterVS() != null) {
            result.controlCenter = new ActorVSDto(eventVS.getControlCenterVS().getName(),
                    eventVS.getControlCenterVS().getServerURL());
            result.controlCenter.setId(eventVS.getControlCenterVS().getId());
        }
        result.fieldsEventVS = eventVS.getFieldsEventVS();
        result.cardinality = eventVS.getCardinality();
        return result;
    }

    @JsonIgnore
    public EventVSElection getEventVSElection() {
        EventVSElection result = new EventVSElection();
        result.setId(id);
        result.setAccessControlEventVSId(accessControlEventVSId);
        result.setDateCreated(dateCreated);
        result.setSubject(subject);
        result.setContent(getContent());
        result.setDateBegin(dateBegin);
        result.setDateFinish(dateFinish);
        result.setFieldsEventVS(fieldsEventVS);
        result.setTagList(getTags());
        result.setUrl(getURL());
        Set<FieldEventVS> fieldEventVSSet = new HashSet<>(getFieldsEventVS());
        for(FieldEventVS fieldEventVS : fieldEventVSSet) {
            fieldEventVS.setEventVS(result);
        }
        result.setFieldsEventVS(fieldEventVSSet);
        return result;
    }

    public void validate(String serverURL) throws ValidationExceptionVS {
        if(id == null) throw new ValidationExceptionVS("ERROR - missing param - 'id'");
        if(certCAVotacion == null) throw new ValidationExceptionVS("ERROR - missing param - 'certCAVotacion'");
        if(userVS == null) throw new ValidationExceptionVS("ERROR - missing param - 'userVS'");
        if(fieldsEventVS == null) throw new ValidationExceptionVS("ERROR - missing param - 'fieldsEventVS'");
        if(URL == null) throw new ValidationExceptionVS("ERROR - missing param - 'fieldsEventVS'");
        if(controlCenterURL == null) throw new ValidationExceptionVS("ERROR - missing param - 'controlCenterURL'");
        controlCenterURL = StringUtils.checkURL(controlCenterURL);
        if (!controlCenterURL.equals(serverURL)) throw new ValidationExceptionVS("ERROR - expected controlCenterURL:" +
                serverURL + " found: " + controlCenterURL);
    }

    public void validateCancelation(String contextURL) throws ValidationExceptionVS {
        if(operation == null || TypeVS.EVENT_CANCELLATION != operation) throw new ValidationExceptionVS(
                "ERROR - operation expected 'EVENT_CANCELLATION' found: " + operation);
        if(accessControlURL == null) throw new ValidationExceptionVS("ERROR - missing param 'accessControlURL'");
        if(!accessControlURL.equals(contextURL))  throw new ValidationExceptionVS(
                "ERROR - accessControlURL - expected: " + contextURL + " - found: " + accessControlURL);
        if(eventId == null) throw new ValidationExceptionVS("ERROR - missing param 'eventId'");
        if(state == null || (EventVS.State.DELETED_FROM_SYSTEM != state && EventVS.State.CANCELED != state))
            throw new ValidationExceptionVS("ERROR - expected state 'DELETED_FROM_SYSTEM' found: " + state);
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

    public List<String> getTags() {
        return tags;
    }

    public Set<FieldEventVS> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public ActorVSDto getAccessControl() {
        return accessControl;
    }

    public ActorVSDto getControlCenter() {
        return controlCenter;
    }

    public boolean isBackupAvailable() {
        return backupAvailable;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }

    public void setDateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public void setPublishRequestURL(String publishRequestURL) {
        this.publishRequestURL = publishRequestURL;
    }

    public void setVoteVSInfoURL(String voteVSInfoURL) {
        this.voteVSInfoURL = voteVSInfoURL;
    }

    public void setEventCACertificateURL(String eventCACertificateURL) {
        this.eventCACertificateURL = eventCACertificateURL;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setUserVS(String userVS) {
        this.userVS = userVS;
    }

    public void setCardinality(EventVS.Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    public void setState(EventVS.State state) {
        this.state = state;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setFieldsEventVS(Set<FieldEventVS> fieldsEventVS) {
        this.fieldsEventVS = fieldsEventVS;
    }

    public void setAccessControl(ActorVSDto accessControl) {
        this.accessControl = accessControl;
    }

    public void setControlCenter(ActorVSDto controlCenter) {
        this.controlCenter = controlCenter;
    }

    public void setBackupAvailable(boolean backupAvailable) {
        this.backupAvailable = backupAvailable;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public EventVS.Type getType() {
        return type;
    }

    public void setType(EventVS.Type type) {
        this.type = type;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getAccessControlEventVSId() {
        return accessControlEventVSId;
    }

    public void setAccessControlEventVSId(Long accessControlEventVSId) {
        this.accessControlEventVSId = accessControlEventVSId;
    }

    public String getCertCAVotacion() {
        return certCAVotacion;
    }

    public void setCertCAVotacion(String certCAVotacion) {
        this.certCAVotacion = certCAVotacion;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getControlCenterURL() {
        return controlCenterURL;
    }

    public void setControlCenterURL(String controlCenterURL) {
        this.controlCenterURL = controlCenterURL;
    }

    public String getCertChain() {
        return certChain;
    }

    public void setCertChain(String certChain) {
        this.certChain = certChain;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public VoteVSDto getVoteVS() {
        return voteVS;
    }

    public void setVoteVS(VoteVSDto voteVS) {
        this.voteVS = voteVS;
    }
}