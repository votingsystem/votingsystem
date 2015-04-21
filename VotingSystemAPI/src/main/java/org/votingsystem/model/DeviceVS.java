package org.votingsystem.model;

import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="DeviceVS")
@NamedQueries({
        @NamedQuery(name = "findDeviceByUserAndDeviceId", query =
                "SELECT d FROM DeviceVS d WHERE d.userVS =:userVS and d.deviceId =:deviceId"),
        @NamedQuery(name = "findDeviceByDeviceId", query = "SELECT d FROM DeviceVS d WHERE d.deviceId =:deviceId"),
        @NamedQuery(name = "findByDeviceIdAndUserVS", query = "SELECT d FROM DeviceVS d WHERE d.deviceId =:deviceId " +
                "and d.userVS =:userVS")
})
public class DeviceVS extends EntityVS implements Serializable {

	private static final long serialVersionUID = 1L;

    public enum Type {MOBILE, PC}

    public enum State {CANCELED, OK, PENDING, RENEWED}

	@Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @Column(name="type") @Enumerated(EnumType.STRING) private Type type = Type.MOBILE;
    @Column(name="device" ) private String deviceId;
    @Column(name="email" ) private String email;
    @Column(name="phone" ) private String phone;
    @Column(name="state" ) @Enumerated(EnumType.STRING)  private State state;
    @Column(name="metaInf" ) private String metaInf;
    @OneToOne private CertificateVS certificateVS;
    @Column(name="reason", columnDefinition="TEXT") private String reason;
    @Column(name="deviceName") private String deviceName;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;
    @Transient private transient X509Certificate x509Certificate;

    public DeviceVS() {}

    public DeviceVS(Long deviceId) {
        this.id = deviceId;
    }

    public DeviceVS(UserVS userVS, String deviceId, String email, String phone, String deviceName,
            CertificateVS certificateVS) {
        this.userVS = userVS;
        this.deviceId =deviceId;
        this.email = email;
        this.phone = phone;
        this.deviceName = deviceName;
        this.certificateVS = certificateVS;
    }

    public DeviceVS(UserVS userVS, String deviceId, String email, String phone, Type type) {
        this.userVS = userVS;
        this.deviceId =deviceId;
        this.email = email;
        this.phone = phone;
        this.type = type;
    }

    public DeviceVS(Long deviceId, String name) {
        this.id = deviceId;
        this.deviceName = name;
    }

	public UserVS getUserVS() {
		return userVS;
	}

    public String getMetaInf() {
        return metaInf;
    }

    public void setMetaInf(String metaInf) {
        this.metaInf = metaInf;
    }

	public void setUserVS(UserVS userVS) {
		this.userVS = userVS;
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

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getEmail() {
		return email;
	}

	public DeviceVS setEmail(String email) {
		this.email = email;
        return this;
	}

    public State getState() {
        return state;
    }

    public DeviceVS setState(State state) {
        this.state = state;
        return this;
    }

	public String getPhone() {
		return phone;
	}

	public DeviceVS setPhone(String phone) {
        this.phone = phone;
        return this;
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

    public CertificateVS getCertificateVS() {
        return certificateVS;
    }

    public DeviceVS setCertificateVS(CertificateVS certificateVS) {
        this.certificateVS = certificateVS;
        return this;
    }

    public Type getType() {
        return type;
    }

    public DeviceVS setType(Type type) {
        this.type = type;
        return this;
    }

    public X509Certificate getX509Certificate() throws Exception {
        if(x509Certificate == null) x509Certificate = CertUtils.loadCertificate(certificateVS.getContent());
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public DeviceVS updateCertInfo (X509Certificate certificate) throws IOException {
        this.x509Certificate = certificate;
        return updateCertInfo(CertUtils.getCertExtensionData(certificate, ContextVS.DEVICEVS_OID));
    }

    public DeviceVS updateCertInfo (Map<String, String> dataMap) throws IOException {
        setPhone(dataMap.get("mobilePhone"));
        setEmail(dataMap.get("email"));
        setDeviceId(dataMap.get("deviceId"));
        setType(Type.valueOf(dataMap.get("deviceType")));
        return this;
    }

}
