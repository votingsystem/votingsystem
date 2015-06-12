package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.util.TimePeriod;

import java.util.ArrayList;
import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeriodResultDto {

    private TimePeriod timePeriod;
    private BalancesDto systemBalance;
    private List<BalancesDto> groupVSBalanceList;
    private List<BalancesDto>  userVSBalanceList;
    private List<BalancesDto>  bankVSBalanceList;

    public PeriodResultDto() {}

    public static PeriodResultDto init(TimePeriod timePeriod) {
        PeriodResultDto periodResultDto = new PeriodResultDto();
        periodResultDto.setGroupVSBalanceList(new ArrayList<>());
        periodResultDto.setUserVSBalanceList(new ArrayList<>());
        periodResultDto.setBankVSBalanceList(new ArrayList<>());
        periodResultDto.setTimePeriod(timePeriod);
        return periodResultDto;
    }

    public List<BalancesDto> getGroupVSBalanceList() {
        return groupVSBalanceList;
    }

    public void setGroupVSBalanceList(List<BalancesDto> groupVSBalanceList) {
        this.groupVSBalanceList = groupVSBalanceList;
    }

    public List<BalancesDto> getUserVSBalanceList() {
        return userVSBalanceList;
    }

    public void setUserVSBalanceList(List<BalancesDto> userVSBalanceList) {
        this.userVSBalanceList = userVSBalanceList;
    }

    public List<BalancesDto> getBankVSBalanceList() {
        return bankVSBalanceList;
    }

    public void setBankVSBalanceList(List<BalancesDto> bankVSBalanceList) {
        this.bankVSBalanceList = bankVSBalanceList;
    }

    public void addBankVSBalance(BalancesDto balancesDto) {
        bankVSBalanceList.add(balancesDto);
    }

    public void addGroupVSBalance(BalancesDto balancesDto) {
        groupVSBalanceList.add(balancesDto);
    }

    public void addUserVSBalance(BalancesDto balancesDto) {
        userVSBalanceList.add(balancesDto);
    }

    public BalancesDto getSystemBalance() {
        return systemBalance;
    }

    public void setSystemBalance(BalancesDto systemBalance) {
        this.systemBalance = systemBalance;
    }

    public TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(TimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }
}
