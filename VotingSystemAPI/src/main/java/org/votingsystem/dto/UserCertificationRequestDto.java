package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.Address;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCertificationRequestDto {

    private Address address;
    private byte[] csrRequest;
    private byte[] token;

    public UserCertificationRequestDto(){}

    public UserCertificationRequestDto(Address address, byte[] csrRequest, byte[] token) {
        this.address = address;
        this.csrRequest = csrRequest;
        this.token = token;
    }

    public byte[] getCsrRequest() {
        return csrRequest;
    }

    public void setCsrRequest(byte[] csrRequest) {
        this.csrRequest = csrRequest;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public byte[] getToken() {
        return token;
    }

    public void setToken(byte[] token) {
        this.token = token;
    }

}
