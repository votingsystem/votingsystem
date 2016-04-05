package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.model.CurrencyCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemAccountsDto {

    private Map<CurrencyCode, List<CurrencyAccountDto>> systemAccounts;
    private Map<CurrencyCode, List<TagVSDto>> userAccounts;
    private Map<CurrencyCode, List<TagVSDto>> bankInputs;

    public SystemAccountsDto() {}

    public SystemAccountsDto(List<CurrencyAccountDto> systemAccounts, List<TagVSDto> userAccounts,
                              List<TagVSDto> bankInputs) {
        this.systemAccounts = systemAccounts.stream().collect(Collectors.groupingBy(CurrencyAccountDto::getCurrencyCode));
        this.userAccounts = userAccounts.stream().collect(Collectors.groupingBy(TagVSDto::getCurrencyCode));
        this.bankInputs = bankInputs.stream().collect(Collectors.groupingBy(TagVSDto::getCurrencyCode));
    }

    public Map<CurrencyCode, List<CurrencyAccountDto>> getSystemAccounts() {
        return systemAccounts;
    }

    public Map<CurrencyCode, List<TagVSDto>> getUserAccounts() {
        return userAccounts;
    }

    public Map<CurrencyCode, List<TagVSDto>> getBankInputs() {
        return bankInputs;
    }

}
