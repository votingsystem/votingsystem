package org.currency.test;

import org.votingsystem.http.HttpConn;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.MediaType;
import org.votingsystem.util.OperationType;
import org.votingsystem.testlib.BaseTest;
import java.util.logging.Logger;


public class Test extends BaseTest {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        new Test().testHttp();
        System.exit(0);
    }

    public void testHttp() throws Exception {
        ResponseDto response = HttpConn.getInstance().doGetRequest(
                OperationType.GET_METADATA.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID), MediaType.XML);
        log.info("Message: " + response.getMessage());
    }

}
