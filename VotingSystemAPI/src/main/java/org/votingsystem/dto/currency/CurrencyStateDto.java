package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;

import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyStateDto {

    private String hashCertVS;
    private Long batchId;
    private Currency.State state;
    private Currency.Type type;
    private String currencyCert;
    private String currencyCode;
    private String leftOverCert;
    private String tag;
    private BigDecimal amount;
    private String currencyChangeCert;
    private Date dateCreated;

    public CurrencyStateDto() {}

    public CurrencyStateDto(String hashCertVS, Currency.State state) {
        this.hashCertVS = hashCertVS;
        this.state = state;
    }

    public CurrencyStateDto(Currency currency) throws Exception {
        hashCertVS = currency.getHashCertVS();
        if(currency.getCurrencyBatch() != null) batchId = currency.getCurrencyBatch().getId();
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
        tag = currency.getTagVS().getName();
        this.dateCreated = currency.getDateCreated();
    }

    @JsonIgnore
    public void setBatchResponseCerts(Collection<Currency> batchCurrencyList) throws Exception {
        for(Currency currency : batchCurrencyList) {
            if(currency.getType() == Currency.Type.LEFT_OVER && currency.getDateCreated().after(dateCreated)) {
                X509Certificate certX509 = CertUtils.loadCertificate(currency.getContent());
                leftOverCert = new String(PEMUtils.getPEMEncoded (certX509));
            } else if(currency.getType() == Currency.Type.CHANGE && currency.getDateCreated().after(dateCreated)) {
                X509Certificate certX509 = CertUtils.loadCertificate(currency.getContent());
                currencyChangeCert = new String(PEMUtils.getPEMEncoded (certX509));
            }
        }
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
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

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getCurrencyCert() {
        return currencyCert;
    }

    public void setCurrencyCert(String currencyCert) {
        this.currencyCert = currencyCert;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
