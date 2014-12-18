package org.votingsystem.model;

import net.sf.json.JSONObject;
import org.votingsystem.signature.util.CertUtils;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="DeviceVS")
public class DeviceVS implements Serializable {

	private static final long serialVersionUID = 1L;

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public enum Type {MOBILE, PC}

	@Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @Column(name="type") @Enumerated(EnumType.STRING) private Type type = Type.MOBILE;
    @Column(name="device" ) private String deviceId;
    @Column(name="email" ) private String email;
    @Column(name="phone" ) private String phone;
    @OneToOne private CertificateVS certificateVS;
    @Column(name="reason", columnDefinition="TEXT") private String reason;
    @Column(name="deviceName") private String deviceName;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;
    @Transient private transient X509Certificate x509Certificate;

	public UserVS getUserVS() {
		return userVS;
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
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public CertificateVS getCertificateVS() {
        return certificateVS;
    }

    public void setCertificateVS(CertificateVS certificateVS) {
        this.certificateVS = certificateVS;
    }

    public DeviceVS.Type getType() {
        return type;
    }

    public DeviceVS setType(Type type) {
        this.type = type;
        return this;
    }

    public DeviceVS updateCertInfo (X509Certificate certificate) throws IOException {
        return updateCertInfo(CertUtils.getCertExtensionData(certificate, ContextVS.DEVICEVS_OID));
    }

    public DeviceVS updateCertInfo (JSONObject jsonData) throws IOException {
        setPhone(jsonData.getString("mobilePhone"));
        setEmail(jsonData.getString("email"));
        setDeviceId(jsonData.getString("deviceId"));
        setType(Type.valueOf(jsonData.getString("deviceType")));
        return this;
    }

    public static DeviceVS parse(JSONObject jsonObject) throws Exception {
        DeviceVS deviceVS = new DeviceVS();
        deviceVS.setId(jsonObject.getLong("id"));
        deviceVS.setDeviceName(jsonObject.getString("deviceName"));
        deviceVS.setEmail(jsonObject.getString("email"));
        deviceVS.setPhone(jsonObject.getString("phone"));
        deviceVS.setDeviceId(jsonObject.getString("deviceId"));
        Collection<X509Certificate>  certChain = CertUtils.fromPEMToX509CertCollection(
                jsonObject.getString("certPEM").getBytes());
        deviceVS.setX509Certificate(certChain.iterator().next());
        return deviceVS;
    }

}
