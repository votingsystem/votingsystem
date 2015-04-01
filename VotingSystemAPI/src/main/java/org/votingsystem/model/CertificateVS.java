package org.votingsystem.model;

import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="CertificateVS")
@NamedQueries({
        @NamedQuery(name = "findCertBySerialNumberAndType", query =
                "SELECT c FROM CertificateVS c WHERE c.type =:type and c.serialNumber =:serialNumber"),
        @NamedQuery(name = "findCertByStateAndType", query =
                "SELECT c FROM CertificateVS c WHERE c.type =:type and c.state =:state"),
        @NamedQuery(name = "findCertBySerialNumber", query =
                "SELECT c FROM CertificateVS c WHERE c.serialNumber =:serialNumber"),
        @NamedQuery(name = "findCertByActorVSAndStateAndType", query =
                "SELECT c FROM CertificateVS c WHERE c.actorVS =:actorVS and c.state =:state and type =:type"),
        @NamedQuery(name = "findCertByUserAndStateAndSerialNumberAndCertificateCA", query =
                "SELECT c FROM CertificateVS c WHERE c.userVS =:userVS and c.state =:state " +
                        "and c.serialNumber =:serialNumber and c.authorityCertificateVS =:authorityCertificateVS"),
        @NamedQuery(name = "findCertByUserAndState", query =
        "SELECT c FROM CertificateVS c WHERE c.userVS =:userVS and c.state =:state")
})
public class CertificateVS extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(CertificateVS.class.getSimpleName());

    public enum State {OK, ERROR, CANCELED, USED, LAPSED, UNKNOWN}

    public enum Type {VOTEVS_ROOT, VOTEVS, USER, CERTIFICATE_AUTHORITY, ACTOR_VS,
        ANONYMOUS_REPRESENTATIVE_DELEGATION, CURRENCY, TIMESTAMP_SERVER}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @OneToOne private UserRequestCsrVS userRequestCsrVS;
    @OneToOne(mappedBy="certificateVS") private VoteVS voteVS;
    @Column(name="serialNumber", nullable=false) private Long serialNumber;
    @Column(name="content", nullable=false) private byte[] content;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="actorVS") private ActorVS actorVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVS") private EventVS eventVS;

    @Column(name="hashCertVSBase64", unique=true) private String hashCertVSBase64;

    @Column(name="messageSMIME" ) private MessageSMIME messageSMIME;

    @Column(name="metaInf", columnDefinition="TEXT")  private String metaInf;

    @Column(name="description", columnDefinition="TEXT")  private String description = "";

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="authorityCertificateVS") private CertificateVS authorityCertificateVS;

    @Column(name="certChainPEM") private byte[] certChainPEM;

    @Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type;

    @Column(name="isRoot") private Boolean isRoot;

    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23, insertable=true) private Date lastUpdated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validFrom", length=23, insertable=true) private Date validFrom;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23, insertable=true) private Date validTo;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="cancelDate", length=23, insertable=true) private Date cancelDate;

    @Transient private String eventId;

    @Transient private String representativeURL;

    @Transient private String serverURL;

    public CertificateVS() {}

    public static CertificateVS VOTE(VoteVS voteVS) throws CertificateEncodingException {
        CertificateVS result = new CertificateVS();
        result.setIsRoot(false);
        result.setState(State.OK).setType(Type.VOTEVS);
        result.setHashCertVSBase64(voteVS.getHashCertVSBase64());
        result.setUserVS(voteVS.getMessageSMIME().getUserVS());
        result.setEventVS(voteVS.getEventVS());
        result.setContent(voteVS.getX509Certificate().getEncoded());
        result.setValidFrom(voteVS.getX509Certificate().getNotBefore());
        result.setValidTo(voteVS.getX509Certificate().getNotAfter());
        result.setSerialNumber(voteVS.getX509Certificate().getSerialNumber().longValue());
        return result;
    }

    public CertificateVS(boolean isRoot, Type type, State state, String description, byte[] content, long serialNumber,
            Date validFrom, Date validTo) {
        this.isRoot = isRoot;
        this.type = type;
        this.state = state;
        this.description = description;
        this.content = content;
        this.serialNumber = serialNumber;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public CertificateVS(ActorVS actorVS, byte[] certChainPEM, byte[] content, State state, long serialNumber,
                        Type type, Date validFrom, Date validTo) {
        this.actorVS = actorVS;
        this.certChainPEM = certChainPEM;
        this.content = content;
        this.state = state;
        this.serialNumber = serialNumber;
        this.type = type;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public CertificateVS(ActorVS actorVS, EventVS eventVS, X509Certificate x509Certificate)
            throws CertificateEncodingException {
        this.actorVS = actorVS;
        this.eventVS = eventVS;
        this.state = CertificateVS.State.OK;
        this.type = CertificateVS.Type.ACTOR_VS;
        this.content = x509Certificate.getEncoded();
        this.serialNumber = x509Certificate.getSerialNumber().longValue();
        this.validFrom = x509Certificate.getNotBefore();
        this.validTo = x509Certificate.getNotAfter();

    }

    public CertificateVS(UserVS userVS, X509Certificate x509Cert, State state, Type type,
             CertificateVS authorityCertificateVS, Date validFrom, Date validTo) throws CertificateEncodingException {
        this.userVS = userVS;
        this.content = x509Cert.getEncoded();
        this.serialNumber = x509Cert.getSerialNumber().longValue();
        this.state = state;
        this.type = type;
        this.authorityCertificateVS = authorityCertificateVS;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
    public CertificateVS(X509Certificate x509Cert, State state, Type type, String hashCertVSBase64,
            Date validFrom, Date validTo) throws CertificateEncodingException {
        this(null, x509Cert, state, type, null, validFrom, validTo);
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public CertificateVS(X509Certificate x509Cert, EventVS eventVS, Type type, State state, String hashCertVSBase64)
            throws CertificateEncodingException {
        this.serialNumber = x509Cert.getSerialNumber().longValue();
        this.content = x509Cert.getEncoded();
        this.eventVS = eventVS;
        this.type = type;
        this.state = state;
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public CertificateVS(X509Certificate x509Cert, EventVS eventVS, Type type, State state)
            throws CertificateEncodingException {
        this.serialNumber = x509Cert.getSerialNumber().longValue();
        this.content = x509Cert.getEncoded();
        this.eventVS = eventVS;
        this.type = type;
        this.state = state;
        this.validTo = x509Cert.getNotAfter();
        this.validFrom = x509Cert.getNotBefore();
    }

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public CertificateVS setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
        return this;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
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

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public State getState() {
        return state;
    }

    public CertificateVS setState(State state) {
        this.state = state;
        return this;
    }

    public EventVS getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public ActorVS getActorVS() {
        return actorVS;
    }

    public void setActorVS(ActorVS actorVS) {
        this.actorVS = actorVS;
    }

    public String getEventVSId() {
        return eventId;
    }

    public void setEventVSId(String eventId) {
        this.eventId = eventId;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public VoteVS getVoteVS() {
        return voteVS;
    }

    public void setVoteVS(VoteVS voteVS) {
        this.voteVS = voteVS;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public CertificateVS getAuthorityCertificateVS() { return authorityCertificateVS; }

    public void setAuthorityCertificateVS(CertificateVS authorityCertificate) {
        this.authorityCertificateVS = authorityCertificate;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() { return validTo; }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public UserRequestCsrVS getUserRequestCsrVS() {
        return userRequestCsrVS;
    }

    public void setUserRequestCsrVS(UserRequestCsrVS userRequestCsrVS) {
        this.userRequestCsrVS = userRequestCsrVS;
    }

    public Date getCancelDate() {
        return cancelDate;
    }

    public void setCancelDate(Date cancelDate) {
        this.cancelDate = cancelDate;
    }

    public String getRepresentativeURL() {
        return representativeURL;
    }

    public void setRepresentativeURL(String representativeURL) {
        this.representativeURL = representativeURL;
    }

    public byte[] getCertChainPEM() {
        return certChainPEM;
    }

    public void setCertChainPEM(byte[] certChain) {
        this.certChainPEM = certChain;
    }


    public Boolean getIsRoot() {
        return isRoot;
    }

    public void setIsRoot(Boolean isRoot) {
        this.isRoot = isRoot;
    }

    public String getMetaInf() {
        return metaInf;
    }

    public void setMetaInf(String metaInf) {
        this.metaInf = metaInf;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void updateDescription(String description) {
        if(this.description == null) this.description = description;
        else this.description = this.description + " ----- " + DateUtils.getDateStr(new Date()) + ": " + description;
    }

    public X509Certificate getX509Cert() throws Exception {
        X509Certificate x509Cert = CertUtils.loadCertificate(content);
        return x509Cert;
    }

}
