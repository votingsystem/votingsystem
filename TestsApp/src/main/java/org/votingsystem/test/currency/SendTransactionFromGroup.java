package org.votingsystem.test.currency;

import org.votingsystem.dto.currency.GroupDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.dto.TransactionPlanDto;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.*;

import java.io.File;
import java.util.logging.Logger;

public class SendTransactionFromGroup {

    private static Logger log =  Logger.getLogger(SendTransactionFromGroup.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SimulationData simulationData = new SimulationData();
        simulationData.setGroupId(5L);
        simulationData.setServerURL("https://192.168.1.5/CurrencyServer");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer();
        GroupDto groupDto = HttpHelper.getInstance().getData(
                GroupDto.class, currencyServer.getGroupURL(simulationData.getGroupId()), MediaType.JSON);
        File transactionsPlan = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("transactionsPlan/group.json"));
        TransactionPlanDto transactionPlanDto = JSON.getMapper().readValue(transactionsPlan, TransactionPlanDto.class);
        transactionPlanDto.setCurrencyServer(currencyServer);
        transactionPlanDto.setGroupDto(groupDto);


        transactionPlanDto.runGroupTransactions();
        simulationData.finishAndExit(ResponseVS.SC_OK, "currencyResultMap: " + transactionPlanDto.getGroupBalance());
    }

}

