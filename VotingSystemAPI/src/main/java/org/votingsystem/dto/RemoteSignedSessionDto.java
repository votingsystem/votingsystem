package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteSignedSessionDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private byte[] csrRequest;

    public byte[] getCsrRequest() {
        return csrRequest;
    }

    public void setCsrRequest(byte[] csrRequest) {
        this.csrRequest = csrRequest;
    }
}
