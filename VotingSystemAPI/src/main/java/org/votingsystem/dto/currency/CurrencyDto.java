package org.votingsystem.dto.currency;


import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.ObjectUtils;

public class CurrencyDto {

    private Boolean timeLimited;
    private String object;
    private String hashCertVS;
    private String certificationRequest;

    public CurrencyDto() {}

    public CurrencyDto(Currency currency) {
        timeLimited = currency.getIsTimeLimited();
        object = ObjectUtils.serializeObjectToString(currency);
        hashCertVS = currency.getHashCertVS();
    }

    public CurrencyDto(Boolean timeLimited, String object) {
        this.timeLimited = timeLimited;
        this.object = object;
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
}
