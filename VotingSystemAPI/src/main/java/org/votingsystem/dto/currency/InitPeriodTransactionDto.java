package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.User;
import org.votingsystem.util.TypeVS;

import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InitPeriodTransactionDto {

    private TypeVS operation;
    private BigDecimal amount;
    private BigDecimal timeLimitedNotExpended;
    private UserDto toUser;
    private String tag;
    private String currencyCode;
    private String UUID;


    public InitPeriodTransactionDto() {}

    public InitPeriodTransactionDto(BigDecimal amount, BigDecimal timeLimitedNotExpended, String currencyCode,
                                    String tag, User user) {
        this.setOperation(TypeVS.CURRENCY_PERIOD_INIT);
        this.setAmount(amount);
        this.setTimeLimitedNotExpended(timeLimitedNotExpended);
        this.setCurrencyCode(currencyCode);
        this.setTag(tag);
        this.setToUser(UserDto.BASIC(user));
        this.setUUID(java.util.UUID.randomUUID().toString());
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
