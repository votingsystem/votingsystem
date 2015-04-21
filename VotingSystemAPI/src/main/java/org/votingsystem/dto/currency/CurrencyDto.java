package org.votingsystem.dto.currency;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyDto {

    private BigDecimal amount;
    private String currencyCode;
    private String hashCertVS;
    private String tag;
    private Boolean timeLimited;
    private String object;
    private String certificationRequest;


    public CurrencyDto() {}

    public CurrencyDto(Boolean timeLimited, String object) {
        this.timeLimited = timeLimited;
        this.object = object;
    }

    public static CurrencyDto serialize(Currency currency) throws Exception {
        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setAmount(currency.getAmount());
        currencyDto.setCurrencyCode(currency.getCurrencyCode());
        currencyDto.setHashCertVS(currency.getHashCertVS());
        currencyDto.setTag(currency.getTag().getName());
        currencyDto.setTimeLimited(currency.getIsTimeLimited());
        currencyDto.setObject(ObjectUtils.serializeObjectToString(currency));
        if(currency.getCertificationRequest() != null)
            currencyDto.setCertificationRequest(new String(currency.getCertificationRequest().getCsrPEM()));
        return currencyDto;
    }


    public static Set<CurrencyDto> serializeCollection(Collection<Currency> currencyCollection) throws Exception {
        Set<CurrencyDto> result = new HashSet<>();
        for(Currency currency : currencyCollection) {
            result.add(CurrencyDto.serialize(currency));
        }
        return result;
    }

    public Currency deSerialize() throws Exception {
        if(certificationRequest != null) {
            CertificationRequestVS certificationRequestVS = (CertificationRequestVS) ObjectUtils.deSerializeObject(
                    certificationRequest.getBytes());
            return Currency.fromCertificationRequestVS(certificationRequestVS);
        } else {
            return (Currency) ObjectUtils.deSerializeObject(object.getBytes());
        }
    }

    public static Set<Currency> deSerializeCollection(Collection<CurrencyDto> currencyCollection) throws Exception {
        Set<Currency> result = new HashSet<>();
        for(CurrencyDto currencyDto : currencyCollection) {
            result.add(currencyDto.deSerialize());
        }
        return result;
    }

    public static Set<Currency> getCurrencySet(Collection<CurrencyDto> currencyDtoCollection) throws Exception {
        Set<Currency> currencySet = new HashSet<>();
        for(CurrencyDto currencyDto : currencyDtoCollection) {
            currencySet.add(currencyDto.deSerialize());
        }
        return currencySet;
    }



    public Boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public String getCertificationRequest() {
        return certificationRequest;
    }

    public void setCertificationRequest(String certificationRequest) {
        this.certificationRequest = certificationRequest;
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
