package org.votingsystem.cooin.util;

import groovy.util.ConfigObject;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.iban4j.*;
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
                getConfig().getProperty("vs")).get("IBAN_bankCode");
        branchCode = (String) ((ConfigObject) ((GrailsApplication) ApplicationContextHolder.getBean("grailsApplication")).
                getConfig().getProperty("vs")).get("IBAN_branchCode");
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

    public static String validate(String IBAN) throws IbanFormatException, InvalidCheckDigitException, UnsupportedCountryException {
        IbanUtil.validate(IBAN);
        return IBAN;
    }
}