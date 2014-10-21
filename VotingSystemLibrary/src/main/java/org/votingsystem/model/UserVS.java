package org.votingsystem.model;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertUtils;
import javax.persistence.*;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="UserVS")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="userVSType", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("UserVS")
public class UserVS implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(UserVS.class);

    public enum Type {USER, GROUP, SYSTEM, REPRESENTATIVE, BANKVS}

    public enum State {ACTIVE, PENDING, SUSPENDED, CANCELLED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
	@Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type = Type.USER;
    @Column(name="nif", unique=true) private String nif;

    @Column(name="IBAN") private String IBAN;

    @Column(name="name") private String name;

    @Column(name="url") private String url;
    
    @Column(name="metaInf", columnDefinition="TEXT") private String metaInf = "{\"numRepresentations\"=1}";
    
    @Column(name="firstName" ) private String firstName;

    @Column(name="lastName" ) private String lastName;

    @Column(name="description", columnDefinition="TEXT" ) private String description;
    
    @Column(name="representativeMessage" ) private MessageSMIME representativeMessage;
    
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
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="representativeRegisterDate", length=23) private Date representativeRegisterDate;
    
    @Transient private transient X509Certificate certificate;
    @Transient private transient CertificateVS certificateVS;
    @Transient private transient CertificateVS certificateCA;
    @Transient private transient JSONObject metaInfJSON;
    @Transient private transient TimeStampToken timeStampToken;
    @Transient private transient SignerInformation signerInformation;
    @Transient private KeyStore keyStore;

    public UserVS() {}

    public String getIBAN() {
        return IBAN;
    }

    public UserVS setIBAN(String IBAN) {
        this.IBAN = IBAN;
        return this;
    }

    public UserVS(String nif) { this.nif = nif; }

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

	public void setRepresentative(UserVS representative) {
		this.representative = representative;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

	public MessageSMIME getRepresentativeMessage() {
		return representativeMessage;
	}

	public void setRepresentativeMessage(MessageSMIME representativeMessage) {
		this.representativeMessage = representativeMessage;
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

	public Date getRepresentativeRegisterDate() {
		return representativeRegisterDate;
	}

	public void setRepresentativeRegisterDate(Date representativeRegisterDate) {
		this.representativeRegisterDate = representativeRegisterDate;
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

    public void setState(State state) {
        this.state = state;
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
        return serverURL + "/userVS/" + userId;
    }

    public JSONObject getMetaInfJSON() {
        if(metaInfJSON == null) metaInfJSON = (JSONObject) JSONSerializer.toJSON(metaInf);
        return metaInfJSON;
    }

    public static UserVS getUserVS (X509Certificate certificate) {
        UserVS userVS = new UserVS();
        userVS.setCertificate(certificate);
        String subjectDN = certificate.getSubjectDN().getName();
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

    public void beforeInsert(){
        if(nif != null) {
            this.nif = nif.toUpperCase();
        }
        if(name == null) {
            this.name = (firstName == null? "":firstName + " " + lastName == null? "":lastName).trim();
        }
    }

    public static UserVS parse (Map userVSDataMap) {
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
        if(!JSONNull.getInstance().equals(userVSDataMap.get("nif")) && userVSDataMap.containsKey("nif"))
            userVS.setNif((String) userVSDataMap.get("nif"));
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
        return userVS;
    }

    public JSONObject toJSON() throws Exception {
        JSONObject jsonData = new JSONObject();
        jsonData.put("id", id);
        jsonData.put("URL", url);
        jsonData.put("nif", nif);
        jsonData.put("IBAN", IBAN);
        if(state != null) jsonData.put("state", state.toString());
        if(type != null) jsonData.put("type", type.toString());
        if(reason != null) jsonData.put("reason", reason);
        if(certificate != null) {
            JSONArray jsonArrayData = new JSONArray();
            JSONObject jsonCertData = new JSONObject();
            jsonCertData.put("serialNumber", certificate.getSerialNumber());
            jsonCertData.put("pemCert", new String(CertUtils.getPEMEncoded(certificate), "UTF-8"));
            jsonArrayData.add(jsonCertData);
            jsonData.put("certificateList", jsonArrayData);
        }
        JSONObject deviceData = new JSONObject();
        deviceData.put("phone", getPhone());
        deviceData.put("email", getEmail());
        jsonData.put("deviceData", deviceData);
        jsonData.put("name", name);
        jsonData.put("firstName", firstName);
        jsonData.put("lastName", lastName);
        jsonData.put("description", description);
        return jsonData;
    }
}