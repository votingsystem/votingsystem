package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.votingsystem.dto.AddressDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.OperationType;


/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterDto {

    @JsonProperty("Operation")
    private OperationTypeDto operation;
    private AddressDto address;
    private UserDto user;
    private String mobileCsr;
    private String deviceId;

    public RegisterDto() {}

    public RegisterDto(UserDto user, String deviceId, String mobileCsr, String entityToId) {
        this.user = user;
        this.mobileCsr = mobileCsr;
        this.deviceId = deviceId;
        operation = new OperationTypeDto(CurrencyOperation.REGISTER, entityToId);
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    public String getMobileCsr() {
        return mobileCsr;
    }

    public RegisterDto setMobileCsr(String mobileCsr) {
        this.mobileCsr = mobileCsr;
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

}