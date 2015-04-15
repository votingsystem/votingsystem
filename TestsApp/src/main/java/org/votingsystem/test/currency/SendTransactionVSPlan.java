package org.votingsystem.test.currency;

import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.test.dto.TransactionVSPlanDto;
import org.votingsystem.util.ContextVS;

import java.util.Map;
import java.util.logging.Logger;

public class SendTransactionVSPlan {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(SendTransactionVSFromBankVS.class, new SimulationData());
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        TransactionVSPlanDto transactionVSPlanDto = new TransactionVSPlanDto(
                TestUtils.getFileFromResources("transactionPlan.json"), currencyServer);
        Map bankVSCurrencyResultMap = transactionVSPlanDto.runBankVSTransactions("TEST_BANKVS_SEND_TRANSACTIONVS");
        Map groupVSCurrencyResultMap = transactionVSPlanDto.runGroupVSTransactions("TEST_GROUPVS_SEND_TRANSACTIONVS");
        TestUtils.finish("bankVSCurrencyResultMap: " + bankVSCurrencyResultMap + "\n groupVSCurrencyResultMap: " +
                groupVSCurrencyResultMap);
    }
}
