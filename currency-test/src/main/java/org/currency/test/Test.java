package org.currency.test;

import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.OperationType;

import java.util.Locale;
import java.util.logging.Logger;


public class Test extends BaseTest {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        new Test().test();
        System.exit(0);
    }

    public void test() throws Exception {
        ResponseDto response = HttpConn.getInstance().doPostRequest("test".getBytes(), null,
                CurrencyOperation.REGISTER_DEVICE.getUrl(Constants.ID_PROVIDER_ENTITY_ID));
        log.info("Message: " + response.getMessage());
    }

}
