package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.util.crypto.PEMUtils;

import java.io.Serializable;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deviceId;
    private String deviceName;
    private String email;
    private String phone;
    private String publicKeyPEM;
    private String x509CertificatePEM;
    private AESParamsDto aesParams;
    private String firstName;
    private String lastName;
    private String NIF;
    private String IBAN;
    private Device.Type deviceType;

    public DeviceDto() {}

    public DeviceDto(User user, CertExtensionDto certExtensionDto) {
        this.NIF = user.getNif();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phone = certExtensionDto.getMobilePhone();
        this.email = certExtensionDto.getEmail();
        this.deviceId = certExtensionDto.getDeviceId();
        this.deviceType = certExtensionDto.getDeviceType();
    }

    public DeviceDto(Long id, String name, String deviceId) {
        this.setId(id);
        this.setDeviceName(name);
        this.setDeviceId(deviceId);
    }

    public static DeviceDto INIT_SIGNED_SESSION(User user) throws Exception {
        DeviceDto deviceDto = new DeviceDto(user.getDevice());
        deviceDto.setIBAN(user.getIBAN());
        return deviceDto;
    }

    public static DeviceDto INIT_BROWSER_SESSION(User user, Device browserDevice) throws Exception {
        DeviceDto deviceDto = new DeviceDto(browserDevice);
        deviceDto.setIBAN(user.getIBAN());
        return deviceDto;
    }


    public DeviceDto(Device device) throws Exception {
        this.setId(device.getId());
        this.setDeviceId(device.getDeviceId());
        this.setDeviceName(device.getDeviceName());
        this.setPhone(device.getPhone());
        this.setEmail(device.getEmail());
        this.setAesParams(device.getAesParams());
        X509Certificate x509Cert = device.getX509Certificate();
        if(x509Cert != null) x509CertificatePEM = new String(PEMUtils.getPEMEncoded(x509Cert));
    }

    @JsonIgnore
    public Device getDevice() throws Exception {
        Device device = new Device();
        device.setId(getId());
        device.setDeviceId(getDeviceId());
        device.setDeviceName(getDeviceName());
        device.setEmail(getEmail());
        device.setPhone(getPhone());
        if(getX509CertificatePEM() != null) {
            Collection<X509Certificate> certChain = PEMUtils.fromPEMToX509CertCollection(getX509CertificatePEM().getBytes());
            device.setX509Certificate(certChain.iterator().next());
        }
        return device;
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

    public String getX509CertificatePEM() {
        return x509CertificatePEM;
    }

    public void setX509CertificatePEM(String x509CertificatePEM) {
        this.x509CertificatePEM = x509CertificatePEM;
    }

    @JsonIgnore public X509Certificate getX509Certificate() throws Exception {
        if(x509CertificatePEM == null) return null;
        else return PEMUtils.fromPEMToX509Cert(x509CertificatePEM.getBytes());
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNIF() {
        return NIF;
    }

    public void setNIF(String NIF) {
        this.NIF = NIF;
    }

    public Device.Type getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Device.Type deviceType) {
        this.deviceType = deviceType;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

    @JsonIgnore public PublicKey getPublicKey() throws Exception {
        if(publicKeyPEM == null) return null;
        else return PEMUtils.fromPEMToRSAPublicKey(publicKeyPEM);
    }

    public AESParamsDto getAesParams() {
        return aesParams;
    }

    public DeviceDto setAesParams(AESParamsDto aesParams) {
        this.aesParams = aesParams;
        return this;
    }
}
