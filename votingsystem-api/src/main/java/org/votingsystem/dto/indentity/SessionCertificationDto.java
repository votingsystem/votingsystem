package org.votingsystem.dto.indentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SessionCertificationDto {

    @JacksonXmlProperty(localName = "Operation")
    private OperationTypeDto operation;

    private Integer statusCode;

    private AddressDto address;
    private UserDto user;
    private String signerCertPEM;

    private String browserCsr;
    private String browserCertificate;
    private String browserUUID;

    private String mobileCsr;
    private String mobileCertificate;
    private String mobileUUID;

    private String privateKeyPEM;


    public SessionCertificationDto() { }

    public SessionCertificationDto(UserDto user, String mobileCsr,  String mobileUUID,
                                   String browserCsr, String browserUUID) {
        this.user = user;
        this.mobileCsr = mobileCsr;
        this.mobileUUID = mobileUUID;
        this.browserCsr = browserCsr;
        this.browserUUID = browserUUID;
    }

    public SessionCertificationDto(OperationTypeDto operationType) {
        this.operation = operationType;
    }

    public SessionCertificationDto(AddressDto address, String browserCsr) {
        this.address = address;
        this.browserCsr = browserCsr;
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

    public SessionCertificationDto setMobileCsr(String mobileCsr) {
        this.mobileCsr = mobileCsr;
        return this;
    }

    public String getMobileCertificate() {
        return mobileCertificate;
    }

    public SessionCertificationDto setMobileCertificate(String mobileCertificate) {
        this.mobileCertificate = mobileCertificate;
        return this;
    }

    public String getBrowserCsr() {
        return browserCsr;
    }

    public SessionCertificationDto setBrowserCsr(String browserCsr) {
        this.browserCsr = browserCsr;
        return this;
    }

    public String getBrowserCertificate() {
        return browserCertificate;
    }

    public SessionCertificationDto setBrowserCertificate(String browserCertificate) {
        this.browserCertificate = browserCertificate;
        return this;
    }

    public UserDto getUser() {
        return user;
    }

    public SessionCertificationDto setUser(UserDto user) {
        this.user = user;
        return this;
    }

    public OperationTypeDto getOperation() {
        return operation;
    }

    public SessionCertificationDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

    public String getSignerCertPEM() {
        return signerCertPEM;
    }

    public SessionCertificationDto setSignerCertPEM(String signerCertPEM) {
        this.signerCertPEM = signerCertPEM;
        return this;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public SessionCertificationDto setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getBrowserUUID() {
        return browserUUID;
    }

    public SessionCertificationDto setBrowserUUID(String browserUUID) {
        this.browserUUID = browserUUID;
        return this;
    }

    public String getMobileUUID() {
        return mobileUUID;
    }

    public SessionCertificationDto setMobileUUID(String mobileUUID) {
        this.mobileUUID = mobileUUID;
        return this;
    }

    public String getPrivateKeyPEM() {
        return privateKeyPEM;
    }

    public SessionCertificationDto setPrivateKeyPEM(String privateKeyPEM) {
        this.privateKeyPEM = privateKeyPEM;
        return this;
    }

}