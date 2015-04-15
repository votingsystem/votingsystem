package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.UserVS;

import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyAccountsInfoDto {

    private UserVSDto userVS;
    private List<CurrencyAccountDto> accounts;

    public CurrencyAccountsInfoDto() {}

    public CurrencyAccountsInfoDto(List<CurrencyAccountDto> accounts, UserVS userVS) {
        this.setAccounts(accounts);
        this.setUserVS(UserVSDto.BASIC(userVS));
    }

    public UserVSDto getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVSDto userVS) {
        this.userVS = userVS;
    }

    public List<CurrencyAccountDto> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<CurrencyAccountDto> accounts) {
        this.accounts = accounts;
    }
}
