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
    private String UUID;

    public UserCertificationRequestDto(){}

    public UserCertificationRequestDto(AddressVS addressVS, byte[] csrRequest) {
        this.addressVS = addressVS;
        this.csrRequest = csrRequest;
        this.UUID = java.util.UUID.randomUUID().toString();
    }

    public byte[] getCsrRequest() {
        return csrRequest;
    }

    public void setCsrRequest(byte[] csrRequest) {
        this.csrRequest = csrRequest;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public AddressVS getAddressVS() {
        return addressVS;
    }

    public void setAddressVS(AddressVS addressVS) {
        this.addressVS = addressVS;
    }

}
