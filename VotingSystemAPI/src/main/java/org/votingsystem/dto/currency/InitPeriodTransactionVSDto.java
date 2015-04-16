package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.TypeVS;

import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InitPeriodTransactionVSDto {

    private TypeVS operation;
    private BigDecimal amount;
    private BigDecimal timeLimitedNotExpended;
    private UserVSDto toUser;
    private String tag;
    private String UUID;


    public InitPeriodTransactionVSDto() {}

    public InitPeriodTransactionVSDto(BigDecimal amount, BigDecimal timeLimitedNotExpended, String tag, UserVS userVS) {
        this.setOperation(TypeVS.CURRENCY_INIT_PERIOD);
        this.setAmount(amount);
        this.setTimeLimitedNotExpended(timeLimitedNotExpended);
        this.setTag(tag);
        this.setToUser(UserVSDto.BASIC(userVS));
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

    public UserVSDto getToUser() {
        return toUser;
    }

    public void setToUser(UserVSDto toUser) {
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
}
