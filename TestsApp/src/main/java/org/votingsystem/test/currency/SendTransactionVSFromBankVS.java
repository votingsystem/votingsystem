package org.votingsystem.test.currency;

import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.dto.TransactionVSPlanDto;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

import java.util.logging.Logger;


public class SendTransactionVSFromBankVS {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(SendTransactionVSFromBankVS.class);
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        TransactionVSPlanDto transactionVSPlanDto = JSON.getMapper().readValue(
                TestUtils.getFileFromResources("transactionsPlan/bankVS.json"), TransactionVSPlanDto.class);
        transactionVSPlanDto.setCurrencyServer(currencyServer);
        transactionVSPlanDto.runTransactionsVS("TEST_BANKVS_SEND_TRANSACTIONVS");
        log.info("Transaction report:" + transactionVSPlanDto.getReport());
        TestUtils.finish("currencyResultMap: " + transactionVSPlanDto.getBankVSBalance());
    }

}