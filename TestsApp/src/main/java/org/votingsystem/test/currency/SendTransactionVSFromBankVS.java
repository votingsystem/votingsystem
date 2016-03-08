package org.votingsystem.test.currency;

import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.dto.TransactionVSPlanDto;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;

import java.io.File;
import java.util.logging.Logger;


public class SendTransactionVSFromBankVS {

    private static Logger log =  Logger.getLogger(SendTransactionVSFromBankVS.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        File transactionsPlan = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("transactionsPlan/bankVS.json"));
        TransactionVSPlanDto transactionVSPlanDto = JSON.getMapper().readValue(transactionsPlan, TransactionVSPlanDto.class);
        transactionVSPlanDto.setCurrencyServer(currencyServer);
        transactionVSPlanDto.runTransactionsVS();
        log.info("Transaction report:" + transactionVSPlanDto.getReport());
        log.info("currencyResultMap: " + transactionVSPlanDto.getBankVSBalance());
        System.exit(0);
    }

}