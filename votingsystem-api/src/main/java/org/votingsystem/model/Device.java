package org.votingsystem.model;

import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.util.Constants;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="DEVICE")
@NamedQueries({
        @NamedQuery(name = Device.FIND_BY_UUID, query = "SELECT d FROM Device d WHERE d.user =:user and d.UUID =:deviceUUID"),
        @NamedQuery(name = Device.FIND_BY_USER_AND_UUID, query = "SELECT d FROM Device d WHERE d.UUID =:deviceUUID and d.user =:user")
})
public class Device extends EntityBase implements Serializable {

	private static final long serialVersionUID = 1L;

    public static final String FIND_BY_UUID= "Device.findByUUID";
    public static final String FIND_BY_USER_AND_UUID = "Device.findByUserAndUUID";


    public enum Type {MOBILE, PC, BROWSER}

    public enum State {CANCELED, OK, PENDING, RENEWED}

	@Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false) private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="USER_ID") private User user;
    @Column(name="TYPE") @Enumerated(EnumType.STRING) private Type type = Type.MOBILE;
    @Column(name="UUID") private String UUID;
    @Column(name="EMAIL") private String email;
    @Column(name="PHONE") private String phone;
    @Column(name="STATE") @Enumerated(EnumType.STRING)  private State state;
    @Column(name="META_INF") private String metaInf;
    @OneToOne @JoinColumn(name="CERTIFICATE_ID")
    private Certificate certificate;
    @OneToOne @JoinColumn(name="SIGNED_DOCUMENT")
    private SignedDocument signedDocument;
    @Column(name="REASON", columnDefinition="TEXT") private String reason;
    @Column(name="DEVICE_NAME") private String deviceName;
    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    @Transient private transient X509Certificate x509Certificate;

    public Device() {}

    public Device(User user, String UUID, String email, String phone, String deviceName, Certificate certificate){
        this.user = user;
        this.UUID = UUID;
        this.email = email;
        this.phone = phone;
        this.deviceName = deviceName;
        this.certificate = certificate;
    }

    public Device(User user, String UUID, String email, String phone, Type type) {
        this.user = user;
        this.UUID = UUID;
        this.email = email;
        this.phone = phone;
        this.type = type;
    }

    public Device(Long deviceId, String name) {
        this.id = deviceId;
        this.deviceName = name;
    }


    public Device(DeviceDto deviceDto) throws Exception {
        this.UUID = deviceDto.getUUID();
        this.deviceName = deviceDto.getDeviceName();
        this.email = deviceDto.getEmail();
        this.phone = deviceDto.getPhone();
        this.x509Certificate = deviceDto.getX509Certificate();
    }

	public User getUser() {
		return user;
	}

    public String getMetaInf() {
        return metaInf;
    }

    public void setMetaInf(String metaInf) {
        this.metaInf = metaInf;
    }

	public void setUser(User user) {
		this.user = user;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public Device setEmail(String email) {
		this.email = email;
        return this;
	}

    public State getState() {
        return state;
    }

    public Device setState(State state) {
        this.state = state;
        return this;
    }

	public String getPhone() {
		return phone;
	}

	public Device setPhone(String phone) {
        this.phone = phone;
        return this;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDeviceName() {
        if(deviceName == null) deviceName = type.toString() + " - " + email;
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public Device setCertificate(Certificate certificate) {
        this.certificate = certificate;
        return this;
    }

    public Type getType() {
        return type;
    }

    public Device setType(Type type) {
        this.type = type;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public Device setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public X509Certificate getX509Certificate() throws Exception {
        if(x509Certificate == null && certificate != null)
            x509Certificate = CertificateUtils.loadCertificate(certificate.getContent());
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public Device setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
        return this;
    }

    public Device updateCertInfo (X509Certificate certificate) throws Exception {
        this.x509Certificate = certificate;
        CertExtensionDto extensionDto = CertificateUtils.getCertExtensionData(CertExtensionDto.class, certificate,
                Constants.DEVICE_OID);
        return updateCertInfo(extensionDto);
    }

    public Device updateCertInfo (CertExtensionDto extensionDto) {
        setPhone(extensionDto.getMobilePhone());
        setEmail(extensionDto.getEmail());
        setUUID(extensionDto.getUUID());
        setType(extensionDto.getDeviceType());
        return this;
    }

}
