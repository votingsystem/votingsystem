package org.currency.test.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import org.currency.test.Constants;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class CheckBundleTest extends BaseTest {

    private static Logger log =  Logger.getLogger(CheckBundleTest.class.getName());

    public static void main(String[] args) throws Exception {
        new CheckBundleTest().checkBundle();
        System.exit(0);
    }

    private void checkBundle() throws Exception {
        Set<String> revocationHashSet = Sets.newHashSet("2dZCSiH8prnd731kyUEGzhoPF0OwA23vFvG+RYOcR5o=");
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(JSON.getMapper().writeValueAsBytes(revocationHashSet),
                MediaType.JSON, CurrencyOperation.BUNDLE_STATE.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));
        log.info("response: " + responseDto.getMessage().trim());
        Map<String, CurrencyStateDto> result = (Map<String, CurrencyStateDto>) responseDto.getMessage(
                new TypeReference<Map<String, CurrencyStateDto>>() {});
        log.info("result: " + result.toString());
    }

}