package org.votingsystem.model;

import org.apache.log4j.Logger;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;

@Entity @Table(name="FieldEventVS")
public class FieldEventVS implements Serializable {

    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(VoteVS.class);

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="accessControlFieldEventId") private Long accessControlFieldEventId;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVS", nullable=false) private EventVS eventVS;
    @Column(name="content", length=10000, nullable=false) private String content;

    @Transient private String value;
    @Transient private Long numVoteRequests;
    @Transient private Long numUsersWithVote;
    @Transient private Long numRepresentativesWithVote;
    @Transient private Long numVotesResult;

    public FieldEventVS() {}

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

    public static FieldEventVS populate (Map fieldMap) {
        FieldEventVS fieldEvent = null;
        try {
            fieldEvent = new FieldEventVS();
            if(fieldMap.containsKey("id")) fieldEvent.setId(((Integer) fieldMap.get("id")).longValue());
            if(fieldMap.containsKey("content")) {
                fieldEvent.setContent((String) fieldMap.get("content"));
            }
            if(fieldMap.containsKey("value")) {
                fieldEvent.setValue((String) fieldMap.get("value"));
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return fieldEvent;
    }

}