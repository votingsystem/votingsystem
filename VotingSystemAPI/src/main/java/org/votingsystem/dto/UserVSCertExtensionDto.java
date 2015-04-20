package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.DeviceVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserVSCertExtensionDto {

    private String deviceId;
    private String deviceName;
    private String email;
    private String mobilePhone;
    private DeviceVS.Type deviceType;


    public UserVSCertExtensionDto() {}

    public UserVSCertExtensionDto(String deviceId, String deviceName, String email, String phone, DeviceVS.Type deviceType) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.email = email;
        this.mobilePhone = phone;
        this.deviceType = deviceType;
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

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public DeviceVS.Type getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceVS.Type deviceType) {
        this.deviceType = deviceType;
    }
}
