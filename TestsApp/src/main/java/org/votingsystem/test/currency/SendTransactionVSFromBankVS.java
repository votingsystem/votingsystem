package org.votingsystem.test.currency;

import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.test.dto.TransactionVSPlanDto;
import org.votingsystem.util.ContextVS;

import java.util.Map;
import java.util.logging.Logger;


public class SendTransactionVSFromBankVS {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(SendTransactionVSFromBankVS.class);
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        TransactionVSPlanDto transactionVSPlanDto = new TransactionVSPlanDto(
                TestUtils.getFileFromResources("transactionsPlan/bankVS.json"), currencyServer);
        transactionVSPlanDto.runTransactionsVS("TEST_BANKVS_SEND_TRANSACTIONVS");
        log.info("Transaction report:" + transactionVSPlanDto.getReport());
        TestUtils.finish("currencyResultMap: " + transactionVSPlanDto.getBankVSBalance());
    }

}