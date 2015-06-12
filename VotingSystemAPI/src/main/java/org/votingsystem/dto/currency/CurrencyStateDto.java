package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.util.CertUtils;

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
    private String leftOverCert;
    private String currencyChangeCert;
    private Date dateCreated;

    public CurrencyStateDto() {}

    public CurrencyStateDto(Currency currency) {
        hashCertVS = currency.getHashCertVS();
        if(currency.getCurrencyBatch() != null) batchId = currency.getCurrencyBatch().getId();
        state = currency.getState();
        type = currency.getType();
        this.dateCreated = currency.getDateCreated();
    }

    public static CurrencyStateDto UNKNOWN() {
        CurrencyStateDto currencyStateDto = new CurrencyStateDto();
        currencyStateDto.state = Currency.State.UNKNOWN;
        return currencyStateDto;
    }

    @JsonIgnore
    public void setBatchResponseCerts(Collection<Currency> batchCurrencyList) throws Exception {
        for(Currency currency : batchCurrencyList) {
            if(currency.getType() == Currency.Type.LEFT_OVER && currency.getDateCreated().after(dateCreated)) {
                X509Certificate certX509 = CertUtils.loadCertificate(currency.getContent());
                leftOverCert = new String(CertUtils.getPEMEncoded (certX509));
            } else if(currency.getType() == Currency.Type.CHANGE && currency.getDateCreated().after(dateCreated)) {
                X509Certificate certX509 = CertUtils.loadCertificate(currency.getContent());
                currencyChangeCert = new String(CertUtils.getPEMEncoded (certX509));
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

    public void setState(Currency.State state) {
        this.state = state;
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
}
