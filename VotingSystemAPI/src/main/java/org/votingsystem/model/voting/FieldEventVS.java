package org.votingsystem.model.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;


@Entity @Table(name="FieldEventVS")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldEventVS extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(VoteVS.class.getSimpleName());

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="accessControlFieldEventId") private Long accessControlFieldEventId;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVS", nullable=false) @JsonIgnore private EventVS eventVS;
    @Column(name="content", length=10000, nullable=false) private String content;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @Transient private String value;
    @Transient private Long numVoteRequests;
    @Transient private Long numUsersWithVote;
    @Transient private Long numRepresentativesWithVote;
    @Transient private Long numVotesResult;
    @Transient private Long numVotesVS;

    public FieldEventVS() {}

    public FieldEventVS(String content, String value) {
        this.content = content;
        this.value = value;
    }

    public FieldEventVS(EventVS eventVS, String content) {
        this.eventVS = eventVS;
        this.content = content;
    }

    public FieldEventVS(EventVS eventVS, String content, Long accessControlFieldEventId) {
        this.eventVS = eventVS;
        this.content = content;
        this.accessControlFieldEventId = accessControlFieldEventId;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public EventVS getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getNumVoteRequests() {
        return numVoteRequests;
    }

    public void setNumVoteRequests(Long numVoteRequests) {
        this.numVoteRequests = numVoteRequests;
    }

    public Long getNumUsersWithVote() {
        return numUsersWithVote;
    }

    public void setNumUsersWithVote(Long numUsersWithVote) {
        this.numUsersWithVote = numUsersWithVote;
    }

    public Long getNumRepresentativesWithVote() {
        return numRepresentativesWithVote;
    }

    public void setNumRepresentativesWithVote(Long numRepresentativesWithVote) {
        this.numRepresentativesWithVote = numRepresentativesWithVote;
    }

    public Long getNumVotesResult() {
        return numVotesResult;
    }

    public void setNumVotesResult(Long numVotesResult) {
        this.numVotesResult = numVotesResult;
    }


    public Long getAccessControlFieldEventId() {
        return accessControlFieldEventId;
    }

    public void setAccessControlFieldEventId(Long accessControlFieldEventId) {
        this.accessControlFieldEventId = accessControlFieldEventId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Long getNumVotesVS() {
        return numVotesVS;
    }

    public void setNumVotesVS(Long numVotesVS) {
        this.numVotesVS = numVotesVS;
    }

    @Override
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}