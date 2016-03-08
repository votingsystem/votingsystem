package org.votingsystem.test.currency;

import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.dto.TransactionVSPlanDto;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;

import java.io.File;
import java.util.logging.Logger;

public class SendTransactionVSPlan {

    private static Logger log =  Logger.getLogger(SendTransactionVSPlan.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        File transactionPlan = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("transactionPlan.json"));
        TransactionVSPlanDto transactionVSPlanDto = JSON.getMapper().readValue(transactionPlan, TransactionVSPlanDto.class);
        transactionVSPlanDto.setCurrencyServer(currencyServer);
        transactionVSPlanDto.runBankVSTransactions();
        transactionVSPlanDto.runGroupVSTransactions();
        log.info("bankVSCurrencyResultMap: " + transactionVSPlanDto.getBankVSBalance() + "\n groupVSCurrencyResultMap: " +
                transactionVSPlanDto.getGroupVSBalance());
    }

}
