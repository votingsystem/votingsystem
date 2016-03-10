package org.votingsystem.model.voting;

import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.VoteCertExtensionDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.crypto.CertUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="Vote")
public class Vote extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(Vote.class.getName());

    public enum State{OK, CANCELED, ERROR}

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @OneToOne private CMSMessage cmsMessage;
    @OneToOne private CertificateVS certificateVS;
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="optionSelected") private FieldEventVS optionSelected;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventElection") private EventElection eventVS;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @Column(name="dateCreated", length=23) @Temporal(TemporalType.TIMESTAMP) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;


    @Transient private String voteUUID;
    @Transient private String hashCertVSBase64;
    @Transient private String hashAccessRequestBase64;
    @Transient private String accessControlURL;
    @Transient private String eventURL;
    @Transient private Long accessControlEventId;
    @Transient private String representativeURL;
    @Transient private String originHashCertVote;
    @Transient private String originHashAccessRequest;
    @Transient private X509Certificate x509Certificate;
    @Transient private TimeStampToken timeStampToken;
    @Transient private Set<X509Certificate> serverCerts = new HashSet<>();
    @Transient private CMSSignedMessage receipt;
    @Transient private boolean isValid = false;

    public Vote () {}

    public Vote (VoteDto voteDto) {
        id = voteDto.getId();
        eventVS = new EventElection();
        eventVS.setId(voteDto.getEventId());
        eventVS.setUrl(voteDto.getEventURL());
        eventURL = voteDto.getEventURL();
        hashCertVSBase64 = voteDto.getHashCertVSBase64();
        optionSelected = voteDto.getOptionSelected();
        voteUUID = voteDto.getUUID();
        state = voteDto.getState();
    }

    public Vote (EventElection eventVS) {
        this.eventVS = eventVS;
        this.eventURL = eventVS.getUrl();
    }

    public Vote (X509Certificate x509Certificate, TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
        this.x509Certificate = x509Certificate;
    }

    public Vote (FieldEventVS optionSelected, EventElection eventVS, State state, CertificateVS certificateVS,
                    CMSMessage cmsMessage) {
        this.optionSelected = optionSelected;
        this.eventVS = eventVS;
        this.state = state;
        this.certificateVS = certificateVS;
        this.cmsMessage = cmsMessage;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public FieldEventVS getOptionSelected() {
        return optionSelected;
    }

    public void setOptionSelected(FieldEventVS optionSelected) {
        this.optionSelected = optionSelected;
    }

    public String getOriginHashCertVote() {
        return originHashCertVote;
    }

    public void setOriginHashCertVote(String originHashCertVote) {
        this.originHashCertVote = originHashCertVote;
    }

    public String getOriginHashAccessRequest() {
        return originHashAccessRequest;
    }

    public void setOriginHashAccessRequest(String originHashAccessRequest) {
        this.originHashAccessRequest = originHashAccessRequest;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setEventVS(EventElection eventVS) {
		this.eventVS = eventVS;
	}

	public EventElection getEventVS() {
		return eventVS;
	}

	public void setFieldEventVS(FieldEventVS optionSelected) {
		this.setOptionSelected(optionSelected);
	}

	public FieldEventVS getFieldEventVS() {
		return getOptionSelected();
	}

	public CMSMessage getCMSMessage() {
		return cmsMessage;
	}

	public void setCmsMessage(CMSMessage cmsMessage) {
		this.cmsMessage = cmsMessage;
	}

	public State getState() {
		return state;
	}

	public Vote setState(State state) {
		this.state = state;
        return this;
	}

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

	public CertificateVS getCertificateVS() {
		return certificateVS;
	}

	public void setCertificateVS(CertificateVS certificateVS) {
		this.certificateVS = certificateVS;
	}

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }

    public Set<X509Certificate> getServerCerts() {
        return serverCerts;
    }

    public boolean isValid() { return isValid; }

    public void setValid(boolean isValid) { this.isValid = isValid; }

    public CMSSignedMessage getReceipt() { return receipt; }

    public void setReceipt(CMSSignedMessage receipt) { this.receipt = receipt; }

    public void setServerCerts(Set<X509Certificate> serverCerts) { this.serverCerts = serverCerts; }

    public X509Certificate getX509Certificate() { return x509Certificate; }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    public String getRepresentativeURL() {
        return representativeURL;
    }

    public void setRepresentativeURL(String representativeURL) {
        this.representativeURL = representativeURL;
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

    public String getEventURL() {
        return eventURL;
    }

    public void setEventURL(String eventURL) {
        this.eventURL = eventURL;
    }

    public Long getAccessControlEventId() {
        return accessControlEventId;
    }

    public void setAccessControlEventId(Long accessControlEventId) {
        this.accessControlEventId = accessControlEventId;
    }

    public Vote loadSignatureData(X509Certificate x509Certificate, TimeStampToken timeStampToken) throws Exception {
        this.timeStampToken = timeStampToken;
        this.x509Certificate = x509Certificate;
        VoteCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(VoteCertExtensionDto.class, x509Certificate,
                ContextVS.VOTE_OID);
        this.accessControlEventId = certExtensionDto.getEventId();
        this.accessControlURL = certExtensionDto.getAccessControlURL();
        this.hashCertVSBase64 = certExtensionDto.getHashCertVS();
        this.representativeURL = CertUtils.getCertExtensionData(x509Certificate, ContextVS.REPRESENTATIVE_VOTE_OID);
        return this;
    }

}