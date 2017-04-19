package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.votingsystem.dto.AddressDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.util.CurrencyOperation;


/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterDto {

    private OperationTypeDto operation;
    private AddressDto address;
    private UserDto user;
    private String csr;
    private String issuedCertificate;
    private String deviceId;

    public RegisterDto() {}

    public RegisterDto(UserDto user, String deviceId, String csr, String entityToId) {
        this.user = user;
        this.csr = csr;
        this.deviceId = deviceId;
        operation = new OperationTypeDto(CurrencyOperation.REGISTER_DEVICE, entityToId);
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    public String getCsr() {
        return csr;
    }

    public RegisterDto setCsr(String csr) {
        this.csr = csr;
        return this;
    }

    public UserDto getUser() {
        return user;
    }

    public RegisterDto setUser(UserDto user) {
        this.user = user;
        return this;
    }

    public OperationTypeDto getOperation() {
        return operation;
    }

    public RegisterDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public RegisterDto setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public String getIssuedCertificate() {
        return issuedCertificate;
    }

    public void setIssuedCertificate(String issuedCertificate) {
        this.issuedCertificate = issuedCertificate;
    }

}