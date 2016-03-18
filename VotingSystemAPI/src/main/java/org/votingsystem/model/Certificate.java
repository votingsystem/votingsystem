package org.votingsystem.model;

import org.votingsystem.util.DateUtils;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.crypto.CertUtils;

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
@Table(name="Certificate")
@NamedQueries({
        @NamedQuery(name = "findCertBySerialNumber", query =
                "SELECT c FROM Certificate c WHERE c.serialNumber =:serialNumber"),
        @NamedQuery(name = "findCertByActorAndStateAndType", query =
                "SELECT c FROM Certificate c WHERE c.actor =:actor and c.state =:state and c.type =:type")
})
public class Certificate extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(Certificate.class.getName());

    public enum State {OK, ERROR, CANCELED, USED, LAPSED, UNKNOWN}

    public enum Type {
        VOTE_ROOT, VOTE, USER, USER_ID_CARD, BROWSER_SESSION, CERTIFICATE_AUTHORITY, CERTIFICATE_AUTHORITY_ID_CARD,
        ACTOR, ANONYMOUS_REPRESENTATIVE_DELEGATION, CURRENCY, TIMESTAMP_SERVER}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="serialNumber", nullable=false) private Long serialNumber;
    @Column(name="content", nullable=false) private byte[] content;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userId") private User user;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="actor") private Actor actor;

    @Column(name="hashCertVSBase64", unique=true) private String hashCertVSBase64;

    @Column(name="cmsMessage" ) private CMSMessage cmsMessage;

    @Column(name="metaInf", columnDefinition="TEXT")  private String metaInf;

    @Column(name="description", columnDefinition="TEXT")  private String description = "";

    @Column(name="subjectDN")  private String subjectDN;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="authorityCertificate") private Certificate authorityCertificate;

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

    public Certificate() {}

    public Certificate(X509Certificate x509Cert) throws CertificateEncodingException {
        this.validFrom = x509Cert.getNotBefore();
        this.validTo = x509Cert.getNotAfter();
        this.content = x509Cert.getEncoded();
        this.serialNumber = x509Cert.getSerialNumber().longValue();
    }

    public static Certificate VOTE(String hashCertVSBase64, User user, X509Certificate x509Cert)
            throws CertificateEncodingException {
        Certificate result = new Certificate(x509Cert);
        result.setIsRoot(false);
        result.setState(State.OK).setType(Type.VOTE);
        result.setHashCertVSBase64(hashCertVSBase64);
        result.setUser(user);
        result.subjectDN = x509Cert.getSubjectDN().toString();
        return result;
    }

    public static Certificate ELECTION(X509Certificate x509Cert) throws CertificateEncodingException {
        Certificate result = new Certificate(x509Cert);
        result.type = Certificate.Type.VOTE_ROOT;
        result.state = Certificate.State.OK;
        result.subjectDN = x509Cert.getSubjectDN().toString();
        return result;
    }

    public static Certificate ACTOR(Actor actor, X509Certificate x509Cert)
            throws CertificateEncodingException {
        Certificate result = new Certificate(x509Cert);
        result.type = Certificate.Type.ACTOR;
        result.state = Certificate.State.OK;
        result.subjectDN = x509Cert.getSubjectDN().toString();
        result.actor = actor;
        return result;
    }

    public static Certificate ANONYMOUS_REPRESENTATIVE_DELEGATION(String hashCertVSBase64, X509Certificate x509Cert)
            throws CertificateEncodingException {
        Certificate result = new Certificate(x509Cert);
        result.type = Certificate.Type.ANONYMOUS_REPRESENTATIVE_DELEGATION;
        result.state = Certificate.State.OK;
        result.subjectDN = x509Cert.getSubjectDN().toString();
        result.hashCertVSBase64 = hashCertVSBase64;
        return result;
    }

    public static Certificate AUTHORITY(X509Certificate x509Cert, String description) throws CertificateException,
            NoSuchAlgorithmException,  NoSuchProviderException {
        Certificate result = new Certificate(x509Cert);
        result.isRoot = CertUtils.isSelfSigned(x509Cert);
        result.subjectDN = x509Cert.getSubjectDN().toString();
        //TODO
        if(result.subjectDN.contains("C=ES,O=DIRECCION GENERAL DE LA POLICIA,OU=DNIE,CN=AC"))
            result.type = Type.CERTIFICATE_AUTHORITY_ID_CARD;
        else result.type = Certificate.Type.CERTIFICATE_AUTHORITY;
        result.state = Certificate.State.OK;
        result.description = description;
        return result;
    }

    public static Certificate USER(User user, X509Certificate x509Cert)
            throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        Certificate result = new Certificate(x509Cert);
        if(user.getCertificateCA() != null &&
                Type.CERTIFICATE_AUTHORITY_ID_CARD == user.getCertificateCA().getType()) result.type = Type.USER_ID_CARD;
        else result.type = Type.USER;
        result.state = Certificate.State.OK;
        result.user = user;
        result.subjectDN = x509Cert.getSubjectDN().toString();
        result.authorityCertificate = user.getCertificateCA();
        return result;
    }

    public static Certificate ISSUED_USER_CERT(User user, X509Certificate x509Cert, Certificate authorityCertificate)
            throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        Certificate result = new Certificate(x509Cert);
        if(Type.CERTIFICATE_AUTHORITY_ID_CARD == authorityCertificate.getType()) result.type = Type.USER_ID_CARD;
        else result.type = Type.USER;
        result.state = Certificate.State.OK;
        result.user = user;
        result.subjectDN = x509Cert.getSubjectDN().toString();
        result.authorityCertificate = authorityCertificate;
        return result;
    }

    public static Certificate ISSUED_BROWSER_CERT(User user, X509Certificate x509Cert, Certificate authorityCertificate)
            throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        Certificate result = new Certificate(x509Cert);
        result.type = Type.BROWSER_SESSION;
        result.state = Certificate.State.OK;
        result.user = user;
        result.subjectDN = x509Cert.getSubjectDN().toString();
        result.authorityCertificate = authorityCertificate;
        return result;
    }

    public CMSMessage getCmsMessage() {
        return cmsMessage;
    }

    public Certificate setCmsMessage(CMSMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
        return this;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Certificate setState(State state) {
        this.state = state;
        return this;
    }

    public Actor getActor() {
        return actor;
    }

    public void setActor(Actor actor) {
        this.actor = actor;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
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

    public Certificate getAuthorityCertificate() { return authorityCertificate; }

    public void setAuthorityCertificate(Certificate authorityCertificate) {
        this.authorityCertificate = authorityCertificate;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
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

    public X509Certificate getX509Certificate() throws Exception {
        return CertUtils.loadCertificate(content);
    }

}
