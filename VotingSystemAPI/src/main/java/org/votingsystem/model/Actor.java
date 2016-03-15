package org.votingsystem.model;

import org.votingsystem.model.voting.ControlCenter;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.crypto.PEMUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="Actor")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn( name="serverType", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("Actor")
@NamedQueries({
        @NamedQuery(name = "findActorByServerURL", query =
                "SELECT a FROM Actor a WHERE a.serverURL =:serverURL")
})
public class Actor extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(Actor.class.getName());

    public static final long serialVersionUID = 1L;

    public enum Type {CONTROL_CENTER, ACCESS_CONTROL, CURRENCY, TIMESTAMP_SERVER, SERVER, USER;}

    public enum State { SUSPENDED, OK, PAUSED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @Column(name="serverURL") private String serverURL;

    @Column(name="name") private String name;

    @Enumerated(EnumType.STRING)
    @Column(name="state") private State state;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23, insertable=true) public Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23, insertable=true) public Date lastUpdated;

    @Transient private Type serverType;
    @Transient private String certChainPEM;
    @Transient private String webSocketURL;
    @Transient private String timeStampServerURL;
    @Transient private Collection<X509Certificate> certChain;
    @Transient private transient X509Certificate x509Certificate;
    @Transient private Set<TrustAnchor> trustAnchors = null;
    @Transient private Certificate certificate;
    @Transient private Date date;
    @Transient private ControlCenter controlCenter;
    @Transient private X509Certificate timeStampCert = null;

    public Actor() { }

    public Actor(String serverURL) {
        this.serverURL = serverURL;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public State getState() {
        return state;
    }

    public Actor setState(State state) {
        this.state = state;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = StringUtils.checkURL(serverURL);
    }

    public String getTimeStampServiceURL() {
        return getTimeStampServerURL() + "/timestamp";
    }

    public static String getTimeStampServiceURL(String serverURL) {
        return StringUtils.checkURL(serverURL) + "/timestamp";
    }

    public String getTimeStampDiscreteServiceURL() {
        return getTimeStampServerURL() + "/timestamp/discrete";
    }

    public static String getTimeStampDiscreteServiceURL(String serverURL) {
        return StringUtils.checkURL(serverURL) + "/timestamp/discrete";
    }

    public String getTimeStampServerURL() {
        return timeStampServerURL;
    }

    public void setTimeStampServerURL(String timeStampServerURL) {
        this.timeStampServerURL = timeStampServerURL;
    }

    public String getServerURL() {
        return serverURL;
    }

    public X509Certificate getX509Certificate() throws Exception {
        if(x509Certificate != null) return x509Certificate;
        if(certChainPEM == null) return null;
        certChain = PEMUtils.fromPEMToX509CertCollection(certChainPEM.getBytes());
        x509Certificate = certChain.iterator().next();
        return x509Certificate;
    }

    public Actor setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
        return this;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public Type getType() { return serverType; }

    public void setType(Type serverType) { this.serverType = serverType; }

    public X509Certificate getTimeStampCert() {
        return timeStampCert;
    }

    public void setTimeStampCert(X509Certificate timeStampCert) {
        this.timeStampCert = timeStampCert;
    }

    public Set<TrustAnchor> getTrustAnchors() throws Exception {
        if(trustAnchors != null) return trustAnchors;
        if(certChainPEM == null) return null;
        certChain = PEMUtils.fromPEMToX509CertCollection(certChainPEM.getBytes());
        trustAnchors = new HashSet<>();
        for (X509Certificate cert:certChain) {
            trustAnchors.add(new TrustAnchor(cert, null));
        }
        return trustAnchors;
    }

    public void setTrustAnchors(Set<TrustAnchor> trustAnchors) {
        this.trustAnchors = trustAnchors;
    }

    public String getCertChainPEM() {
        return certChainPEM;
    }

    public void setTimeStampCertPEM(String timeStampPEM) throws Exception {
        if(timeStampPEM == null) {
            this.timeStampCert = null;
            return;
        }
        timeStampCert = PEMUtils.fromPEMToX509CertCollection(timeStampPEM.getBytes()).iterator().next();
    }

    public String getWebSocketURL() {
        return webSocketURL;
    }

    public void setWebSocketURL(String webSocketURL) {
        this.webSocketURL = webSocketURL;
    }

    public void setCertChainPEM(String certChainPEM) {
        this.certChainPEM = certChainPEM;
    }

    public Collection<X509Certificate> getCertChain() {
        return certChain;
    }

    public ControlCenter getControlCenter() {
        return controlCenter;
    }

    public void setControlCenter(ControlCenter controlCenter) {
        this.controlCenter = controlCenter;
    }

    public String getRootCAServiceURL() {
        return serverURL + "/rest/certificate/addCertificateAuthority";
    }

    public static String getRootCAServiceURL(String serverURL) {
        return serverURL + "/rest/certificate/addCertificateAuthority";
    }

    public String getServerInfoURL() {
        return serverURL + "/rest/serverInfo";
    }

    public String getMultiSignedMessageTestServiceURL() {
        return serverURL + "/rest/encryptor/getMultiSignedMessage";
    }

    public String getEncryptionServiceURL() {
        return getServerURL() + "/rest/encryptor";
    }

    public static String getServerInfoURL(String serverURL) {
        return serverURL + "/rest/serverInfo";
    }

    public String getUserCertServiceURL() {
        return getServerURL() + "/rest/development/adduser";
    }

    public String getUserBaseInitServiceURL() {
        return getServerURL() + "/rest/user/prepareUserBaseData";
    }

    public String getReceiptViewerURL() {
        return getServerURL() + "/rest/cmsMessage/contentViewer";
    }

    public String getVoteCancelerURL() {
        return getServerURL() + "/rest/vote/cancel";
    }

    public static String getReceiptViewerURL(String serverURL) {
        return serverURL + "/rest/cmsMessage/contentViewer";
    }

    public String getGroupURL(Long id) {
        return getServerURL() + "/rest/group/id/" + String.valueOf(id);
    }

    public String getUserURL(Long id) {
        return getServerURL() + "/rest/user/id/" + String.valueOf(id);
    }

    public String getUserBalanceURL(String nif, String menu, String locale) {
        String sufix = (menu != null || locale != null)? "?menu=" + menu + "&locale=" + locale : "";
        return getServerURL() + "/spa.xhtml" + sufix + "#!/rest/balance/user/nif/" + nif;
    }

    public String getDeviceListByNifServiceURL(String nif) {
        return getServerURL() + "/rest/device/nif/" + nif + "/list";
    }

    public String getDashBoardURL(String menu, String locale) {
        String sufix = (menu != null || locale != null)? "?menu=" + menu + "&locale=" + locale : "";
        return getServerURL() + "/app/admin.xhtml" + sufix;
    }
}