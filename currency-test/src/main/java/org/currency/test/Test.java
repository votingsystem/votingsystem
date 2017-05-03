package org.currency.test;

import com.fasterxml.jackson.core.type.TypeReference;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.NifUtils;
import org.votingsystem.xml.XML;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;


public class Test extends BaseTest {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        new Test().test();
        System.exit(0);
    }

    public void test() throws Exception {
        Iban iban = new Iban.Builder()
                .countryCode(CountryCode.AT).bankCode("88888").accountNumber("00000010101").build();
        log.info(iban.toString());
    }



}
