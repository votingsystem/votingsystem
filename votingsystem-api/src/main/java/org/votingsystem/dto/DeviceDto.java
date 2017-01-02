package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.util.IdDocument;

import java.io.Serializable;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deviceId;
    private String deviceName;
    private String email;
    private String phone;
    private String publicKeyPEM;
    private String x509CertificatePEM;
    private String name;
    private String Surname;
    private String numId;
    private IdDocument documentType;
    private String IBAN;
    private Device.Type deviceType;

    @JsonIgnore private PublicKey publicKey;

    public DeviceDto() {}

    public DeviceDto(String phone, String email) {
        this.phone = phone;
        this.email = email;
    }

    public DeviceDto(User user, CertExtensionDto certExtensionDto) {
        this.numId = user.getNumId();
        this.name = user.getName();
        this.Surname = user.getSurname();
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
        X509Certificate x509Cert = device.getX509Certificate();
        if(x509Cert != null) x509CertificatePEM = new String(PEMUtils.getPEMEncoded(x509Cert));
    }

    public DeviceDto(Long id) {
        this.setId(id);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return Surname;
    }

    public void setSurname(String surname) {
        this.Surname = surname;
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

    public IdDocument getDocumentType() {
        return documentType;
    }

    public void setDocumentType(IdDocument documentType) {
        this.documentType = documentType;
    }

    public String getNumId() {
        return numId;
    }

    public void setNumId(String numId) {
        this.numId = numId;
    }

    public DeviceDto setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }
}
