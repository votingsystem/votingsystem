package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.util.Interval;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashBoardDto {

    private Interval timePeriod;
    private TransactionVS.Type type;
    private Long numTransFromBankVS;
    private Long numTransFromUserVS;
    private Long numTransCurrencyInitPeriod;
    private Long numTransCurrencyInitPeriodTimeLimited;
    private Long numTransCurrencyRequest;
    private Long numTransCurrencySend;
    private Long numTransCurrencyChange;
    private Long numTransCancellation;

    private TransFromGroupVS transFromGroupVSToMemberGroup;
    private TransFromGroupVS transFromGroupVSToAllMembers;

    public DashBoardDto () {}

    public DashBoardDto(Interval timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Interval getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(Interval timePeriod) {
        this.timePeriod = timePeriod;
    }

    public TransactionVS.Type getType() {
        return type;
    }

    public void setType(TransactionVS.Type type) {
        this.type = type;
    }

    public Long getNumTransFromBankVS() {
        return numTransFromBankVS;
    }

    public void setNumTransFromBankVS(Long numTransFromBankVS) {
        this.numTransFromBankVS = numTransFromBankVS;
    }

    public Long getNumTransFromUserVS() {
        return numTransFromUserVS;
    }

    public void setNumTransFromUserVS(Long numTransFromUserVS) {
        this.numTransFromUserVS = numTransFromUserVS;
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

    public TransFromGroupVS getTransFromGroupVSToMemberGroup() {
        return transFromGroupVSToMemberGroup;
    }

    public void setTransFromGroupVSToMemberGroup(TransFromGroupVS transFromGroupVSToMemberGroup) {
        this.transFromGroupVSToMemberGroup = transFromGroupVSToMemberGroup;
    }

    public TransFromGroupVS getTransFromGroupVSToAllMembers() {
        return transFromGroupVSToAllMembers;
    }

    public void setTransFromGroupVSToAllMembers(TransFromGroupVS transFromGroupVSToAllMembers) {
        this.transFromGroupVSToAllMembers = transFromGroupVSToAllMembers;
    }

    public Long getNumTransCurrencyChange() {
        return numTransCurrencyChange;
    }

    public void setNumTransCurrencyChange(Long numTransCurrencyChange) {
        this.numTransCurrencyChange = numTransCurrencyChange;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransFromGroupVS {
        private Long numTrans;
        private Long numUsers;

        public TransFromGroupVS() {}

        public TransFromGroupVS(Long numTrans, Long numUsers) {
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
