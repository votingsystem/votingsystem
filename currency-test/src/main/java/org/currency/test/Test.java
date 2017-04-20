package org.currency.test;

import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyOperation;

import java.util.logging.Logger;


public class Test extends BaseTest {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        new Test().test();
        System.exit(0);
    }

    public void test() throws Exception {
        Bank bank = User.FROM_CERT(Bank.class, null, null);
        log.info("bank: " + bank.getClass().getSimpleName());
    }

}
