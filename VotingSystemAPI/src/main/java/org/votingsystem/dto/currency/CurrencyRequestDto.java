package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.TagVS;
import org.votingsystem.util.TypeVS;
import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyRequestDto {

    private TypeVS operation = TypeVS.CURRENCY_REQUEST;
    private String subject;
    private String serverURL;
    private String currencyCode;
    private TagVS tagVS;
    private String UUID;
    private BigDecimal totalAmount;
    private Boolean isTimeLimited;

    public CurrencyRequestDto() {}

    public CurrencyRequestDto(String subject, BigDecimal totalAmount, String currencyCode, boolean isTimeLimited,
              String serverURL, TagVS tagVS) {
        this.subject = subject;
        this.serverURL = serverURL;
        this.totalAmount = totalAmount;
        this.currencyCode = currencyCode;
        this.isTimeLimited = isTimeLimited;
        this.tagVS = tagVS;
        this.UUID = java.util.UUID.randomUUID().toString();
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Boolean isTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public TagVS getTagVS() {
        return tagVS;
    }

    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
    }
}
