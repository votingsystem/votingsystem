package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.model.voting.ControlCenter;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.FieldEvent;
import org.votingsystem.throwable.ValidationException;
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
    private String eventCACertificateURL;
    private String subject;
    private String content;
    private String duration;
    private String user;
    private EventVS.Cardinality cardinality;
    private EventVS.State state;
    private List<String> tags;
    private Set<FieldEvent> fieldsEventVS;
    private ActorDto accessControl;
    private ActorDto controlCenter;
    private boolean backupAvailable;
    private TypeVS operation;
    private EventVS.Type type;
    private Long eventId;
    private Long accessControlEventId;
    private String certCAVotacion;
    private String controlCenterURL;
    private String certChain;
    private String accessControlURL;
    private String serverURL;
    private VoteDto vote;
    private String UUID;

    public EventVSDto() {}

    public EventVSDto(String serverURL) {
        this.serverURL = serverURL;
    }

    public EventVSDto(EventVS eventVS) {
        this.setId(eventVS.getId());
        this.setURL(eventVS.getUrl());
        this.setDateCreated(eventVS.getDateCreated());
        this.setSubject(eventVS.getSubject());
        this.setContent(eventVS.getContent());
        if(eventVS.getUser() != null) this.setUser(eventVS.getUser().getFirstName() + " " + eventVS.getUser().getLastName());
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
        if(eventVS instanceof EventElection) {
            this.setURL(contextURL + "/rest/eventElection/id/" + eventVS.getId());
            ControlCenter controlCenter = eventVS.getControlCenter();
            if(controlCenter != null) this.setControlCenter(new ActorDto(
                    controlCenter.getServerURL(), controlCenter.getName()));
        }
        this.setAccessControl(new ActorDto(contextURL, serverName));
    }

    public static EventVSDto formatToSign(EventVS eventVS) {
        if(!(eventVS instanceof EventElection)) return new EventVSDto(eventVS, null, null);
        EventVSDto result = new EventVSDto();
        result.subject = eventVS.getSubject();
        result.content = eventVS.getContent();
        result.dateBegin = eventVS.getDateBegin();
        result.dateFinish = eventVS.getDateFinish();
        result.URL = eventVS.getUrl();
        result.setBackupAvailable(eventVS.getBackupAvailable());
        if(eventVS.getUser() != null) result.user = eventVS.getUser().getNif();
        if(eventVS.getVote() != null) result.vote = new VoteDto(eventVS.getVote(), null);
        result.accessControlEventId = eventVS.getAccessControlEventId();
        result.type = eventVS.getType();
        result.id = eventVS.getId();
        result.UUID =  java.util.UUID.randomUUID().toString();
        result.tags = eventVS.getTagList();
        if(eventVS.getControlCenter() != null) {
            result.controlCenter = new ActorDto(eventVS.getControlCenter().getName(),
                    eventVS.getControlCenter().getServerURL());
            result.controlCenter.setId(eventVS.getControlCenter().getId());
        }
        result.fieldsEventVS = eventVS.getFieldsEventVS();
        result.cardinality = eventVS.getCardinality();
        return result;
    }

    @JsonIgnore
    public EventElection getEventElection() {
        EventElection result = new EventElection();
        result.setId(id);
        result.setAccessControlEventId(accessControlEventId);
        result.setDateCreated(dateCreated);
        result.setSubject(subject);
        result.setContent(getContent());
        result.setDateBegin(dateBegin);
        result.setDateFinish(dateFinish);
        result.setFieldsEventVS(fieldsEventVS);
        result.setTagList(getTags());
        result.setUrl(getURL());
        Set<FieldEvent> fieldEventSet = new HashSet<>(getFieldsEventVS());
        for(FieldEvent fieldEvent : fieldEventSet) {
            fieldEvent.setEventVS(result);
        }
        result.setFieldsEventVS(fieldEventSet);
        return result;
    }

    public void validate(String serverURL) throws ValidationException {
        if(id == null) throw new ValidationException("ERROR - missing param - 'id'");
        if(certCAVotacion == null) throw new ValidationException("ERROR - missing param - 'certCAVotacion'");
        if(user == null) throw new ValidationException("ERROR - missing param - 'user'");
        if(fieldsEventVS == null) throw new ValidationException("ERROR - missing param - 'fieldsEventVS'");
        if(URL == null) throw new ValidationException("ERROR - missing param - 'fieldsEventVS'");
        if(controlCenterURL == null) throw new ValidationException("ERROR - missing param - 'controlCenterURL'");
        controlCenterURL = StringUtils.checkURL(controlCenterURL);
        if (!controlCenterURL.equals(serverURL)) throw new ValidationException("ERROR - expected controlCenterURL:" +
                serverURL + " found: " + controlCenterURL);
    }

    public void validateCancelation(String contextURL) throws ValidationException {
        if(operation == null || TypeVS.EVENT_CANCELLATION != operation) throw new ValidationException(
                "ERROR - operation expected 'EVENT_CANCELLATION' found: " + operation);
        if(accessControlURL == null) throw new ValidationException("ERROR - missing param 'accessControlURL'");
        if(!accessControlURL.equals(contextURL))  throw new ValidationException(
                "ERROR - accessControlURL - expected: " + contextURL + " - found: " + accessControlURL);
        if(eventId == null) throw new ValidationException("ERROR - missing param 'eventId'");
        if(state == null || (EventVS.State.DELETED_FROM_SYSTEM != state && EventVS.State.CANCELED != state))
            throw new ValidationException("ERROR - expected state 'DELETED_FROM_SYSTEM' found: " + state);
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

    public String getUser() {
        return user;
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

    public Set<FieldEvent> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public ActorDto getAccessControl() {
        return accessControl;
    }

    public ActorDto getControlCenter() {
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

    public void setUser(String user) {
        this.user = user;
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

    public void setFieldsEventVS(Set<FieldEvent> fieldsEventVS) {
        this.fieldsEventVS = fieldsEventVS;
    }

    public void setAccessControl(ActorDto accessControl) {
        this.accessControl = accessControl;
    }

    public void setControlCenter(ActorDto controlCenter) {
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

    public Long getAccessControlEventId() {
        return accessControlEventId;
    }

    public void setAccessControlEventId(Long accessControlEventId) {
        this.accessControlEventId = accessControlEventId;
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

    public VoteDto getVote() {
        return vote;
    }

    public void setVote(VoteDto vote) {
        this.vote = vote;
    }
}