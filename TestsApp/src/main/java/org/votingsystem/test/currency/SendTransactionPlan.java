package org.votingsystem.test.currency;

import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.dto.TransactionPlanDto;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;

import java.io.File;
import java.util.logging.Logger;

public class SendTransactionPlan {

    private static Logger log =  Logger.getLogger(SendTransactionPlan.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer();
        File transactionPlan = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("transactionPlan.json"));
        TransactionPlanDto transactionPlanDto = JSON.getMapper().readValue(transactionPlan, TransactionPlanDto.class);
        transactionPlanDto.setCurrencyServer(currencyServer);
        transactionPlanDto.runBankTransactions();
        transactionPlanDto.runGroupTransactions();
        log.info("bankCurrencyResultMap: " + transactionPlanDto.getBankBalance() + "\n groupCurrencyResultMap: " +
                transactionPlanDto.getGroupBalance());
    }

}
