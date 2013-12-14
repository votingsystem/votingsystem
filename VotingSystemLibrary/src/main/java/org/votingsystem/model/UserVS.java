package org.votingsystem.model;

import org.apache.log4j.Logger;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.encoders.Hex;
import org.votingsystem.signature.util.CMSUtils;

import javax.persistence.*;
import javax.xml.bind.DatatypeConverter;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity @Table(name="UserVS") @DiscriminatorValue("UserVS")
public class UserVS implements Serializable {

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

    public void setDescription(String description) {
        this.description = description;
    }

    public enum Type {USER, REPRESENTATIVE, USER_WITH_CANCELLED_REPRESENTATIVE, EX_REPRESENTATIVE}
	
    private static final long serialVersionUID = 1L;
    
    private static Logger log = Logger.getLogger(UserVS.class);

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
	@Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type;
    @Column(name="nif", nullable=false) private String nif;

    @Column(name="name") private String name;

    @Column(name="url") private String url;
    
    @Column(name="metaInf", columnDefinition="TEXT") private String metaInf = "{\"numRepresentations\"=1}";
    
    @Column(name="firstName" ) private String firstName;

    @Column(name="lastName" ) private String lastName;

    @Column(name="description" ) private String description;
    
    @Column(name="representativeMessage" ) private MessageSMIME representativeMessage;
    
    @Column(name="country" ) private String country;
    
    @Column(name="phone" ) private String phone;
    
    @Column(name="email" ) private String email;

    @Column(name="cn") private String cn;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="representative") private UserVS representative;
    
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="representativeRegisterDate", length=23) private Date representativeRegisterDate;
    
    @Transient private transient X509Certificate certificate;
    @Transient private transient CertificateVS certificateCA;
    @Transient private transient TimeStampToken timeStampToken;
    @Transient private transient SignerInformation signerInformation;
    @Transient private KeyStore keyStore;

    public UserVS() {}

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
	
	public void beforeInsert(){ if(nif != null) this.nif = nif.toUpperCase();}

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

    @Transient public String getSignatureBase64() {
        if (signerInformation.getSignature() == null) return null;
        return DatatypeConverter.printBase64Binary(signerInformation.getSignature());
    }

    @Transient public String getSignatureHex() {
        if (signerInformation.getSignature() == null) return null;
        return new String(signerInformation.getSignature());
    }

    @Transient public String getEncryptiontId() {
        if(signerInformation == null) return null;
        else return CMSUtils.getEncryptiontId(signerInformation.getEncryptionAlgOID()); 
    }

    @Transient public Date getSignatureDate() {
        if(timeStampToken == null) return null;
        return timeStampToken.getTimeStampInfo().getGenTime();
    }

    @Transient public String getDigestId() {
        if(signerInformation == null) return null;
        else return CMSUtils.getDigestId(signerInformation.getDigestAlgOID()); }

    @Transient public String getContentDigestHex() {
        if (signerInformation.getContentDigest() == null) return null;
        return new String(Hex.encode(signerInformation.getContentDigest()));
    }

    public void setSigner(SignerInformation signer) {
        this.signerInformation = signer;
    }

    @Transient public String getContentDigestBase64() {
        if (signerInformation.getContentDigest() == null) return null;
        return DatatypeConverter.printBase64Binary(signerInformation.getContentDigest());
    }

    public static UserVS getUserVS (X509Certificate certificate) {
        UserVS userVS = new UserVS();
        userVS.setCertificate(certificate);
        String subjectDN = certificate.getSubjectDN().getName();
        if (subjectDN.contains("C=")) userVS.setCountry(subjectDN.split("C=")[1].split(",")[0]);
        if (subjectDN.contains("SERIALNUMBER=")) userVS.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
        if (subjectDN.contains("SURNAME=")) userVS.setFirstName(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.contains("GIVENNAME=")) userVS.setName(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
        if (subjectDN.contains("CN=")) userVS.setCn(subjectDN.split("CN=")[1]);
        if (subjectDN.split("emailAddress=").length > 1) userVS.setEmail(subjectDN.split("emailAddress=")[1].split(",")[0]);
        if (subjectDN.split("mobilePhone=").length > 1) userVS.setPhone(subjectDN.split("mobilePhone=")[1].split(",")[0]);
        return userVS;
    }

    public static UserVS populate (Map userVSDataMap) {
        UserVS userVS = new UserVS();
        if(userVSDataMap.containsKey("name")) userVS.setName((String) userVSDataMap.get("name"));
        if(userVSDataMap.containsKey("nif")) userVS.setNif((String) userVSDataMap.get("nif"));
        if(userVSDataMap.containsKey("nif")) userVS.setId(((Integer) userVSDataMap.get("id")).longValue());
        if(userVSDataMap.containsKey("email")) userVS.setEmail((String) userVSDataMap.get("email"));
        if(userVSDataMap.containsKey("phone")) userVS.setPhone((String) userVSDataMap.get("phone"));
        if(userVSDataMap.containsKey("firstName")) userVS.setFirstName((String) userVSDataMap.get("firstName"));
        if(userVSDataMap.containsKey("metaInf")) userVS.setMetaInf((String) userVSDataMap.get("metaInf"));
        if(userVSDataMap.containsKey("country")) userVS.setCountry((String) userVSDataMap.get("country"));
        if(userVSDataMap.containsKey("type")) {
            Type type = Type.valueOf((String) userVSDataMap.get("type"));
            userVS.setType(type);
        }
        return userVS;
    }

}