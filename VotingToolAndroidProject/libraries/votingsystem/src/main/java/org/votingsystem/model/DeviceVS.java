package org.votingsystem.model;

import org.json.JSONObject;
import org.votingsystem.signature.util.CertUtils;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DeviceVS {

    public enum Type {MOBILE, PC}

    private Long id;
    private String deviceId;
    private String email;
    private String phone;
    private String deviceName;
    private X509Certificate x509Certificate;

    public static DeviceVS parse(JSONObject jsonObject) throws Exception {
        DeviceVS deviceVS = new DeviceVS();
        deviceVS.setId(jsonObject.getLong("id"));
        deviceVS.setDeviceName(jsonObject.getString("deviceName"));
        deviceVS.setEmail(jsonObject.getString("email"));
        deviceVS.setPhone(jsonObject.getString("phone"));
        deviceVS.setDeviceId(jsonObject.getString("deviceId"));
        Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(
                jsonObject.getString("certPEM").getBytes());
        deviceVS.setX509Certificate(certChain.iterator().next());
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

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
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
}
