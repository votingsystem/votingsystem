package org.votingsystem.managed;

import org.votingsystem.dto.ResponseDto;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ServiceUpdatedMessage {

    private ResponseDto response;
    private String clientUUID;

    public ServiceUpdatedMessage(ResponseDto response, String clientUUID) {
        this.response = response;
        this.clientUUID = clientUUID;
    }

    public ResponseDto getResponse() {
        return response;
    }

    public String getClientUUID() {
        return clientUUID;
    }

}
