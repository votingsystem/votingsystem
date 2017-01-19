package org.votingsystem.dto.indentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.dto.AddressDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.UserDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "BrowserCertification")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrowserCertificationDto {

    @JacksonXmlProperty(localName = "Operation")
    private OperationTypeDto operation;

    private Integer statusCode;

    private AddressDto address;
    private UserDto user;
    private String signerCertPEM;

    private String browserCsr;
    private String browserCsrSigned;
    private String browserUUID;

    private String mobileCsr;
    private String mobileCsrSigned;
    private String mobileUUID;

    private String token;
    private String userUUID;

    public BrowserCertificationDto() { }

    public BrowserCertificationDto(OperationTypeDto operationType) {
        this.operation = operationType;
    }

    public BrowserCertificationDto(AddressDto address, String browserCsr, String token) {
        this.address = address;
        this.browserCsr = browserCsr;
        this.token = token;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserUUID() {
        return userUUID;
    }

    public BrowserCertificationDto setUserUUID(String userUUID) {
        this.userUUID = userUUID;
        return this;
    }

    public String getMobileCsr() {
        return mobileCsr;
    }

    public BrowserCertificationDto setMobileCsr(String mobileCsr) {
        this.mobileCsr = mobileCsr;
        return this;
    }

    public String getMobileCsrSigned() {
        return mobileCsrSigned;
    }

    public BrowserCertificationDto setMobileCsrSigned(String mobileCsrSigned) {
        this.mobileCsrSigned = mobileCsrSigned;
        return this;
    }

    public String getBrowserCsr() {
        return browserCsr;
    }

    public BrowserCertificationDto setBrowserCsr(String browserCsr) {
        this.browserCsr = browserCsr;
        return this;
    }

    public String getBrowserCsrSigned() {
        return browserCsrSigned;
    }

    public BrowserCertificationDto setBrowserCsrSigned(String browserCsrSigned) {
        this.browserCsrSigned = browserCsrSigned;
        return this;
    }

    public UserDto getUser() {
        return user;
    }

    public BrowserCertificationDto setUser(UserDto user) {
        this.user = user;
        return this;
    }

    public OperationTypeDto getOperation() {
        return operation;
    }

    public BrowserCertificationDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

    public String getSignerCertPEM() {
        return signerCertPEM;
    }

    public BrowserCertificationDto setSignerCertPEM(String signerCertPEM) {
        this.signerCertPEM = signerCertPEM;
        return this;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public BrowserCertificationDto setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getBrowserUUID() {
        return browserUUID;
    }

    public BrowserCertificationDto setBrowserUUID(String browserUUID) {
        this.browserUUID = browserUUID;
        return this;
    }

    public String getMobileUUID() {
        return mobileUUID;
    }

    public BrowserCertificationDto setMobileUUID(String mobileUUID) {
        this.mobileUUID = mobileUUID;
        return this;
    }

}