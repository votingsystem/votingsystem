package org.currency.test;

import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.NifUtils;

import java.util.logging.Logger;


public class Test extends BaseTest {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        new Test().test();
        System.exit(0);
    }

    public void test() throws Exception {
        log.info(NifUtils.getNif(999999999));
    }

}
