package org.votingsystem.client.webextension.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.UserVS;

import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrowserSessionDto {

    private Boolean isConnected = Boolean.FALSE;
    private UserVSDto userVS;
    private DeviceVSDto device;

    public BrowserSessionDto() {}

    public Boolean isConnected() {
        return isConnected;
    }

    public void setIsConnected(Boolean isConnected) {
        this.isConnected = isConnected;
    }

    public UserVSDto getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVSDto userVS) {
        this.userVS = userVS;
    }

    public DeviceVSDto getDevice() {
        return device;
    }

    public void setDevice(DeviceVSDto device) throws Exception {
        this.device = device;
        if(device.getCertPEM() != null) {
            X509Certificate signerCert = device.getX509Cert();
            userVS = new UserVSDto(UserVS.FROM_X509_CERT(signerCert)).setDeviceVS(device);
        }
    }

}
