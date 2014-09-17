package org.votingsystem.vicket.util;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.iban4j.*;
import groovy.util.ConfigObject;
/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class IbanVSUtil {

    private static final IbanVSUtil INSTANCE = new IbanVSUtil();

    private String bankCode = null;
    private String  branchCode = null;

    private IbanVSUtil() {
        bankCode = (String) ((ConfigObject) ((GrailsApplication) ApplicationContextHolder.getBean("grailsApplication")).
                getConfig().getProperty("VotingSystem")).get("IBAN_bankCode");
        branchCode = (String) ((ConfigObject) ((GrailsApplication) ApplicationContextHolder.getBean("grailsApplication")).
                getConfig().getProperty("VotingSystem")).get("IBAN_branchCode");
    }

    public String getIBAN(Long userId) {
        String accountNumberStr = String.format("%010d", userId);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode(bankCode).branchCode(branchCode)
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        return iban.toString();
    }

    public static String getIBAN(Long userId, String bankCodeStr, String branchCodeStr) {
        String accountNumberStr = String.format("%010d", userId);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode(bankCodeStr).branchCode(branchCodeStr)
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        return iban.toString();
    }


    public static IbanVSUtil getInstance() {
        return INSTANCE;
    }

    public static void validate(String IBAN) throws IbanFormatException, InvalidCheckDigitException, UnsupportedCountryException {
        IbanUtil.validate(IBAN);
    }
}
