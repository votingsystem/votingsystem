package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.model.currency.Transaction;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncomesDto {

    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal timeLimited = BigDecimal.ZERO;
    private String tag;

    public IncomesDto() {}

    public static IncomesDto ZERO() {
        return new IncomesDto(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public IncomesDto(BigDecimal total, BigDecimal timeLimited) {
        this.total = total;
        this.timeLimited = timeLimited;
        if(this.total == null) this.total = BigDecimal.ZERO;
        if(this.timeLimited == null) this.timeLimited = BigDecimal.ZERO;
    }

    public IncomesDto(Transaction transaction) {
        add(transaction);
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void addTotal(BigDecimal sum) {
        total = total.add(sum);
    }

    public BigDecimal getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(BigDecimal timeLimited) {
        this.timeLimited = timeLimited;
    }

    public void addTimeLimited(BigDecimal sum) {
        if(timeLimited == null)  timeLimited = sum;
        else timeLimited = timeLimited.add(sum);
    }

    public IncomesDto add(Transaction transaction) {
        if(transaction.getIsTimeLimited()) timeLimited = timeLimited.add(transaction.getAmount());
        total = total.add(transaction.getAmount());
        return this;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

}
