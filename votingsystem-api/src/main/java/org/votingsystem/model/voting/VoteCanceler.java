package org.votingsystem.model.voting;

import org.votingsystem.model.EntityBase;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="VOTE_CANCELER")
public class VoteCanceler extends EntityBase implements Serializable {


    public enum State {CANCELLATION_WITHOUT_VOTE,//Access request without vote
	    CANCELLATION_OK, CANCELLATION_ERROR}
	
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;
    @OneToOne
	@JoinColumn(name="SIGNED_DOCUMENT_ID")
	private SignedDocument signedDocument;
    @OneToOne
	@JoinColumn(name="ANON_VOTE_CERT_REQUEST_ID")
	private AnonVoteCertRequest accessRequest;
    @OneToOne @JoinColumn(name="VOTE_ID")
	private Vote vote;
    @Enumerated(EnumType.STRING)
    @Column(name="STATE", nullable=false)
    private State state;
    @Column(name="CERT_REVOCATION_HASH_BASE64") private String certRevocationHashBase64;
    @Column(name="ORIGIN_CERT_REVOCATION_HASH_BASE64") private String originCertRevocationHashBase64;
    @Column(name="ANON_CERT_REQUEST_REVOCATION_HASH_BASE64") private String anonCertRequestRevocationHashBase64;
    @Column(name="ORIGIN_ANON_CERT_REQUEST_REVOCATION_HASH_BASE64") private String originAnonCertRequestRevocationHashBase64;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ELECTION", nullable=false)
    private Election election;
	@Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
	@Convert(converter = LocalDateTimeAttributeConverter.class)
	private LocalDateTime dateCreated;
	@Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
	@Convert(converter = LocalDateTimeAttributeConverter.class)
	private LocalDateTime lastUpdated;

    public VoteCanceler() {}

    public VoteCanceler(SignedDocument signedDocument, AnonVoteCertRequest accessRequest, State state,
				String originAnonCertRequestRevocationHashBase64, String anonCertRequestRevocationHashBase64,
				String originCertRevocationHashBase64, String certRevocationHashBase64, Vote vote) {
        this.signedDocument = signedDocument;
        this.accessRequest = accessRequest;
        this.state = state;
        this.originAnonCertRequestRevocationHashBase64 = originAnonCertRequestRevocationHashBase64;
        this.anonCertRequestRevocationHashBase64 = anonCertRequestRevocationHashBase64;
        this.originCertRevocationHashBase64 = originCertRevocationHashBase64;
        this.certRevocationHashBase64 = certRevocationHashBase64;
        this.election = vote.getElection();
        this.vote = vote;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

	public void setSignedDocument(SignedDocument cmsMessage) {
		this.signedDocument = cmsMessage;
	}

	public SignedDocument getSignedDocument() {
		return signedDocument;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setElection(Election election) {
		this.election = election;
	}

	public Election getElection() {
		return election;
	}

	public AnonVoteCertRequest getAccessRequest() {
		return accessRequest;
	}

	public void setAccessRequest(AnonVoteCertRequest accessRequest) {
		this.accessRequest = accessRequest;
	}

	public String getCertRevocationHashBase64() {
		return certRevocationHashBase64;
	}

	public void setCertRevocationHashBase64(String certRevocationHashBase64) {
		this.certRevocationHashBase64 = certRevocationHashBase64;
	}

	public String getOriginCertRevocationHashBase64() {
		return originCertRevocationHashBase64;
	}

	public void setOriginCertRevocationHashBase64(
			String originCertRevocationHashBase64) {
		this.originCertRevocationHashBase64 = originCertRevocationHashBase64;
	}

	public String getAccessRequestHashBase64() {
		return anonCertRequestRevocationHashBase64;
	}

	public void setAccessRequestHashBase64(String hashAccessRequestBase64) {
		this.anonCertRequestRevocationHashBase64 = hashAccessRequestBase64;
	}

	public String getOriginAnonCertRequestRevocationHashBase64() {
		return originAnonCertRequestRevocationHashBase64;
	}

	public void setOriginAnonCertRequestRevocationHashBase64(
			String originAnonCertRequestRevocationHashBase64) {
		this.originAnonCertRequestRevocationHashBase64 = originAnonCertRequestRevocationHashBase64;
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

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
