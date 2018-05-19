package org.votingsystem.model;

import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.crypto.cms.CMSUtils;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.util.IdDocument;
import org.votingsystem.util.JSON;

import javax.persistence.*;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="SIGNER")
@NamedQueries({
        @NamedQuery(name = User.FIND_USER_BY_TYPE, query = "SELECT u FROM User u WHERE u.type =:type"),
        @NamedQuery(name = User.FIND_USER_BY_NIF, query = "SELECT u FROM User u WHERE u.numId =:numId"),
        @NamedQuery(name = User.FIND_USER_BY_IBAN, query = "SELECT u FROM User u WHERE u.IBAN =:IBAN"),
        @NamedQuery(name = User.FIND_USER_IN_LIST_ACTIVE_OR_CANCELED_AFTER, query = "SELECT u FROM User u " +
                "WHERE u.type in :typeList and(u.state = 'ACTIVE' or (u.state != 'ACTIVE' and(u.dateCanceled >=:dateCanceled)))"),
        @NamedQuery(name = User.COUNT_USER_ACTIVE_BY_DATE_AND_IN_LIST, query = "SELECT COUNT(u) FROM User u " +
                "WHERE (u.dateCanceled is null OR u.dateCanceled >=:date) and u.type in :inList")
})
public class User extends EntityBase implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(User.class.getName());


    public static final String FIND_USER_IN_LIST_ACTIVE_OR_CANCELED_AFTER = "User.findUserInListActiveOrCancelledAfter";
    public static final String FIND_USER_BY_TYPE = "User.findUserByType";
    public static final String FIND_USER_BY_NIF = "User.findUserByNif";
    public static final String FIND_USER_BY_IBAN = "User.findUserByIBAN";
    public static final String COUNT_USER_ACTIVE_BY_DATE_AND_IN_LIST = "User.countUserActiveByDateAndInList";

    public enum Type {ENTITY, USER, ID_CARD_USER, TIMESTAMP_SERVER, ANON_ELECTOR, BANK, CURRENCY_SERVER, IDENTITY_SERVER,
        ANON_CURRENCY, BROWSER, MOBILE
    }

    public enum State {ACTIVE, PENDING, SUSPENDED, CANCELED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false) private Long id;
    @Column(name="ENTITY_ID")
    private String entityId;
	@Column(name="TYPE", nullable=false) @Enumerated(EnumType.STRING) private Type type = Type.USER;
    /** identification document number*/
    @Column(name="NUM_ID")
    private String numId;
    @Column(name="TYPE_ID")
    @Enumerated(EnumType.STRING)
    private IdDocument documentType;
    @Column(name="IBAN") private String IBAN;
    @Column(name="NAME") private String name;
    @Column(name="SURNAME" ) private String surname;
    @Column(name="UUID" ) private String UUID;
    @Column(name="URL") private String url;
    @Column(name="META_INF", columnDefinition="TEXT") private String metaInf;
    @Column(name="DESCRIPTION", columnDefinition="TEXT" ) private String description;
    @Column(name="COUNTRY" ) private String country;
    @Column(name="PHONE" ) private String phone;
    @Column(name="EMAIL" ) private String email;
    @Column(name="CN") private String cn;
    @Column(name="STATE") @Enumerated(EnumType.STRING) private State state = State.ACTIVE;
    @OneToOne
    @JoinColumn(name="ADDRESS_ID")
    private Address address;
    @Column(name="DATE_CANCELED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCanceled;
    @Column(name="DATE_ACTIVATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateActivated;
    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;
    
    @Transient private transient X509Certificate x509Certificate;
    @Transient private transient Certificate certificate;
    @Transient private transient Certificate certificateCA;
    @Transient private transient Map metaInfMap;
    @Transient private transient TimeStampToken timeStampToken;
    @Transient private transient SignerInformation signerInformation;
    @Transient private transient Device device;
    @Transient private KeyStore keyStore;
    @Transient private boolean isValidElector = false;
    @Transient private boolean isAnonymousUser = false;

    public User() {}

    public User(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public User(Type type, String name, String surname, String email, String phone) {
        this.type = type;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phone = phone;
    }

    public User(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getIBAN() {
        return IBAN;
    }

    public User setIBAN(String IBAN) {
        this.IBAN = IBAN;
        return this;
    }

    public User setNumIdAndType(String numId, IdDocument typeId) {
        this.numId = numId;
        this.documentType = typeId;
        return this;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public User setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
        return this;
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

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getCn() {
        return cn;
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

	public Certificate getCertificateCA() {
		return certificateCA;
	}

	public User setCertificateCA(Certificate certificate) {
		this.certificateCA = certificate;
		return this;
	}

	public Type getType() {
		return type;
	}

	public User setType(Type type) {
		this.type = type;
        return this;
	}

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surName) {
        this.surname = surName;
    }

    public String getEntityId() {
        return entityId;
    }

    public User setEntityId(String entityId) {
        this.entityId = entityId;
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

    public void setSignerInformation(SignerInformation signer) {
        this.signerInformation = signer;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getDateCanceled() {
        return dateCanceled;
    }

    public void setDateCanceled(LocalDateTime dateCanceled) {
        this.dateCanceled = dateCanceled;
    }

    public String getDescription() {
        return description;
    }

    public User setDescription(String description) {
        this.description = description;
        return this;
    }

    public LocalDateTime getDateActivated() {
        return dateActivated;
    }

    public void setDateActivated(LocalDateTime dateActivated) {
        this.dateActivated = dateActivated;
    }

    public State getState() {
        return state;
    }

    public User setState(State state) {
        this.state = state;
        return this;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public User setCertificate(Certificate certificate) {
        this.certificate = certificate;
        return this;
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

    public String getSignedContentDigestBase64() {
        if (signerInformation.getContentDigest() == null) return null;
        return DatatypeConverter.printBase64Binary(signerInformation.getContentDigest());
    }

    public boolean isValidElector() {
        return isValidElector;
    }

    public User setValidElector(boolean validElector) {
        isValidElector = validElector;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public Map getMetaInfMap() throws IOException {
        if(metaInfMap == null) {
            if(metaInf == null) {
                metaInfMap = new HashMap<>();
                metaInf = new JSON().getMapper().writeValueAsString(metaInfMap);
            } else metaInfMap = new JSON().getMapper().readValue(metaInf, HashMap.class);
        }
        return metaInfMap;
    }

    public String getNumId() {
        return numId;
    }

    public String getNumIdAndType() {
        return numId + " " + documentType;
    }

    public void setNumId(String numId) {
        this.numId = numId;
    }

    public IdDocument getDocumentType() {
        return documentType;
    }

    public void setDocumentType(IdDocument typeId) {
        this.documentType = typeId;
    }


    public Device getDevice() {
        return device;
    }

    public User setDevice(Device device) {
        this.device = device;
        return this;
    }

    public boolean isAnonymousUser() {
        return isAnonymousUser;
    }

    public void setAnonymousUser(boolean anonymousUser) {
        isAnonymousUser = anonymousUser;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getFullName() {
        return name + " " + surname;
    }

    @Override @PrePersist
    public void prePersist() {
        LocalDateTime date = LocalDateTime.now();
        setDateCreated(date);
        setLastUpdated(date);
        if(UUID == null)
            setUUID(java.util.UUID.randomUUID().toString());
    }

    public static <T extends User> T getUser(Class<T> userClass, X500Name subject) throws IllegalAccessException,
            InstantiationException {
        T result = userClass.newInstance();
        String unasignedAttributes = null;
        for(RDN rdn : subject.getRDNs()) {
            AttributeTypeAndValue attributeTypeAndValue = rdn.getFirst();
            if(BCStyle.SERIALNUMBER.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setNumIdAndType(attributeTypeAndValue.getValue().toString(), IdDocument.NIF);
            } else if(BCStyle.SURNAME.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setSurname(attributeTypeAndValue.getValue().toString());
            } else if(BCStyle.GIVENNAME.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setName(attributeTypeAndValue.getValue().toString());
            } else if(BCStyle.CN.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setCn(attributeTypeAndValue.getValue().toString());
            } else if(BCStyle.C.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setCountry(attributeTypeAndValue.getValue().toString());
            } else {
                if(unasignedAttributes == null)
                    unasignedAttributes = "oid: " + attributeTypeAndValue.getType().getId() + ", value: " +
                            attributeTypeAndValue.getValue().toString();
                else unasignedAttributes = " - oid: " + attributeTypeAndValue.getType().getId() + ", value: " +
                        attributeTypeAndValue.getValue().toString();
            }
        }
        if(unasignedAttributes != null)
            log.fine("unasignedAttributes: " + unasignedAttributes);
        return result;
    }

    /**
     *
     * @param x509Certificate
     * @param type
     * @return
     * @throws CertificateEncodingException
     */
    public static User FROM_CERT(X509Certificate x509Certificate, User.Type type) throws CertificateEncodingException,
            InstantiationException, IllegalAccessException {
        X500Name x500name = new JcaX509CertificateHolder(x509Certificate).getSubject();
        User user = getUser(User.class, x500name).setType(type);
        user.setX509Certificate(x509Certificate);
        if(user.getName() == null)
            user.setName(x509Certificate.getSubjectDN().toString());
        return user;
    }

    public static <T extends User> T FROM_CERT(Class<T> userClass, X509Certificate x509Certificate, User.Type type)
            throws IllegalAccessException, InstantiationException, CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(x509Certificate).getSubject();
        T result = getUser(userClass, x500name);
        result.setX509Certificate(x509Certificate);
        if(result.getName() == null)
            result.setName(x509Certificate.getSubjectDN().toString());
        return result;
    }

}