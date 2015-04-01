package org.votingsystem.test.currency;

import org.votingsystem.model.CurrencyServer;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.test.util.TransactionVSPlan;
import org.votingsystem.util.ContextVS;

import java.util.Map;
import java.util.logging.Logger;


public class BankVS_sendTransactionVS {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(BankVS_sendTransactionVS.class);
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        TransactionVSPlan transactionVSPlan = new TransactionVSPlan(
                TestUtils.getFileFromResources("transactionsPlan/bankVS.json"), currencyServer);
        Map currencyResultMap = transactionVSPlan.runTransactionsVS("TEST_BANKVS_SEND_TRANSACTIONVS");
        log.info("Transaction report:" + transactionVSPlan.getReport());
        TestUtils.finish("currencyResultMap: " + currencyResultMap);
    }

}