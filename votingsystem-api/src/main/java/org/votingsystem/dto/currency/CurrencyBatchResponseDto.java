package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyBatchResponseDto {

    private String leftOverCert;
    private String currencyChangeCert;
    private byte[] receipt;
    private String message;

    public CurrencyBatchResponseDto() {};

    public CurrencyBatchResponseDto(byte[] receipt, String leftOverCert) throws Exception {
        this.receipt = receipt;
        this.leftOverCert = leftOverCert;
    }

    public CurrencyBatchResponseDto(byte[] receipt, String leftOverCert, String currencyChangeCert) throws Exception {
        this(receipt, leftOverCert);
        this.currencyChangeCert = currencyChangeCert;
    }

    public byte[] getReceipt() {
        return receipt;
    }

    public CurrencyBatchResponseDto setReceipt(byte[] receipt) {
        this.receipt = receipt;
        return this;
    }

    public String getLeftOverCert() {
        return leftOverCert;
    }

    public CurrencyBatchResponseDto setLeftOverCert(String leftOverCert) {
        this.leftOverCert = leftOverCert;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public CurrencyBatchResponseDto setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getCurrencyChangeCert() {
        return currencyChangeCert;
    }

    public CurrencyBatchResponseDto setCurrencyChangeCert(String currencyChangeCert) {
        this.currencyChangeCert = currencyChangeCert;
        return this;
    }

}