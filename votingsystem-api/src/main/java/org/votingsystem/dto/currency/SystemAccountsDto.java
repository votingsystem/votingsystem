package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.util.CurrencyCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemAccountsDto {

    private Map<CurrencyCode, List<CurrencyAccountDto>> systemAccounts;
    private Map<CurrencyCode, List<TagDto>> userAccounts;
    private Map<CurrencyCode, List<TagDto>> bankInputs;

    public SystemAccountsDto() {}

    public SystemAccountsDto(List<CurrencyAccountDto> systemAccounts, List<TagDto> userAccounts,
                              List<TagDto> bankInputs) {
        this.systemAccounts = systemAccounts.stream().collect(Collectors.groupingBy(CurrencyAccountDto::getCurrencyCode));
        this.userAccounts = userAccounts.stream().collect(Collectors.groupingBy(TagDto::getCurrencyCode));
        this.bankInputs = bankInputs.stream().collect(Collectors.groupingBy(TagDto::getCurrencyCode));
    }

    public Map<CurrencyCode, List<CurrencyAccountDto>> getSystemAccounts() {
        return systemAccounts;
    }

    public Map<CurrencyCode, List<TagDto>> getUserAccounts() {
        return userAccounts;
    }

    public Map<CurrencyCode, List<TagDto>> getBankInputs() {
        return bankInputs;
    }

}
