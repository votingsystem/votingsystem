package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.Device;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertExtensionDto {

    private String deviceName;
    private String email;
    private String mobilePhone;
    private String numId;
    private String givenname;
    private String surname;
    private String entityId;
    private Device.Type deviceType;
    private String UUID;

    public CertExtensionDto() {}

    public CertExtensionDto(String numId , String givenname, String surname) {
        this.numId = numId;
        this.givenname = givenname;
        this.surname = surname;
    }

    public CertExtensionDto(String deviceName, String UUID, String email, String phone, Device.Type deviceType) {
        this.deviceName = deviceName;
        this.UUID = UUID;
        this.email = email;
        this.mobilePhone = phone;
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public CertExtensionDto setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public CertExtensionDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public CertExtensionDto setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
        return this;
    }

    public Device.Type getDeviceType() {
        return deviceType;
    }

    public CertExtensionDto setDeviceType(Device.Type deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public String getNumId() {
        return numId;
    }

    public CertExtensionDto setNumId(String numId) {
        this.numId = numId;
        return this;
    }

    public String getGivenname() {
        return givenname;
    }

    public CertExtensionDto setGivenname(String givenname) {
        this.givenname = givenname;
        return this;
    }

    public String getSurname() {
        return surname;
    }

    public CertExtensionDto setSurname(String surname) {
        this.surname = surname;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public CertExtensionDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    @JsonIgnore
    public String getPrincipal() {
        return "SERIALNUMBER=" + numId + ", GIVENNAME=" + givenname + ", SURNAME=" + surname;
    }

    public String getEntityId() {
        return entityId;
    }

    public CertExtensionDto setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

}
