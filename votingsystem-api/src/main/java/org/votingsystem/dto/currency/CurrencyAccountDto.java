package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.util.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyAccountDto {

    private Long id;
    private CurrencyCode currencyCode;
    private String IBAN;
    private BigDecimal amount;
    private LocalDateTime lastUpdated;

    public CurrencyAccountDto() {}

    public CurrencyAccountDto(CurrencyAccount currencyAccount) {
        this.setId(currencyAccount.getId());
        this.setCurrencyCode(currencyAccount.getCurrencyCode());
        this.setIBAN(currencyAccount.getIBAN());
        this.setAmount(currencyAccount.getBalance());
        this.setLastUpdated(currencyAccount.getLastUpdated());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}
