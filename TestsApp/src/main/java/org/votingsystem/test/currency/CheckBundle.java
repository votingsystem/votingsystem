package org.votingsystem.test.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class CheckBundle {

    private static Logger log =  Logger.getLogger(CheckBundle.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer();
        Set<String> hashCertSet = Sets.newHashSet("2dZCSiH8prnd731kyUEGzhoPF0OwA23vFvG+RYOcR5o=");
        ResponseVS responseVS = HttpHelper.getInstance().sendData(JSON.getMapper().writeValueAsBytes(hashCertSet),
                ContentType.JSON, currencyServer.getCurrencyBundleStateServiceURL());
        log.info("mayBeJSON: " + responseVS.getMessage().trim());
        Map<String, CurrencyStateDto> result = (Map<String, CurrencyStateDto>) responseVS.getMessage(
                new TypeReference<Map<String, CurrencyStateDto>>() {});
        log.info("result: " + result.toString());
        System.exit(0);
    }

}