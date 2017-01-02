package org.votingsystem.model;

import org.votingsystem.crypto.CertUtils;
import org.votingsystem.dto.CertExtensionDto;
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
        @NamedQuery(name = "findDeviceByUserAndDeviceId", query =
                "SELECT d FROM Device d WHERE d.user =:user and d.deviceId =:deviceId"),
        @NamedQuery(name = "findByDeviceIdAndUser", query = "SELECT d FROM Device d WHERE d.deviceId =:deviceId " +
                "and d.user =:user")
})
public class Device extends EntityBase implements Serializable {

	private static final long serialVersionUID = 1L;

    public enum Type {MOBILE, PC, BROWSER}

    public enum State {CANCELED, OK, PENDING, RENEWED}

	@Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false) private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="USER_ID") private User user;
    @Column(name="TYPE") @Enumerated(EnumType.STRING) private Type type = Type.MOBILE;
    @Column(name="DEVICE" ) private String deviceId;//hardware id
    @Column(name="EMAIL" ) private String email;
    @Column(name="PHONE" ) private String phone;
    @Column(name="STATE" ) @Enumerated(EnumType.STRING)  private State state;
    @Column(name="META_INF" ) private String metaInf;
    @OneToOne
    @JoinColumn(name="CERTIFICATE_ID")
    private Certificate certificate;
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

    public Device(Long deviceId) {
        this.id = deviceId;
    }

    public Device(User user, String deviceId, String email, String phone, String deviceName,
                  Certificate certificate) {
        this.user = user;
        this.deviceId =deviceId;
        this.email = email;
        this.phone = phone;
        this.deviceName = deviceName;
        this.certificate = certificate;
    }

    public Device(User user, String deviceId, String email, String phone, Type type) {
        this.user = user;
        this.deviceId =deviceId;
        this.email = email;
        this.phone = phone;
        this.type = type;
    }

    public Device(Long deviceId, String name) {
        this.id = deviceId;
        this.deviceName = name;
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

	public String getDeviceId() {
		return deviceId;
	}

	public Device setDeviceId(String deviceId) {
		this.deviceId = deviceId;
        return this;
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

    public X509Certificate getX509Certificate() throws Exception {
        if(x509Certificate == null && certificate != null) x509Certificate = CertUtils.loadCertificate(certificate.getContent());
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public Device updateCertInfo (X509Certificate certificate) throws Exception {
        this.x509Certificate = certificate;
        CertExtensionDto extensionDto = CertUtils.getCertExtensionData(CertExtensionDto.class, certificate,
                Constants.DEVICE_OID);
        return updateCertInfo(extensionDto);
    }

    public Device updateCertInfo (CertExtensionDto extensionDto) {
        setPhone(extensionDto.getMobilePhone());
        setEmail(extensionDto.getEmail());
        setDeviceId(extensionDto.getDeviceId());
        setType(extensionDto.getDeviceType());
        return this;
    }

}
