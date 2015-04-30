package org.votingsystem.test.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.util.*;
import java.util.logging.Logger;

public class CheckBundle {

    private static Logger log =  Logger.getLogger(CheckBundle.class.getName());

    public static void main(String[] args) throws Exception {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        Set<String> hashCertSet = new HashSet<>(Arrays.asList("g8SAcWHXZ4GeZGyIciedc6zBPoXyS1HFDUCvhw8W5h0="));
        List resquestArray = new ArrayList<>();
        resquestArray.addAll(hashCertSet);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(resquestArray.toString().getBytes(),
                ContentTypeVS.JSON, currencyServer.getCurrencyBundleStateServiceURL());
        log.info("mayBeJSON: " + responseVS.getMessage().trim());
        Map<String, Currency.State> result = (Map<String, Currency.State>) responseVS.getMessage(
                new TypeReference<Map<String, Currency.State>>() {});
        log.info("result: " + result.toString());
        System.exit(0);
    }

}