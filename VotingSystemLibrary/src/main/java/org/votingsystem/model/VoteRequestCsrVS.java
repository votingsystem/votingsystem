package org.votingsystem.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity @Table(name="VoteRequestCsrVS")
public class VoteRequestCsrVS implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public enum State {OK, PENDING, REJECTED, CANCELLED}
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="serialNumber") private Long serialNumber;
    @Column(name="content", nullable=false) @Lob private byte[] content;
    @OneToOne(mappedBy="voteRequestCsrVS") private CertificateVS certificateVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVSElection") private EventVSElection eventVSElection;
    @Enumerated(EnumType.STRING)
    @Column(name="state", nullable=false) private State state;
    @OneToOne(mappedBy="voteRequestCsrVS") private VoteVSCanceller voteVSCanceller;
    @Column(name="hashCertVoteBase64") private String hashCertVoteBase64;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23, insertable=true) private Date lastUpdated;


    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public CertificateVS getCertificateVS() {
		return certificateVS;
	}

	public void setCertificateVS(CertificateVS certificateVS) {
		this.certificateVS = certificateVS;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public EventVSElection getEventVSElection() {
		return eventVSElection;
	}

	public void setEventVSElection(EventVSElection eventVSElection) {
		this.eventVSElection = eventVSElection;
	}

	public VoteVSCanceller getVoteVSCanceller() {
		return voteVSCanceller;
	}

	public void setVoteVSCanceller(VoteVSCanceller voteVSCanceller) {
		this.voteVSCanceller = voteVSCanceller;
	}

	public String getHashCertVoteBase64() {
		return hashCertVoteBase64;
	}

	public void setHashCertVoteBase64(String hashCertVoteBase64) {
		this.hashCertVoteBase64 = hashCertVoteBase64;
	}

}
