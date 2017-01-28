package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.util.IdDocument;

import java.io.IOException;
import java.io.Serializable;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceDto implements Serializable {

    private static final long serialVersionUID = 1L;


    private String deviceName;
    private String email;
    private String phone;
    private String publicKeyPEM;
    private String certificatePEM;
    private String name;
    private String surname;
    private String numId;
    private IdDocument documentType;
    private Device.Type deviceType;
    private String UUID;

    public DeviceDto() {}

    public DeviceDto(String phone, String email, String UUID, String deviceName) {
        this.phone = phone;
        this.email = email;
        this.UUID = UUID;
        this.deviceName = deviceName;
    }

    public DeviceDto(User user, CertExtensionDto certExtensionDto) {
        this.numId = user.getNumId();
        this.name = user.getName();
        this.surname = user.getSurname();
        this.phone = certExtensionDto.getMobilePhone();
        this.email = certExtensionDto.getEmail();
        this.UUID = certExtensionDto.getUUID();
        this.deviceType = certExtensionDto.getDeviceType();
    }

    public DeviceDto(Device device) throws Exception {
        this.UUID = device.getUUID();
        this.deviceName = device.getDeviceName();
        this.phone = device.getPhone();
        this.email = device.getEmail();
        X509Certificate x509Cert = device.getX509Certificate();
        if(x509Cert != null)
            certificatePEM = new String(PEMUtils.getPEMEncoded(x509Cert));
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

    public String getCertificatePEM() {
        return certificatePEM;
    }

    public void setCertificatePEM(String certificatePEM) {
        this.certificatePEM = certificatePEM;
    }

    @JsonIgnore
    public X509Certificate getX509Certificate() throws Exception {
        if(certificatePEM == null) return null;
        else return PEMUtils.fromPEMToX509Cert(certificatePEM.getBytes());
    }

    @JsonIgnore
    public String getUserFullName() {
        return name + " " + surname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public Device.Type getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Device.Type deviceType) {
        this.deviceType = deviceType;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

    public void setPublicKey(PublicKey publicKey) throws IOException {
        this.publicKeyPEM = new String(PEMUtils.getPEMEncoded(publicKey));
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

    public String getUUID() {
        return UUID;
    }

    public DeviceDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

}