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
    private String nif;
    private String givenname;
    private String surname;
    private Device.Type deviceType;
    private String UUID;

    public CertExtensionDto() {}

    public CertExtensionDto(String nif , String givenname, String surname) {
        this.nif = nif;
        this.givenname = givenname;
        this.surname = surname;
    }

    public CertExtensionDto(String deviceName, String UUID, String email, String phone, Device.Type deviceType) {
        this.UUID = UUID;
        this.deviceName = deviceName;
        this.email = email;
        this.mobilePhone = phone;
        this.deviceType = deviceType;
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

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public Device.Type getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Device.Type deviceType) {
        this.deviceType = deviceType;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getGivenname() {
        return givenname;
    }

    public void setGivenname(String givenname) {
        this.givenname = givenname;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
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
        return "SERIALNUMBER=" + nif + ", GIVENNAME=" + givenname + ", SURNAME=" + surname;
    }

}
