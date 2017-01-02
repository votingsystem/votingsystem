package org.votingsystem.model.voting;

import org.votingsystem.model.EntityBase;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity @Table(name="ANON_VOTE_CERT_REQUEST")
public class AnonVoteCertRequest extends EntityBase implements Serializable {

    public enum State {OK, CANCELED, ERROR}

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false) private Long id;
    @Column(name="STATE", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ELECTION") private Election election;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="USER_ID") private User user;
    @OneToOne
    @JoinColumn(name="SIGNED_DOCUMENT_ID")
    private SignedDocument signedDocument;
    @Column(name="REVOCATION_HASH_BASE64") private String revocationHashBase64;
    @Column(name="META_INF") private String metaInf;
    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    public AnonVoteCertRequest() {}

    public AnonVoteCertRequest(User user, SignedDocument signedDocument, State state, String revocationHashBase64,
                               Election election) {
        this.user = user;
        this.signedDocument = signedDocument;
        this.state = state;
        this.revocationHashBase64 = revocationHashBase64;
        this.election = election;
    }

    public void setId(Long id) { this.id = id; }

    public Long getId() {return id;}

    public void setEventElection(Election eventVS) {  this.election = eventVS; }

    public Election getEventElection() { return election; }

    public AnonVoteCertRequest setState(State state) {
        this.state = state;
        return this;
    }

    public State getState() { return state; }

    public String getAccessRequestHashBase64() { return revocationHashBase64; }

    public void setAccessRequestHashBase64( String hashAccessRequestBase64) {
        this.revocationHashBase64 = hashAccessRequestBase64;
    }

    public SignedDocument getSignedDocument() { return signedDocument;  }

    public void setSignedDocument(SignedDocument signedDocument) { this.signedDocument = signedDocument; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public LocalDateTime getDateCreated() { return dateCreated; }

    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated;  }

    public LocalDateTime getLastUpdated() { return lastUpdated; }

    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getMetaInf() { return metaInf;  }

    public void setMetaInf(String metaInf) { this.metaInf = metaInf; }

}
