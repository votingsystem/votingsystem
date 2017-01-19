package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.util.CurrencyCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyCertExtensionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String currencyServerURL;
    private String hashCertVS;
    private CurrencyCode currencyCode;
    private String tag;
    private Boolean timeLimited;
    private BigDecimal amount;

    public CurrencyCertExtensionDto() {}

    public CurrencyCertExtensionDto(BigDecimal amount, CurrencyCode currencyCode, String hashCertVS, String currencyServerURL,
                    Boolean timeLimited, String tag) {
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.hashCertVS = hashCertVS;
        this.currencyServerURL = currencyServerURL;
        this.tag = tag;
        this.timeLimited = timeLimited;
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

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
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

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

}
