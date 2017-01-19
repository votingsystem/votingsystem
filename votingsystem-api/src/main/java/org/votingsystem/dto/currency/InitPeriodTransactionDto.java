package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.User;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;

import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitPeriodTransactionDto {

    private CurrencyOperation operation;
    private BigDecimal amount;
    private BigDecimal timeLimitedNotExpended;
    private UserDto toUser;
    private String tag;
    private CurrencyCode currencyCode;
    private String UUID;


    public InitPeriodTransactionDto() {}

    public InitPeriodTransactionDto(BigDecimal amount, BigDecimal timeLimitedNotExpended, CurrencyCode currencyCode,
                                    String tag, User user) {
        this.setOperation(CurrencyOperation.CURRENCY_PERIOD_INIT);
        this.setAmount(amount);
        this.setTimeLimitedNotExpended(timeLimitedNotExpended);
        this.setCurrencyCode(currencyCode);
        this.setTag(tag);
        this.setToUser(UserDto.BASIC(user));
        this.setUUID(java.util.UUID.randomUUID().toString());
    }

    public CurrencyOperation getOperation() {
        return operation;
    }

    public void setOperation(CurrencyOperation operation) {
        this.operation = operation;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTimeLimitedNotExpended() {
        return timeLimitedNotExpended;
    }

    public void setTimeLimitedNotExpended(BigDecimal timeLimitedNotExpended) {
        this.timeLimitedNotExpended = timeLimitedNotExpended;
    }

    public UserDto getToUser() {
        return toUser;
    }

    public void setToUser(UserDto toUser) {
        this.toUser = toUser;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }
}
