package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.CurrencyCode;

import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyStateDto {

    private String revocationHash;
    private String batchUUID;
    private Currency.State state;
    private Currency.Type type;
    private String currencyCert;
    private CurrencyCode currencyCode;
    private String leftOverCert;
    private BigDecimal amount;
    private String currencyChangeCert;
    private ZonedDateTime dateCreated;

    public CurrencyStateDto() {}

    public CurrencyStateDto(String revocationHash, Currency.State state) {
        this.revocationHash = revocationHash;
        this.state = state;
    }

    public CurrencyStateDto(Currency currency) throws Exception {
        revocationHash = currency.getRevocationHash();
        if(currency.getCurrencyBatch() != null)
            batchUUID = currency.getCurrencyBatch().getBatchUUID();
        if(currency.getContent() != null) {
            X509Certificate certX509 = CertUtils.loadCertificate(currency.getContent());
            currencyCert = new String(PEMUtils.getPEMEncoded (certX509));
        } else if(currency.getX509AnonymousCert() != null) {
            currencyCert = new String(PEMUtils.getPEMEncoded (currency.getX509AnonymousCert()));
        }
        setAmount(currency.getAmount());
        currencyCode = currency.getCurrencyCode();
        state = currency.getState();
        type = currency.getType();
        this.dateCreated = ZonedDateTime.of(currency.getDateCreated(), ZoneId.systemDefault());
    }

    @JsonIgnore
    public void setBatchResponseCerts(Collection<Currency> batchCurrencyList) throws Exception {
        for(Currency currency : batchCurrencyList) {
            if(currency.getType() == Currency.Type.LEFT_OVER && currency.getDateCreated().isAfter(
                    dateCreated.toLocalDateTime())) {
                X509Certificate certX509 = CertUtils.loadCertificate(currency.getContent());
                leftOverCert = new String(PEMUtils.getPEMEncoded (certX509));
            } else if(currency.getType() == Currency.Type.CHANGE && currency.getDateCreated().isAfter(
                    dateCreated.toLocalDateTime())) {
                X509Certificate certX509 = CertUtils.loadCertificate(currency.getContent());
                currencyChangeCert = new String(PEMUtils.getPEMEncoded (certX509));
            }
        }
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public Currency.State getState() {
        return state;
    }

    public CurrencyStateDto setState(Currency.State state) {
        this.state = state;
        return this;
    }

    public Currency.Type getType() {
        return type;
    }

    public void setType(Currency.Type type) {
        this.type = type;
    }

    public String getLeftOverCert() {
        return leftOverCert;
    }

    public void setLeftOverCert(String leftOverCert) {
        this.leftOverCert = leftOverCert;
    }

    public String getCurrencyChangeCert() {
        return currencyChangeCert;
    }

    public void setCurrencyChangeCert(String currencyChangeCert) {
        this.currencyChangeCert = currencyChangeCert;
    }

    public ZonedDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(ZonedDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getCurrencyCert() {
        return currencyCert;
    }

    public void setCurrencyCert(String currencyCert) {
        this.currencyCert = currencyCert;
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

    public String getBatchUUID() {
        return batchUUID;
    }

    public CurrencyStateDto setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
        return this;
    }

}
