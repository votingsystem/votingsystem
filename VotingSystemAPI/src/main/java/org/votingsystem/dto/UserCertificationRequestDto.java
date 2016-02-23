package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.AddressVS;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCertificationRequestDto {

    private AddressVS addressVS;
    private byte[] csrRequest;
    private byte[] token;

    public UserCertificationRequestDto(){}

    public UserCertificationRequestDto(AddressVS addressVS, byte[] csrRequest,  byte[] token) {
        this.addressVS = addressVS;
        this.csrRequest = csrRequest;
        this.token = token;
    }

    public byte[] getCsrRequest() {
        return csrRequest;
    }

    public void setCsrRequest(byte[] csrRequest) {
        this.csrRequest = csrRequest;
    }

    public AddressVS getAddressVS() {
        return addressVS;
    }

    public void setAddressVS(AddressVS addressVS) {
        this.addressVS = addressVS;
    }

    public byte[] getToken() {
        return token;
    }

    public void setToken(byte[] token) {
        this.token = token;
    }

}
