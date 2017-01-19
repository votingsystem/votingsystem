package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.Interval;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashBoardDto {

    private Interval timePeriod;
    private Transaction.Type type;
    private Long numTransFromBank;
    private Long numTransFromUser;
    private Long numTransCurrencyInitPeriod;
    private Long numTransCurrencyInitPeriodTimeLimited;
    private Long numTransCurrencyRequest;
    private Long numTransCurrencySend;
    private Long numTransCurrencyChange;
    private Long numTransCancellation;

    public DashBoardDto() {}

    public DashBoardDto(Interval timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Interval getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(Interval timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Transaction.Type getType() {
        return type;
    }

    public void setType(Transaction.Type type) {
        this.type = type;
    }

    public Long getNumTransFromBank() {
        return numTransFromBank;
    }

    public void setNumTransFromBank(Long numTransFromBank) {
        this.numTransFromBank = numTransFromBank;
    }

    public Long getNumTransFromUser() {
        return numTransFromUser;
    }

    public void setNumTransFromUser(Long numTransFromUser) {
        this.numTransFromUser = numTransFromUser;
    }

    public Long getNumTransCurrencyInitPeriod() {
        return numTransCurrencyInitPeriod;
    }

    public void setNumTransCurrencyInitPeriod(Long numTransCurrencyInitPeriod) {
        this.numTransCurrencyInitPeriod = numTransCurrencyInitPeriod;
    }

    public Long getNumTransCurrencyInitPeriodTimeLimited() {
        return numTransCurrencyInitPeriodTimeLimited;
    }

    public void setNumTransCurrencyInitPeriodTimeLimited(Long numTransCurrencyInitPeriodTimeLimited) {
        this.numTransCurrencyInitPeriodTimeLimited = numTransCurrencyInitPeriodTimeLimited;
    }

    public Long getNumTransCurrencyRequest() {
        return numTransCurrencyRequest;
    }

    public void setNumTransCurrencyRequest(Long numTransCurrencyRequest) {
        this.numTransCurrencyRequest = numTransCurrencyRequest;
    }

    public Long getNumTransCurrencySend() {
        return numTransCurrencySend;
    }

    public void setNumTransCurrencySend(Long numTransCurrencySend) {
        this.numTransCurrencySend = numTransCurrencySend;
    }

    public Long getNumTransCancellation() {
        return numTransCancellation;
    }

    public void setNumTransCancellation(Long numTransCancellation) {
        this.numTransCancellation = numTransCancellation;
    }

    public Long getNumTransCurrencyChange() {
        return numTransCurrencyChange;
    }

    public void setNumTransCurrencyChange(Long numTransCurrencyChange) {
        this.numTransCurrencyChange = numTransCurrencyChange;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransFromGroup {
        private Long numTrans;
        private Long numUsers;

        public TransFromGroup() {}

        public TransFromGroup(Long numTrans, Long numUsers) {
            this.numTrans = numTrans;
            this.numUsers = numUsers;
        }

        public Long getNumTrans() {
            return numTrans;
        }

        public void setNumTrans(Long numTrans) {
            this.numTrans = numTrans;
        }

        public Long getNumUsers() {
            return numUsers;
        }

        public void setNumUsers(Long numUsers) {
            this.numUsers = numUsers;
        }
    }


}
