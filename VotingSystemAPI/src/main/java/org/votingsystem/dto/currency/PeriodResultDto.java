package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.util.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeriodResultDto {

    private Interval timePeriod;
    private BalancesDto systemBalance;
    private List<BalancesDto> userBalanceList;
    private List<BalancesDto> bankBalanceList;

    public PeriodResultDto() {}

    public PeriodResultDto(Interval timePeriod) {
        userBalanceList = new ArrayList<>();
        bankBalanceList = new ArrayList<>();
        this.timePeriod = timePeriod;
    }

    public List<BalancesDto> getUserBalanceList() {
        return userBalanceList;
    }

    public void setUserBalanceList(List<BalancesDto> userBalanceList) {
        this.userBalanceList = userBalanceList;
    }

    public List<BalancesDto> getBankBalanceList() {
        return bankBalanceList;
    }

    public void setBankBalanceList(List<BalancesDto> bankBalanceList) {
        this.bankBalanceList = bankBalanceList;
    }

    public void addBankBalance(BalancesDto balancesDto) {
        bankBalanceList.add(balancesDto);
    }

    public void addUserBalance(BalancesDto balancesDto) {
        userBalanceList.add(balancesDto);
    }

    public BalancesDto getSystemBalance() {
        return systemBalance;
    }

    public void setSystemBalance(BalancesDto systemBalance) {
        this.systemBalance = systemBalance;
    }

    public Interval getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(Interval timePeriod) {
        this.timePeriod = timePeriod;
    }

}
