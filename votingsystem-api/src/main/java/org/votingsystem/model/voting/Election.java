package org.votingsystem.model.voting;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.dto.metadata.MetaInfDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.EntityBase;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.model.converter.MetaInfConverter;

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
@NamedQueries({
        @NamedQuery(name = Election.FIND_BY_UUID_AND_SYSTEM_ENTITY_ID, query =
                "SELECT e FROM Election e WHERE e.uuid =:electionUUID and e.entityId=:entityId")
})
@Entity @Table(name="ELECTION")
public class Election extends EntityBase implements Serializable {

    private static Logger log = Logger.getLogger(Election.class.getName());

    private static final long serialVersionUID = 1L;


    public static final String FIND_BY_UUID_AND_SYSTEM_ENTITY_ID = "FIND_ELECTION_BY_UUID_AND_SYSTEM_ENTITY_ID";

    /**
     * The order is important in order to sort fields
     *
     * https://stackoverflow.com/questions/33508465/custom-sort-order-for-an-enum-field
     */
    public enum State {
        @JsonProperty("ACTIVE")
        ACTIVE,
        @JsonProperty("PENDING")
        PENDING,
        @JsonProperty("TERMINATED")
        TERMINATED,
        @JsonProperty("CANCELED")
        CANCELED,
        @JsonProperty("DELETED_FROM_SYSTEM")
        DELETED_FROM_SYSTEM }


    @Id @GeneratedValue(strategy=IDENTITY) 
    @Column(name="ID", unique=true, nullable=false)
    private Long id;
    @Column(name="UUID", unique=true)
    private String uuid;
    @Column(name="CONTENT", columnDefinition="TEXT")
    private String content;
    @Column(name="META_INF", columnDefinition="TEXT")
    @Convert(converter = MetaInfConverter.class)
    private MetaInfDto metaInf;
    @Column(name="SUBJECT")
    private String subject;
    @Column(name="ENTITY_ID")
    private String entityId;
    @Column(name="ID_PROVIDER_ENTITY_ID")
    private String idProviderEntityId;
    @Enumerated(EnumType.ORDINAL)
    @Column(name="STATE")
    private State state;
    @OneToOne
    @JoinColumn(name="CERTIFICATE_ID")
    private Certificate certificate;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="PUBLISHER_ID")
    private User publisher;
    @OneToOne
    @JoinColumn(name="SIGNED_DOCUMENT_ID")
    private SignedDocument signedDocument;
    @Column(name="BACKUP_AVAILABLE")
    private Boolean backupAvailable;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="election")
    private Set<ElectionOption> electionOptions;

    @Column(name="DATE_BEGIN", nullable=false, columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateBegin;

    @Column(name="DATE_FINISH", nullable=false, columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateFinish;

    @Column(name="DATE_CANCELED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCanceled;

    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;


    public Election() {}

    public Election(ElectionDto electionDto, SignedDocument signedDocument) {
        this.subject = electionDto.getSubject();
        this.content = electionDto.getContent();
        this.dateBegin = electionDto.getDateBegin().toLocalDateTime();
        this.dateFinish = electionDto.getDateFinish().toLocalDateTime();
        this.state = electionDto.getState();
        this.uuid = electionDto.getUUID();
        electionOptions = new HashSet<>();
        for(ElectionOptionDto electionOptionDto : electionDto.getElectionOptions()) {
            electionOptions.add(new ElectionOption(this, electionOptionDto));
        }
        this.signedDocument = signedDocument;
        if(this.signedDocument != null && this.signedDocument.getSignatures() != null) {
            this.publisher = this.signedDocument.getSignatures().iterator().next().getSigner();
        }
        this.entityId = electionDto.getEntityId();
        this.idProviderEntityId = electionDto.getIdProviderEntityId();
    }

    public Election(String subject, String content, LocalDateTime dateBegin, LocalDateTime dateFinish, State state) {
        this.subject = subject;
        this.content = content;
        this.dateBegin = dateBegin;
        this.dateFinish = dateFinish;
        this.state = state;
    }

    public Set<ElectionOption> getElectionOptions() {
        return electionOptions;
    }

    public void setElectionOptions(Set<ElectionOption> electionOptions) {
        this.electionOptions = electionOptions;
    }

    public String getContent () {
        return content;
    }
    
    public void setContent (String content) {
        this.content = content;
    }

    public String getSubject() {
    	return subject;
    }
    
    public void setSubject (String subject) {
        this.subject = subject;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setPublisher(User publisher) {
        this.publisher = publisher;
    }

    public User getPublisher() {
        return publisher;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setDateBegin(LocalDateTime dateBegin) {
        this.dateBegin = dateBegin;
    }

    public LocalDateTime getDateBegin() {
        return dateBegin;
    }

    public void setDateFinish(LocalDateTime dateFinish) { this.dateFinish = dateFinish; }

    public State getState() {
        return state;
    }

    public Election setState(State state) {
        this.state = state;
        return this;
    }

	public Boolean getBackupAvailable() { return backupAvailable; }

	public void setBackupAvailable(Boolean backupAvailable) {
		this.backupAvailable = backupAvailable;
	}

	public LocalDateTime getDateCanceled() {
		return dateCanceled;
	}

	public void setDateCanceled(LocalDateTime dateCanceled) {
		this.dateCanceled = dateCanceled;
	}

	public MetaInfDto getMetaInf() {
		return metaInf;
	}

	public Election setMetaInf(MetaInfDto metaInf) {
		this.metaInf = metaInf;
        return this;
	}

    public LocalDateTime getDateFinish() {
		if(dateCanceled != null) return dateCanceled;
		else return dateFinish;
	}

    public String getEntityId() {
        return entityId;
    }

    public Election setEntityId(String systemEntityId) {
        this.entityId = systemEntityId;
        return this;
    }

    public boolean isActive(LocalDateTime selectedDate) {
        if(state == State.CANCELED || state == State.DELETED_FROM_SYSTEM)
            return false;
        if (selectedDate.isAfter(dateBegin) && selectedDate.isBefore(dateFinish))
            return true;
        return false;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public Election setCertificate(Certificate certificate) {
        this.certificate = certificate;
        return this;
    }

    public String getUUID() {
        return uuid;
    }

    public Election setUUID(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public Set<X509Certificate> getTrustedCerts() throws Exception {
        Set<X509Certificate> eventTrustedCerts = new HashSet<>();
        eventTrustedCerts.add(CertificateUtils.loadCertificate(certificate.getContent()));
        return eventTrustedCerts;
    }

    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public void setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
    }

    public String getIdProviderEntityId() {
        return idProviderEntityId;
    }

    public Election setIdProviderEntityId(String idProviderEntityId) {
        this.idProviderEntityId = idProviderEntityId;
        return this;
    }

}