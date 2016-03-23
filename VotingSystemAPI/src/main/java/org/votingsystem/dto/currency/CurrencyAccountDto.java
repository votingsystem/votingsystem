package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.CurrencyCode;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.CurrencyAccount;

import java.math.BigDecimal;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyAccountDto {

    private Long id;
    private CurrencyCode currencyCode;
    private String IBAN;
    private BigDecimal amount;
    private Date lastUpdated;
    private TagVS tag;

    public CurrencyAccountDto() {}

    public CurrencyAccountDto(CurrencyAccount currencyAccount) {
        this.setId(currencyAccount.getId());
        this.setCurrencyCode(currencyAccount.getCurrencyCode());
        this.setIBAN(currencyAccount.getIBAN());
        this.setAmount(currencyAccount.getBalance());
        this.setLastUpdated(currencyAccount.getLastUpdated());
        this.setTag(currencyAccount.getTag());
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

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public TagVS getTag() {
        return tag;
    }

    public void setTag(TagVS tag) {
        this.tag = tag;
    }
}
