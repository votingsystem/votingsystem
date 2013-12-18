package org.votingsystem.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="VoteVSCanceller")
public class VoteVSCanceller implements Serializable {
	
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
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23)
    private Date dateCreated;

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


}
