package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.dto.Dto;
import org.votingsystem.model.voting.Election;
import org.votingsystem.model.voting.ElectionOption;
import org.votingsystem.util.OperationType;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "Election")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "entityId", "dateBegin", "dateFinish", "subject", "content", "state", "electionOptions", "backupAvailable", "UUID"})
public class ElectionDto implements Dto {

    @JacksonXmlProperty(localName = "Id")
    private Long id;
    @JacksonXmlProperty(localName = "Operation")
    private OperationType operation;
    @JacksonXmlProperty(localName = "DateCreated")
    private ZonedDateTime dateCreated;
    @JacksonXmlProperty(localName = "DateBegin")
    private ZonedDateTime dateBegin;
    @JacksonXmlProperty(localName = "DateFinish")
    private ZonedDateTime dateFinish;
    @JacksonXmlProperty(localName = "Subject")
    private String subject;
    @JacksonXmlProperty(localName = "Content")
    private String content;
    @JacksonXmlProperty(localName = "Publisher")
    private String publisher;
    @JacksonXmlProperty(localName = "State")
    private Election.State state;
    @JacksonXmlElementWrapper(useWrapping = true, localName = "Options")
    @JacksonXmlProperty(localName = "Option")
    private Set<ElectionOptionDto> electionOptions;
    @JacksonXmlProperty(localName = "BackupAvailable")
    private Boolean backupAvailable;
    @JacksonXmlProperty(localName = "CertChain")
    private String certChain;
    @JacksonXmlProperty(localName = "EntityId")
    private String entityId;
    @JacksonXmlProperty(localName = "UUID")
    private String UUID;

    public ElectionDto() {}

    public ElectionDto(String entityId) {
        this.entityId = entityId;
    }

    public ElectionDto(Election election) {
        this.id = election.getId();
        if(election.getDateCreated() != null) {
            this.dateCreated = election.getDateCreated().atZone(ZoneId.systemDefault());
        }
        if(election.getDateBegin() != null) {
            this.dateBegin = election.getDateBegin().atZone(ZoneId.systemDefault());
        }
        if(election.getDateFinish() != null) {
            this.dateFinish = election.getDateFinish().atZone(ZoneId.systemDefault());
        }
        this.subject = election.getSubject();
        this.content = election.getContent();
        if(election.getPublisher() != null) {
            this.publisher = election.getPublisher().getName() + " " + election.getPublisher().getSurname();
        }
        this.backupAvailable = election.getBackupAvailable();
        this.state = election.getState();
        if(election.getElectionOptions() != null) {
            electionOptions = new HashSet<>();
            for(ElectionOption electionOption : election.getElectionOptions()) {
                electionOptions.add(new ElectionOptionDto(electionOption));
            }
        }
        this.entityId = election.getEntityId();
        this.UUID = election.getUUID();
    }

    public void validate(String serverURL) throws ValidationException {
        if(id == null) throw new ValidationException("ERROR - missing param - 'id'");
        if(publisher == null) throw new ValidationException("ERROR - missing param - 'publisher'");
        if(electionOptions == null) throw new ValidationException("ERROR - missing param - 'electionOptions'");
    }

    public void validateCancelation(String contextURL) throws ValidationException {
        if(operation == null || OperationType.ELECTION_CANCELLATION != operation) throw new ValidationException(
                "ERROR - operation expected 'ELECTION_CANCELLATION' found: " + operation);
        if(state == null || (Election.State.DELETED_FROM_SYSTEM != state && Election.State.CANCELED != state))
            throw new ValidationException("ERROR - expected state 'DELETED_FROM_SYSTEM' found: " + state);
    }

    public Long getId() {
        return id;
    }

    public ZonedDateTime getDateCreated() {
        return dateCreated;
    }

    public ZonedDateTime getDateBegin() {
        return dateBegin;
    }

    public ZonedDateTime getDateFinish() {
        return dateFinish;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getPublisher() {
        return publisher;
    }

    public Election.State getState() {
        return state;
    }

    public Set<ElectionOptionDto> getElectionOptions() {
        return electionOptions;
    }

    public Boolean isBackupAvailable() {
        return backupAvailable;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDateCreated(ZonedDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setDateBegin(ZonedDateTime dateBegin) {
        this.dateBegin = dateBegin;
    }

    public void setDateFinish(ZonedDateTime dateFinish) {
        this.dateFinish = dateFinish;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setState(Election.State state) {
        this.state = state;
    }

    public void setElectionOptions(Set<ElectionOptionDto> electionOptions) {
        this.electionOptions = electionOptions;
    }

    public void setBackupAvailable(Boolean backupAvailable) {
        this.backupAvailable = backupAvailable;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    @JsonIgnore
    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getCertChain() {
        return certChain;
    }

    public void setCertChain(String certChain) {
        this.certChain = certChain;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @JsonIgnore
    public void validatePublishRequest() throws ValidationException {
        if(dateFinish.isBefore(dateBegin)) throw new ValidationException("Date begin is after date finish");
        if(LocalDateTime.now().isAfter(dateFinish.toLocalDateTime())) throw new ValidationException("Bad date finish");
        if(electionOptions == null || electionOptions.size() < 2) throw new ValidationException("Bad number of election options");
    }
}