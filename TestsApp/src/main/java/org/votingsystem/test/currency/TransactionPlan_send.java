package org.votingsystem.test.currency;

import org.votingsystem.model.CurrencyServer;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.test.util.TransactionVSPlan;
import org.votingsystem.util.ContextVS;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TransactionPlan_send {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(BankVS_sendTransactionVS.class, new HashMap());
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        TransactionVSPlan transactionVSPlan = new TransactionVSPlan(
                TestUtils.getFileFromResources("transactionPlan.json"), currencyServer);
        Map bankVSCurrencyResultMap = transactionVSPlan.runBankVSTransactions("TEST_BANKVS_SEND_TRANSACTIONVS");
        Map groupVSCurrencyResultMap = transactionVSPlan.runGroupVSTransactions("TEST_GROUPVS_SEND_TRANSACTIONVS");
        TestUtils.finish("bankVSCurrencyResultMap: " + bankVSCurrencyResultMap + "\n groupVSCurrencyResultMap: " +
                groupVSCurrencyResultMap);
    }
}
