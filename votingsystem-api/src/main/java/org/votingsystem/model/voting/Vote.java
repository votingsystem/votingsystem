package org.votingsystem.model.voting;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.EntityBase;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.util.Constants;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="VOTE")
public class Vote extends EntityBase implements Serializable {

    private static Logger log = Logger.getLogger(Vote.class.getName());

    public enum State{
        @JsonProperty("OK")
        OK,
        @JsonProperty("CANCELED")
        CANCELED,
        @JsonProperty("ERROR")
        ERROR}

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;
    @OneToOne
    @JoinColumn(name="SIGNED_DOCUMENT_ID")
    private SignedDocument signedDocument;
    @Column(name="IDENTITY_SERVICE_ENTITY_ID", nullable = false)
    private String identityServiceEntity;
    @OneToOne
    @JoinColumn(name="CERTIFICATE_ID")
    private Certificate certificate;
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="OPTION_SELECTED_ID")
    private ElectionOption optionSelected;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ELECTION_ID")
    private Election election;
    @Column(name="STATE", nullable=false)
    @Enumerated(EnumType.STRING) private State state;
    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;


    @Transient private String voteUUID;
    @Transient private String originRevocationHash;
    @Transient private String revocationHash;
    @Transient private String originHashAccessRequest;
    @Transient private String hashAccessRequestBase64;
    @Transient private String votingServiceEntity;
    @Transient private String electionUUID;
    @Transient private X509Certificate x509Certificate;
    @Transient private TimeStampToken timeStampToken;
    @Transient private Set<X509Certificate> serverCerts = new HashSet<>();
    @Transient private SignedDocument receipt;
    @Transient private boolean isValid = false;

    public Vote () {}

    public Vote (X509Certificate x509Certificate, TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
        this.x509Certificate = x509Certificate;
    }

    public Vote (ElectionOption optionSelected, Election election, State state, Certificate certificate,
                 SignedDocument signedDocument, String identityServiceEntity) {
        this.optionSelected = optionSelected;
        this.election = election;
        this.state = state;
        this.certificate = certificate;
        this.signedDocument = signedDocument;
        this.identityServiceEntity = identityServiceEntity;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public ElectionOption getOptionSelected() {
        return optionSelected;
    }

    public void setOptionSelected(ElectionOption optionSelected) {
        this.optionSelected = optionSelected;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public void setOriginRevocationHash(String originRevocationHash) {
        this.originRevocationHash = originRevocationHash;
    }

    public String getOriginHashAccessRequest() {
        return originHashAccessRequest;
    }

    public void setOriginHashAccessRequest(String originHashAccessRequest) {
        this.originHashAccessRequest = originHashAccessRequest;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
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

	public void setFieldEvent(ElectionOption optionSelected) {
		this.setOptionSelected(optionSelected);
	}

	public ElectionOption getFieldEvent() {
		return getOptionSelected();
	}

	public SignedDocument getSignedDocument() {
		return signedDocument;
	}

	public void setSignedDocument(SignedDocument signedDocument) {
		this.signedDocument = signedDocument;
	}

    public String getElectionUUID() {
        return electionUUID;
    }

    public void setElectionUUID(String electionUUID) {
        this.electionUUID = electionUUID;
    }

	public State getState() {
		return state;
	}

	public Vote setState(State state) {
		this.state = state;
        return this;
	}

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

	public Certificate getCertificate() {
		return certificate;
	}

	public void setCertificate(Certificate certificate) {
		this.certificate = certificate;
	}

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public Set<X509Certificate> getServerCerts() {
        return serverCerts;
    }

    public boolean isValid() { return isValid; }

    public void setValid(boolean isValid) { this.isValid = isValid; }

    public SignedDocument getReceipt() { return receipt; }

    public void setReceipt(SignedDocument receipt) { this.receipt = receipt; }

    public void setServerCerts(Set<X509Certificate> serverCerts) { this.serverCerts = serverCerts; }

    public X509Certificate getX509Certificate() { return x509Certificate; }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    public String getVoteUUID() {
        return voteUUID;
    }

    public void setVoteUUID(String voteUUID) {
        this.voteUUID = voteUUID;
    }

    private void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    private void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }


    public String getIdentityServiceEntity() {
        return identityServiceEntity;
    }

    public void setIdentityServiceEntity(String identityServiceEntity) {
        this.identityServiceEntity = identityServiceEntity;
    }

    public String getVotingServiceEntity() {
        return votingServiceEntity;
    }

    public void setVotingServiceEntity(String votingServiceEntity) {
        this.votingServiceEntity = votingServiceEntity;
    }

    public Vote loadSignatureData(X509Certificate x509Certificate, TimeStampToken timeStampToken) throws Exception {
        this.timeStampToken = timeStampToken;
        this.x509Certificate = x509Certificate;
        CertVoteExtensionDto certExtensionDto = CertificateUtils.getCertExtensionData(CertVoteExtensionDto.class,
                x509Certificate, Constants.VOTE_OID);
        this.electionUUID = certExtensionDto.getElectionUUID();
        this.identityServiceEntity = certExtensionDto.getIdentityServiceEntity();
        this.votingServiceEntity = certExtensionDto.getVotingServiceEntity();
        this.revocationHash = certExtensionDto.getRevocationHash();
        return this;
    }

}