package org.votingsystem.test.currency;

import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.dto.TransactionVSPlanDto;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.*;

import java.io.File;
import java.util.logging.Logger;

public class SendTransactionVSFromGroupVS {

    private static Logger log =  Logger.getLogger(SendTransactionVSFromGroupVS.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SimulationData simulationData = new SimulationData();
        simulationData.setGroupId(5L);
        simulationData.setServerURL("https://192.168.1.5/CurrencyServer");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);

        GroupVSDto groupVSDto = HttpHelper.getInstance().getData(
                GroupVSDto.class, currencyServer.getGroupURL(simulationData.getGroupId()), MediaTypeVS.JSON);
        File transactionsPlan = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("transactionsPlan/groupVS.json"));
        TransactionVSPlanDto transactionVSPlanDto = JSON.getMapper().readValue(transactionsPlan, TransactionVSPlanDto.class);
        transactionVSPlanDto.setCurrencyServer(currencyServer);
        transactionVSPlanDto.setGroupVSDto(groupVSDto);


        transactionVSPlanDto.runGroupVSTransactions("TEST_GROUPVS_SEND_TRANSACTIONVS");
        simulationData.finishAndExit(ResponseVS.SC_OK, "currencyResultMap: " + transactionVSPlanDto.getGroupVSBalance());
    }

}

