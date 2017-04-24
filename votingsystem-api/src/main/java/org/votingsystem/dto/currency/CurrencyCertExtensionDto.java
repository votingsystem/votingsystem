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

    private String currencyEntity;
    private String revocationHash;
    private CurrencyCode currencyCode;
    private BigDecimal amount;

    public CurrencyCertExtensionDto() {}

    public CurrencyCertExtensionDto(BigDecimal amount, CurrencyCode currencyCode, String revocationHash,
                                    String currencyEntity) {
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.revocationHash = revocationHash;
        this.currencyEntity = currencyEntity;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyEntity() {
        return currencyEntity;
    }

    public void setCurrencyEntity(String currencyEntity) {
        this.currencyEntity = currencyEntity;
    }

}
