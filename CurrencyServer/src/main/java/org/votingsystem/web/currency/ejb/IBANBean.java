package org.votingsystem.web.currency.ejb;

import org.iban4j.*;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.web.cdi.ConfigVS;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class IBANBean {

    private static Logger log = Logger.getLogger(IBANBean.class.getName());

    @Inject ConfigVS config;

    private String bankCode = null;
    private String  branchCode = null;

    @PostConstruct
    public void initialize() {
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
