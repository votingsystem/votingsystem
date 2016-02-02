package org.votingsystem.client.webextension.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.UserVSDto;

import java.util.ArrayList;
import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrowserSessionDto {

    private Boolean isConnected = Boolean.FALSE;
    private UserVSDto userVS;
    private DeviceVSDto cryptoToken;
    private DeviceVSDto device;

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

    public DeviceVSDto getCryptoToken() {
        return cryptoToken;
    }

    public void setCryptoToken(DeviceVSDto cryptoToken) {
        this.cryptoToken = cryptoToken;
    }

    public DeviceVSDto getDevice() {
        return device;
    }

    public void setDevice(DeviceVSDto device) {
        this.device = device;
    }

}
