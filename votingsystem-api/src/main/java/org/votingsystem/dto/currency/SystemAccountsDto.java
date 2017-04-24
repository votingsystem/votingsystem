package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.util.CurrencyCode;

import java.math.BigDecimal;
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
    private Map<CurrencyCode, BigDecimal> userAccounts;
    private Map<CurrencyCode, BigDecimal> bankInputs;

    public SystemAccountsDto() {}

    public SystemAccountsDto(List<CurrencyAccountDto> systemAccounts,  Map<CurrencyCode, BigDecimal> userAccounts,
                             Map<CurrencyCode, BigDecimal> bankInputs) {
        this.systemAccounts = systemAccounts.stream().collect(Collectors.groupingBy(CurrencyAccountDto::getCurrencyCode));
        this.userAccounts = userAccounts;
        this.bankInputs = bankInputs;
    }

    public Map<CurrencyCode, List<CurrencyAccountDto>> getSystemAccounts() {
        return systemAccounts;
    }


    public Map<CurrencyCode, BigDecimal> getUserAccounts() {
        return userAccounts;
    }

    public void setUserAccounts(Map<CurrencyCode, BigDecimal> userAccounts) {
        this.userAccounts = userAccounts;
    }

    public Map<CurrencyCode, BigDecimal> getBankInputs() {
        return bankInputs;
    }

    public void setBankInputs(Map<CurrencyCode, BigDecimal> bankInputs) {
        this.bankInputs = bankInputs;
    }
}
