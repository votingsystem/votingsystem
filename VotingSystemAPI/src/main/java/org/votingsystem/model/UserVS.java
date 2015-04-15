package org.votingsystem.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="UserVS")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="userVSType", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("UserVS")
@NamedQueries({
        @NamedQuery(name = "findUserByType", query = "SELECT u FROM UserVS u WHERE u.type =:type"),
        @NamedQuery(name = "findUserByNIF", query = "SELECT u FROM UserVS u WHERE u.nif =:nif"),
        @NamedQuery(name = "findUserByIBAN", query = "SELECT u FROM UserVS u WHERE u.IBAN =:IBAN"),
        @NamedQuery(name = "countUserActiveOrCancelledAfter", query = "SELECT COUNT(u) FROM UserVS u " +
            "WHERE u.dateCancelled is not null or u.dateCancelled >=:dateCancelled"),
        @NamedQuery(name = "findUserActiveOrCancelledAfterAndInList", query = "SELECT u FROM UserVS u " +
                "WHERE (u.dateCancelled is not null or u.dateCancelled >=:dateCancelled) and u.type in :inList"),
        @NamedQuery(name = "findUserByRepresentativeAndIBAN", query = "SELECT u FROM UserVS u " +
                "WHERE u.representative =:representative and u.IBAN =:IBAN"),
        @NamedQuery(name = "countUserActiveByDateAndInList", query = "SELECT COUNT(u) FROM UserVS u " +
                "WHERE (u.dateCancelled is null OR u.dateCancelled >=:date) and u.type in :inList")
})
public class UserVS extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(UserVS.class.getSimpleName());

    public enum Type {USER, GROUP, SYSTEM, REPRESENTATIVE, BANKVS}

    public enum State {ACTIVE, PENDING, SUSPENDED, CANCELED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
	@Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type = Type.USER;
    @Column(name="nif", unique=true) private String nif;

    @Column(name="IBAN") private String IBAN;

    @Column(name="name") private String name;

    @Column(name="url") private String url;
    
    @Column(name="metaInf", columnDefinition="TEXT") private String metaInf = "{\"numRepresentations\":1}";
    
    @Column(name="firstName" ) private String firstName;

    @Column(name="lastName" ) private String lastName;

    @Column(name="description", columnDefinition="TEXT" ) private String description;
    
    @Column(name="country" ) private String country;
    
    @Column(name="phone" ) private String phone;
    
    @Column(name="email" ) private String email;

    @Column(name="cn") private String cn;

    @Column(name="reason") private String reason;

    @Column(name="state") @Enumerated(EnumType.STRING) private State state = State.ACTIVE;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="representative") private UserVS representative;

    //Owning Entity side of the relationship
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JoinTable(name = "uservs_tagvs", joinColumns = {
            @JoinColumn(name = "uservs", referencedColumnName = "id", nullable = false) },
            inverseJoinColumns = { @JoinColumn(name = "tagvs", nullable = false, referencedColumnName = "id") })
    private Set<TagVS> tagVSSet;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCancelled", length=23) private Date dateCancelled;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateActivated", length=23) private Date dateActivated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;
    
    @Transient private transient X509Certificate certificate;
    @Transient private transient CertificateVS certificateVS;
    @Transient private transient CertificateVS certificateCA;
    @Transient private transient Map metaInfMap;
    @Transient private transient TimeStampToken timeStampToken;
    @Transient private transient SignerInformation signerInformation;
    @Transient private transient DeviceVS deviceVS;
    @Transient private KeyStore keyStore;

    public UserVS() {}

    public UserVS(String nif, String name, Type type) {
        this.nif = nif;
        this.name = name;
        this.type = type;
    }

    public String getIBAN() {
        return IBAN;
    }

    public UserVS setIBAN(String IBAN) {
        this.IBAN = IBAN;
        return this;
    }

    public UserVS(String nif) { this.nif = nif; }
    public UserVS(String nif, Type type, String name) {
        this.nif = nif;
        this.type = type;
        this.name = name;
    }

    public UserVS(String nif, Type type, String name, String firstName, String lastName, String email, String phone) {
        this.nif = nif;
        this.type = type;
        this.name = name;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    public static UserVS REPRESENTATIVE(String representativeURL) {
        UserVS result = new UserVS();
        result.setType(UserVS.Type.REPRESENTATIVE).setUrl(representativeURL);
        return result;
    }

    public SignerInformation getSignerInformation() {
        return signerInformation;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
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

    public String getName() {
        if(name == null) name = firstName + " " + lastName;
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setFirstName(String firstName) {this.firstName = firstName;}

    public String getFirstName() {
        return firstName;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getCn() {
        return cn;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CertificateVS getCertificateCA() {
		return certificateCA;
	}

	public void setCertificateCA(CertificateVS certificate) {
		this.certificateCA = certificate;
	}

	public UserVS getRepresentative() {
		return representative;
	}

	public UserVS setRepresentative(UserVS representative) {
		this.representative = representative;
        return this;
	}

	public Type getType() {
		return type;
	}

	public UserVS setType(Type type) {
		this.type = type;
        return this;
	}

    public String getReason() {
        return reason;
    }

    public UserVS setReason(String reason) {
        this.reason = reason;
        return this;
    }

	public TimeStampToken getTimeStampToken() {
		return timeStampToken;
	}

	public void setTimeStampToken(TimeStampToken timeStampToken) {
		this.timeStampToken = timeStampToken;
	}

	public String getMetaInf() {
		return metaInf;
	}

	public void setMetaInf(String metaInf) {
		this.metaInf = metaInf;
	}

    public void updateAdmins(Set<String> admins) throws IOException {
        getMetaInfMap().put("adminsDNI", admins);
        this.metaInf = new ObjectMapper().writeValueAsString(metaInfMap);
    }

    public void setSignerInformation(SignerInformation signer) {
        this.signerInformation = signer;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getDateCancelled() {
        return dateCancelled;
    }

    public void setDateCancelled(Date dateCancelled) {
        this.dateCancelled = dateCancelled;
    }

    public String getDescription() {
        return description;
    }

    public UserVS setDescription(String description) {
        this.description = description;
        return this;
    }

    public Date getDateActivated() {
        return dateActivated;
    }

    public void setDateActivated(Date dateActivated) {
        this.dateActivated = dateActivated;
    }

    public State getState() {
        return state;
    }

    public UserVS setState(State state) {
        this.state = state;
        return this;
    }

    public CertificateVS getCertificateVS() {
        return certificateVS;
    }

    public void setCertificateVS(CertificateVS certificateVS) {
        this.certificateVS = certificateVS;
    }

    public Set<TagVS> getTagVSSet() {
        return tagVSSet;
    }

    public void setTagVSSet(Set<TagVS> tagVSSet) {
        this.tagVSSet = tagVSSet;
    }

    public String getSignatureBase64() {
        if (signerInformation.getSignature() == null) return null;
        return DatatypeConverter.printBase64Binary(signerInformation.getSignature());
    }

    public String getSignatureHex() {
        if (signerInformation.getSignature() == null) return null;
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        return hexConverter.marshal(getSignatureBase64().getBytes());
    }

    public String getEncryptiontId() {
        if(signerInformation == null) return null;
        else return CMSUtils.getEncryptiontId(signerInformation.getEncryptionAlgOID()); 
    }

    public Date getSignatureDate() {
        if(timeStampToken == null) return null;
        return timeStampToken.getTimeStampInfo().getGenTime();
    }

    public String getDigestId() {
        if(signerInformation == null) return null;
        else return CMSUtils.getDigestId(signerInformation.getDigestAlgOID()); }

    public String getContentDigestHex() {
        if (signerInformation.getContentDigest() == null) return null;
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        return hexConverter.marshal(signerInformation.getContentDigest());
    }

    public void setSigner(SignerInformation signer) {
        this.signerInformation = signer;
    }

    public String getContentDigestBase64() {
        if (signerInformation.getContentDigest() == null) return null;
        return DatatypeConverter.printBase64Binary(signerInformation.getContentDigest());
    }

    public static String getServerInfoURL(String serverURL, Long userId) {
        return serverURL + "/rest/userVS/id/" + userId;
    }

    public Map getMetaInfMap() throws IOException {
        if(metaInfMap == null) {
            if(metaInf == null) {
                metaInfMap = new HashMap<>();
                metaInf = new ObjectMapper().writeValueAsString(metaInfMap);
            } else metaInfMap = new ObjectMapper().readValue(metaInf, HashMap.class);
        }
        return metaInfMap;
    }

    public UserVS updateCertInfo (X509Certificate certificate) {
        UserVS userVS = getUserVS(certificate.getSubjectDN().getName());
        setFirstName(userVS.getFirstName());
        setName(userVS.getFirstName());
        setLastName(userVS.getLastName());
        setNif(userVS.getNif());
        setCountry(userVS.getCountry());
        setCn(userVS.getCn());
        return this;
    }

    public static UserVS getUserVS (X509Certificate certificate) {
        UserVS userVS = getUserVS(certificate.getSubjectDN().getName());
        userVS.setCertificate(certificate);
        return userVS;
    }

    public DeviceVS getDeviceVS() {
        return deviceVS;
    }

    public void setDeviceVS(DeviceVS deviceVS) {
        this.deviceVS = deviceVS;
    }

    public String getFullName() {
        if(firstName == null) return name;
        else return name + " " + firstName;
    }

    public static UserVS getUserVS (String subjectDN) {
        UserVS userVS = new UserVS();
        if (subjectDN.contains("C=")) userVS.setCountry(subjectDN.split("C=")[1].split(",")[0]);
        if (subjectDN.contains("SERIALNUMBER=")) userVS.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
        if (subjectDN.contains("SURNAME=")) userVS.setLastName(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.contains("GIVENNAME=")) {
            String givenname = subjectDN.split("GIVENNAME=")[1].split(",")[0];
            userVS.setName(givenname);
            userVS.setFirstName(givenname);
        }
        if (subjectDN.contains("CN=")) userVS.setCn(subjectDN.split("CN=")[1]);
        return userVS;
    }

    @PrePersist
    public void prePersist() {
        Date date = new Date();
        setDateCreated(date);
        setLastUpdated(date);
        if(nif != null) {
            this.nif = nif.toUpperCase();
        }
        if(name == null) {
            this.name = (firstName == null? "":firstName + " " + lastName == null? "":lastName).trim();
        }
    }

    @PreUpdate
    public void preUpdate() {
        setLastUpdated(new Date());
    }

    public static UserVS parse (Map userVSDataMap) throws Exception {
        UserVS userVS = null;
        switch (Type.valueOf((String) userVSDataMap.get("type"))) {
            case BANKVS:
                userVS = new BankVS();
                break;
            case GROUP:
                userVS = new GroupVS();
                if(userVSDataMap.containsKey("representative")) ((GroupVS)userVS).setRepresentative(
                        UserVS.parse((Map) userVSDataMap.get("representative")));
                break;
            default:
                userVS = new UserVS();
        }
        if(userVSDataMap.containsKey("id")) userVS.setId(((Integer) userVSDataMap.get("id")).longValue());
        if(userVSDataMap.containsKey("nif")) userVS.setNif((String) userVSDataMap.get("nif"));
        if(userVSDataMap.containsKey("IBAN")) userVS.setIBAN((String) userVSDataMap.get("IBAN"));
        if(userVSDataMap.containsKey("name")) userVS.setName((String) userVSDataMap.get("name"));
        if(userVSDataMap.containsKey("email")) userVS.setEmail((String) userVSDataMap.get("email"));
        if(userVSDataMap.containsKey("phone")) userVS.setPhone((String) userVSDataMap.get("phone"));
        if(userVSDataMap.containsKey("firstName")) userVS.setFirstName((String) userVSDataMap.get("firstName"));
        if(userVSDataMap.containsKey("lastName")) userVS.setFirstName((String) userVSDataMap.get("lastName"));
        if(userVSDataMap.containsKey("metaInf")) userVS.setMetaInf((String) userVSDataMap.get("metaInf"));
        if(userVSDataMap.containsKey("country")) userVS.setCountry((String) userVSDataMap.get("country"));
        if(userVSDataMap.containsKey("state")) userVS.setState(State.valueOf((String)userVSDataMap.get("state")));
        if(userVSDataMap.containsKey("type")) {
            Type type = Type.valueOf((String) userVSDataMap.get("type"));
            userVS.setType(type);
        }
        if(userVSDataMap.containsKey("certificateList")) {
            List<Map> arrayCerts = (List) userVSDataMap.get("certificateList");
            for(Map certData : arrayCerts) {
                Collection<X509Certificate> certCollection = CertUtils.fromPEMToX509CertCollection(
                        ((String)certData.get("pemCert")).getBytes());
                userVS.setCertificate(certCollection.iterator().next());
            }
        }
        return userVS;
    }

    public Map toMap() throws Exception {
        Map dataMap = new HashMap<>();
        dataMap.put("id", id);
        dataMap.put("URL", url);
        dataMap.put("nif", nif);
        dataMap.put("IBAN", IBAN);
        if(state != null) dataMap.put("state", state.toString());
        if(type != null) dataMap.put("type", type.toString());
        if(reason != null) dataMap.put("reason", reason);
        if(certificate != null) {
            List dataList = new ArrayList<>();
            Map certDataMap = new HashMap<>();
            certDataMap.put("serialNumber", certificate.getSerialNumber());
            certDataMap.put("pemCert", new String(CertUtils.getPEMEncoded(certificate), "UTF-8"));
            dataList.add(certDataMap);
            dataMap.put("certificateList", dataList);
        }
        Map deviceData = new HashMap<>();
        deviceData.put("phone", getPhone());
        deviceData.put("email", getEmail());
        dataMap.put("deviceData", deviceData);
        dataMap.put("name", name);
        dataMap.put("firstName", firstName);
        dataMap.put("lastName", lastName);
        dataMap.put("description", description);
        return dataMap;
    }

}