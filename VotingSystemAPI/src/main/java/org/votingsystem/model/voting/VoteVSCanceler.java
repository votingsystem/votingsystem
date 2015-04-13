package org.votingsystem.model.voting;

import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.voting.AccessRequestVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="VoteVSCanceler")
public class VoteVSCanceler extends EntityVS implements Serializable {


    public enum State {CANCELLATION_WITHOUT_VOTE,//Access request without vote
	    CANCELLATION_OK, CANCELLATION_ERROR}
	
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @OneToOne private MessageSMIME messageSMIME;
    @OneToOne private AccessRequestVS accessRequestVS;
    @OneToOne private VoteVS voteVS;
    @Enumerated(EnumType.STRING)
    @Column(name="state", nullable=false)
    private State state;
    @Column(name="hashCertVSBase64") private String hashCertVSBase64;
    @Column(name="originHashCertVSBase64") private String originHashCertVSBase64;
    @Column(name="hashAccessRequestBase64") private String hashAccessRequestBase64;
    @Column(name="originHashAccessRequestBase64") private String originHashAccessRequestBase64;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVSElection", nullable=false)
    private EventVSElection eventVSElection;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public VoteVSCanceler() {}

    public VoteVSCanceler(MessageSMIME messageSMIME, AccessRequestVS accessRequestVS, State state,
                          String originHashAccessRequestBase64, String hashAccessRequestBase64, String originHashCertVSBase64,
                          String hashCertVSBase64, VoteVS voteVS) {
        this.messageSMIME = messageSMIME;
        this.accessRequestVS = accessRequestVS;
        this.state = state;
        this.originHashAccessRequestBase64 = originHashAccessRequestBase64;
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
        this.originHashCertVSBase64 = originHashCertVSBase64;
        this.hashCertVSBase64 = hashCertVSBase64;
        this.eventVSElection = (EventVSElection) voteVS.getEventVS();
        this.voteVS = voteVS;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

	public void setMessageSMIME(MessageSMIME messageSMIME) {
		this.messageSMIME = messageSMIME;
	}

	public MessageSMIME getMessageSMIME() {
		return messageSMIME;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setEventVSElection(EventVSElection eventVSElection) {
		this.eventVSElection = eventVSElection;
	}

	public EventVSElection getEventVSElection() {
		return eventVSElection;
	}

	public AccessRequestVS getAccessRequestVS() {
		return accessRequestVS;
	}

	public void setAccessRequestVS(AccessRequestVS accessRequestVS) {
		this.accessRequestVS = accessRequestVS;
	}

	public String getHashCertVSBase64() {
		return hashCertVSBase64;
	}

	public void setHashCertVSBase64(String hashCertVSBase64) {
		this.hashCertVSBase64 = hashCertVSBase64;
	}

	public String getOriginHashCertVSBase64() {
		return originHashCertVSBase64;
	}

	public void setOriginHashCertVSBase64(
			String originHashCertVSBase64) {
		this.originHashCertVSBase64 = originHashCertVSBase64;
	}

	public String getAccessRequestHashBase64() {
		return hashAccessRequestBase64;
	}

	public void setAccessRequestHashBase64(String hashAccessRequestBase64) {
		this.hashAccessRequestBase64 = hashAccessRequestBase64;
	}

	public String getOriginHashAccessRequestBase64() {
		return originHashAccessRequestBase64;
	}

	public void setOriginHashAccessRequestBase64(
			String originHashAccessRequestBase64) {
		this.originHashAccessRequestBase64 = originHashAccessRequestBase64;
	}

	public VoteVS getVoteVS() {
		return voteVS;
	}

	public void setVoteVS(VoteVS voteVS) {
		this.voteVS = voteVS;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

    public Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
