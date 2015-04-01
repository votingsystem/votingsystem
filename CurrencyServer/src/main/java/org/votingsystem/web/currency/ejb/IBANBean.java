package org.votingsystem.web.currency.ejb;

import org.iban4j.*;
import org.votingsystem.web.cdi.ConfigVS;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class IBANBean {

    @Inject ConfigVS config;

    private String bankCode = null;
    private String  branchCode = null;

    @PostConstruct
    public void initialize() throws Exception {
        bankCode = config.getProperties().getProperty("vs.IBAN_bankCode");
        branchCode = config.getProperties().getProperty("vs.IBAN_branchCode");
    }

    public String getIBAN(Long userId) {
        String accountNumberStr = String.format("%010d", userId);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode(bankCode).branchCode(branchCode)
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        return iban.toString();
    }

    public String getIBAN(Long userId, String bankCodeStr, String branchCodeStr) {
        String accountNumberStr = String.format("%010d", userId);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode(bankCodeStr).branchCode(branchCodeStr)
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        return iban.toString();
    }

    public String validateIBAN(String IBAN) throws IbanFormatException, InvalidCheckDigitException, UnsupportedCountryException {
        IbanUtil.validate(IBAN);
        return IBAN;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getBranchCode() {
        return branchCode;
    }
}
