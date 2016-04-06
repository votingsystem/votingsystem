package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InitSessionDto {

    private String deviceId;
    private String httpSessionId;

    public InitSessionDto() {}


    public String getDeviceId() {
        return deviceId;
    }

    public String getHttpSessionId() {
        return httpSessionId;
    }
}
