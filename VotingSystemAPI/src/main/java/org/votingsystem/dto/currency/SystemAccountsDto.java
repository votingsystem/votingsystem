package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.TagVSDto;

import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemAccountsDto {

    private List<CurrencyAccountDto> accountList;
    private List<TagVSDto> tagVSBalanceList;
    private List<TagVSDto> bankVSBalanceList;

    public SystemAccountsDto() {}

    public SystemAccountsDto( List<CurrencyAccountDto> accountList, List<TagVSDto> tagVSBalanceList,
                              List<TagVSDto> bankVSBalanceList) {
        this.accountList = accountList;
        this.tagVSBalanceList = tagVSBalanceList;
        this.bankVSBalanceList = bankVSBalanceList;
    }


    public List<CurrencyAccountDto> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<CurrencyAccountDto> accountList) {
        this.accountList = accountList;
    }

    public List<TagVSDto> getTagVSBalanceList() {
        return tagVSBalanceList;
    }

    public void setTagVSBalanceList(List<TagVSDto> tagVSBalanceList) {
        this.tagVSBalanceList = tagVSBalanceList;
    }

    public List<TagVSDto> getBankVSBalanceList() {
        return bankVSBalanceList;
    }

    public void setBankVSBalanceList(List<TagVSDto> bankVSBalanceList) {
        this.bankVSBalanceList = bankVSBalanceList;
    }

}
