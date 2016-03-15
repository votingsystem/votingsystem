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
    private List<TagVSDto> bankBalanceList;

    public SystemAccountsDto() {}

    public SystemAccountsDto( List<CurrencyAccountDto> accountList, List<TagVSDto> tagVSBalanceList,
                              List<TagVSDto> bankBalanceList) {
        this.accountList = accountList;
        this.tagVSBalanceList = tagVSBalanceList;
        this.bankBalanceList = bankBalanceList;
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

    public List<TagVSDto> getBankBalanceList() {
        return bankBalanceList;
    }

    public void setBankBalanceList(List<TagVSDto> bankBalanceList) {
        this.bankBalanceList = bankBalanceList;
    }

}
