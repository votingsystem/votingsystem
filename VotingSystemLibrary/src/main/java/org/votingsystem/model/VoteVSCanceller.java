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
	
	public enum State {CANCELLATION_WITHOUT_VOTE, CANCELLATION_OK, CANCELLED, ERROR}
	
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @OneToOne private MessageSMIME messageSMIME;
    @OneToOne private AccessRequestVS accessRequestVS;
    @OneToOne private VoteRequestCsrVS voteRequestCsrVS;
    @OneToOne private VoteVS voteVS;
    @Enumerated(EnumType.STRING)
    @Column(name="state", nullable=false)
    private State state;
    @Column(name="hashCertVoteBase64") private String hashCertVoteBase64;
    @Column(name="originHashCertVoteBase64") private String originHashCertVoteBase64;
    @Column(name="hashAccessRequestBase64") private String hashAccessRequestBase64;
    @Column(name="originHashAccessRequestBase64") private String originHashAccessRequestBase64;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVSElection", nullable=false)
    private EventVSElection eventVSElection;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23)
    private Date dateCreated;

     /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
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

	public String getHashCertVoteBase64() {
		return hashCertVoteBase64;
	}

	public void setHashCertVoteBase64(String hashCertVoteBase64) {
		this.hashCertVoteBase64 = hashCertVoteBase64;
	}

	public String getOriginHashCertVoteBase64() {
		return originHashCertVoteBase64;
	}

	public void setOriginHashCertVoteBase64(
			String originHashCertVoteBase64) {
		this.originHashCertVoteBase64 = originHashCertVoteBase64;
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

	public VoteRequestCsrVS getVoteRequestCsrVS() {
		return voteRequestCsrVS;
	}

	public void setVoteRequestCsrVS(VoteRequestCsrVS solicitudCSR) {
		this.voteRequestCsrVS = solicitudCSR;
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
