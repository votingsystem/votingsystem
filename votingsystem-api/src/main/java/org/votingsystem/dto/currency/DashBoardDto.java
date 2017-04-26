package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.Interval;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashBoardDto {

    private Interval timePeriod;
    private CurrencyOperation type;
    private Long numTransFromBank;
    private Long numTransFromUser;
    private Long numTransCurrencyRequest;
    private Long numTransCurrencySend;
    private Long numTransCurrencyChange;

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

    public CurrencyOperation getType() {
        return type;
    }

    public void setType(CurrencyOperation type) {
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
