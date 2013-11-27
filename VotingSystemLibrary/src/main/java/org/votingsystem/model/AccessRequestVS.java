package org.votingsystem.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity @Table(name="AccessRequestVS")
public class AccessRequestVS implements Serializable {

    public enum State {OK, CANCELLED, ERROR}

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVSElection") private EventVSElection eventVSElection;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @OneToOne private MessageSMIME messageSMIME;
    @OneToOne(mappedBy="accessRequestVS") private VoteVSCanceller voteVSCanceller;
    @Column(name="hashAccessRequestBase64") private String hashAccessRequestBase64;
    @Column(name="metainf") private String metaInf;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;


    public void setId(Long id) { this.id = id; }

    public Long getId() {return id;}

    public void setEventVSElection(EventVSElection eventVSElection) {  this.eventVSElection = eventVSElection; }

    public EventVSElection getEventVSElection() { return eventVSElection; }

    public void setState(State state) { this.state = state; }

    public State getState() { return state; }

    public String getAccessRequestHashBase64() { return hashAccessRequestBase64; }

    public void setAccessRequestHashBase64( String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public MessageSMIME getMessageSMIME() { return messageSMIME;  }

    public void setMessageSMIME(MessageSMIME messageSMIME) { this.messageSMIME = messageSMIME; }

    public UserVS getUserVS() { return userVS; }

    public void setUserVS(UserVS userVS) { this.userVS = userVS; }

    public VoteVSCanceller getVoteVSCanceller() { return voteVSCanceller; }

    public void setVoteVSCanceller(VoteVSCanceller voteVSCanceller) { this.voteVSCanceller = voteVSCanceller; }

    public Date getDateCreated() { return dateCreated; }

    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated;  }

    public Date getLastUpdated() { return lastUpdated; }

    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getMetaInf() { return metaInf;  }

    public void setMetaInf(String metaInf) { this.metaInf = metaInf; }

}
