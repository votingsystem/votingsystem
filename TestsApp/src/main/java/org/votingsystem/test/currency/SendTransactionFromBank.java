package org.votingsystem.test.currency;

import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.dto.TransactionPlanDto;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;

import java.io.File;
import java.util.logging.Logger;


public class SendTransactionFromBank {

    private static Logger log =  Logger.getLogger(SendTransactionFromBank.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer();
        File transactionsPlan = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("transactionsPlan/bank.json"));
        TransactionPlanDto transactionPlanDto = JSON.getMapper().readValue(transactionsPlan, TransactionPlanDto.class);
        transactionPlanDto.setCurrencyServer(currencyServer);
        transactionPlanDto.runTransactionsVS();
        log.info("Transaction report:" + transactionPlanDto.getReport());
        log.info("currencyResultMap: " + transactionPlanDto.getBankBalance());
        System.exit(0);
    }

}