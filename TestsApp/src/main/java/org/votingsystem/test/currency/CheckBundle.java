package org.votingsystem.test.currency;

import org.votingsystem.model.CurrencyServer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.util.*;
import java.util.logging.Logger;

public class CheckBundle {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(Test.class);
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        Set<String> hashCertSet = new HashSet<>(Arrays.asList("g8SAcWHXZ4GeZGyIciedc6zBPoXyS1HFDUCvhw8W5h0="));
        List resquestArray = new ArrayList<>();
        resquestArray.addAll(hashCertSet);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(resquestArray.toString().getBytes(),
                ContentTypeVS.JSON, currencyServer.getCurrencyBundleStateServiceURL());
        log.info("mayBeJSON: " + responseVS.getMessage().trim());
        Map result = responseVS.getMessageMap();
        log.info("result: " + result.toString());
        System.exit(0);
    }

}

