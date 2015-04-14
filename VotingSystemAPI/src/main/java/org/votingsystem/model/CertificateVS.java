package org.votingsystem.model;

import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
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
    @Column(name="serialNumber", nullable=false) private Long serialNumber;
    @Column(name="content", nullable=false) private byte[] content;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="actorVS") private ActorVS actorVS;

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

    public CertificateVS(X509Certificate x509Cert) throws CertificateEncodingException {
        this.validFrom = x509Cert.getNotBefore();
        this.validTo = x509Cert.getNotAfter();
        this.content = x509Cert.getEncoded();
        this.serialNumber = x509Cert.getSerialNumber().longValue();
    }

    public static CertificateVS VOTE(String hashCertVSBase64, UserVS userVS, X509Certificate x509Cert)
            throws CertificateEncodingException {
        CertificateVS result = new CertificateVS(x509Cert);
        result.setIsRoot(false);
        result.setState(State.OK).setType(Type.VOTEVS);
        result.setHashCertVSBase64(hashCertVSBase64);
        result.setUserVS(userVS);
        return result;
    }

    public static CertificateVS ELECTION(X509Certificate x509Cert) throws CertificateEncodingException {
        CertificateVS result = new CertificateVS(x509Cert);
        result.type = CertificateVS.Type.VOTEVS_ROOT;
        result.state = CertificateVS.State.OK;
        return result;
    }

    public static CertificateVS ACTORVS(ActorVS actorVS, X509Certificate x509Cert)
            throws CertificateEncodingException {
        CertificateVS result = new CertificateVS(x509Cert);
        result.type = CertificateVS.Type.ACTOR_VS;
        result.state = CertificateVS.State.OK;
        result.actorVS = actorVS;
        return result;
    }

    public static CertificateVS ANONYMOUS_REPRESENTATIVE_DELEGATION(String hashCertVSBase64, X509Certificate x509Cert)
            throws CertificateEncodingException {
        CertificateVS result = new CertificateVS(x509Cert);
        result.type = CertificateVS.Type.ANONYMOUS_REPRESENTATIVE_DELEGATION;
        result.state = CertificateVS.State.OK;
        result.hashCertVSBase64 = hashCertVSBase64;
        return result;
    }

    public static CertificateVS AUTHORITY(X509Certificate x509Cert, String description) throws CertificateException,
            NoSuchAlgorithmException,  NoSuchProviderException {
        CertificateVS result = new CertificateVS(x509Cert);
        result.isRoot = CertUtils.isSelfSigned(x509Cert);
        result.type = CertificateVS.Type.CERTIFICATE_AUTHORITY;
        result.state = CertificateVS.State.OK;
        result.description = description;
        return result;
    }

    public static CertificateVS USER(UserVS userVS, X509Certificate x509Cert)
            throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        CertificateVS result = new CertificateVS(x509Cert);
        result.type = CertificateVS.Type.USER;
        result.state = CertificateVS.State.OK;
        result.authorityCertificateVS = userVS.getCertificateCA();
        return result;
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
