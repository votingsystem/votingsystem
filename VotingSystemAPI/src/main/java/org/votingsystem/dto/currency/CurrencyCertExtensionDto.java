package org.votingsystem.dto.currency;

import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyCertExtensionDto {

    private String currencyServerURL;
    private String hashCertVS;
    private String currencyCode;
    private String tag;
    private BigDecimal amount;

    public CurrencyCertExtensionDto() {}

    public CurrencyCertExtensionDto(BigDecimal amount, String currencyCode, String hashCertVS, String currencyServerURL,
                                    String tag) {
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.hashCertVS = hashCertVS;
        this.currencyServerURL = currencyServerURL;
        this.tag = tag;
    }

    public String getCurrencyServerURL() {
        return currencyServerURL;
    }

    public void setCurrencyServerURL(String currencyServerURL) {
        this.currencyServerURL = currencyServerURL;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
