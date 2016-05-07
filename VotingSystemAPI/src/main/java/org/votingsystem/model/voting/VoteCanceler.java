package org.votingsystem.model.voting;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="VoteCanceler")
public class VoteCanceler extends EntityVS implements Serializable {


    public enum State {CANCELLATION_WITHOUT_VOTE,//Access request without vote
	    CANCELLATION_OK, CANCELLATION_ERROR}
	
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @OneToOne private CMSMessage cmsMessage;
    @OneToOne private AccessRequest accessRequest;
    @OneToOne private Vote vote;
    @Enumerated(EnumType.STRING)
    @Column(name="state", nullable=false)
    private State state;
    @Column(name="hashCertVSBase64") private String hashCertVSBase64;
    @Column(name="originHashCertVSBase64") private String originHashCertVSBase64;
    @Column(name="hashAccessRequestBase64") private String hashAccessRequestBase64;
    @Column(name="originHashAccessRequestBase64") private String originHashAccessRequestBase64;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventElection", nullable=false)
    private EventElection eventElection;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated") private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated") private Date lastUpdated;

    public VoteCanceler() {}

    public VoteCanceler(CMSMessage cmsMessage, AccessRequest accessRequest, State state,
						  String originHashAccessRequestBase64, String hashAccessRequestBase64, String originHashCertVSBase64,
						  String hashCertVSBase64, Vote vote) {
        this.cmsMessage = cmsMessage;
        this.accessRequest = accessRequest;
        this.state = state;
        this.originHashAccessRequestBase64 = originHashAccessRequestBase64;
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
        this.originHashCertVSBase64 = originHashCertVSBase64;
        this.hashCertVSBase64 = hashCertVSBase64;
        this.eventElection = (EventElection) vote.getEventVS();
        this.vote = vote;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

	public void setCmsMessage(CMSMessage cmsMessage) {
		this.cmsMessage = cmsMessage;
	}

	public CMSMessage getCmsMessage() {
		return cmsMessage;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setEventElection(EventElection eventElection) {
		this.eventElection = eventElection;
	}

	public EventElection getEventElection() {
		return eventElection;
	}

	public AccessRequest getAccessRequest() {
		return accessRequest;
	}

	public void setAccessRequest(AccessRequest accessRequest) {
		this.accessRequest = accessRequest;
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

	public Vote getVote() {
		return vote;
	}

	public void setVote(Vote vote) {
		this.vote = vote;
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
