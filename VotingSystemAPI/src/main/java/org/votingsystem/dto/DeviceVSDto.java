package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CryptoTokenVS;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceVSDto {

    private Long id;
    private String deviceId;
    private String sessionId;
    private String deviceName;
    private String email;
    private String phone;
    private String certPEM;
    private CryptoTokenVS type;

    public DeviceVSDto loadUserVS(UserVS userVS) {
        setType(CryptoTokenVS.JKS_KEYSTORE);
        setDeviceName(userVS.getNif() + " - " + userVS.getName());
        return this;
    }

    public DeviceVSDto() {}

    public DeviceVSDto(Long id, String name) {
        this.setId(id);
        this.setDeviceName(name);
    }

    public DeviceVSDto(String deviceId, String sessionId) {
        this.setDeviceId(deviceId);
        this.setSessionId(sessionId);
    }

    public DeviceVSDto(DeviceVS deviceVS) throws Exception {
        this.setId(deviceVS.getId());
        this.setDeviceId(deviceVS.getDeviceId());
        this.setDeviceName(deviceVS.getDeviceName());
        this.setPhone(deviceVS.getPhone());
        this.setEmail(deviceVS.getEmail());
        X509Certificate x509Cert = deviceVS.getX509Certificate();
        if(x509Cert != null) certPEM = new String(CertUtils.getPEMEncoded(x509Cert));
    }

    public DeviceVS getDeviceVS() throws Exception {
        DeviceVS deviceVS = new DeviceVS();
        deviceVS.setId(getId());
        deviceVS.setDeviceId(getDeviceId());
        deviceVS.setDeviceName(getDeviceName());
        deviceVS.setEmail(getEmail());
        deviceVS.setPhone(getPhone());
        if(getCertPEM() != null) {
            Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(getCertPEM().getBytes());
            deviceVS.setX509Certificate(certChain.iterator().next());
        }

        return deviceVS;
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

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCertPEM() {
        return certPEM;
    }

    public void setCertPEM(String certPEM) {
        this.certPEM = certPEM;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public CryptoTokenVS getType() {
        return type;
    }

    public void setType(CryptoTokenVS type) {
        this.type = type;
    }


}
