package org.votingsystem.test.currency;

import org.votingsystem.model.CurrencyServer;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.test.util.TransactionVSPlan;
import org.votingsystem.util.ContextVS;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class GroupVS_sendTransactionVS {

    public static void main(String[] args) throws Exception {
        Map simulationDataMap = new HashMap<>();
        simulationDataMap.put("groupId", 5);
        simulationDataMap.put("serverURL", "http://currency:8086/CurrencyServer");
        Logger log = TestUtils.init(GroupVS_sendTransactionVS.class, simulationDataMap);
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        TransactionVSPlan transactionVSPlan = new TransactionVSPlan(
                TestUtils.getFileFromResources("transactionsPlan/groupVS.json"), currencyServer);
        Map currencyResultMap = transactionVSPlan.runGroupVSTransactions("TEST_GROUPVS_SEND_TRANSACTIONVS");
        TestUtils.finish("currencyResultMap: " + currencyResultMap);
    }

}

