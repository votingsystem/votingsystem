package org.votingsystem.model.voting;

import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity @Table(name="AccessRequestVS")
public class AccessRequestVS extends EntityVS implements Serializable {

    public enum State {OK, CANCELED, ERROR}

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVS") private EventVS eventVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @OneToOne private MessageSMIME messageSMIME;
    @OneToOne(mappedBy="accessRequestVS") private VoteVSCanceler voteVSCanceler;
    @Column(name="hashAccessRequestBase64") private String hashAccessRequestBase64;
    @Column(name="metainf") private String metaInf;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public AccessRequestVS() {}

    public AccessRequestVS(UserVS userVS, MessageSMIME messageSMIME, State state, String hashAccessRequestBase64,
                           EventVS eventVS) {
        this.userVS = userVS;
        this.messageSMIME = messageSMIME;
        this.state = state;
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
        this.eventVS = eventVS;
    }

    public void setId(Long id) { this.id = id; }

    public Long getId() {return id;}

    public void setEventVSElection(EventVS eventVS) {  this.eventVS = eventVS; }

    public EventVS getEventVSElection() { return eventVS; }

    public AccessRequestVS setState(State state) {
        this.state = state;
        return this;
    }

    public State getState() { return state; }

    public String getAccessRequestHashBase64() { return hashAccessRequestBase64; }

    public void setAccessRequestHashBase64( String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public MessageSMIME getMessageSMIME() { return messageSMIME;  }

    public void setMessageSMIME(MessageSMIME messageSMIME) { this.messageSMIME = messageSMIME; }

    public UserVS getUserVS() { return userVS; }

    public void setUserVS(UserVS userVS) { this.userVS = userVS; }

    public VoteVSCanceler getVoteVSCanceler() { return voteVSCanceler; }

    public void setVoteVSCanceler(VoteVSCanceler voteVSCanceler) { this.voteVSCanceler = voteVSCanceler; }

    public Date getDateCreated() { return dateCreated; }

    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated;  }

    public Date getLastUpdated() { return lastUpdated; }

    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getMetaInf() { return metaInf;  }

    public void setMetaInf(String metaInf) { this.metaInf = metaInf; }

}
