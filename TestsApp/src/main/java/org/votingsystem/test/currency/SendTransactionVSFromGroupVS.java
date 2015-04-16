package org.votingsystem.test.currency;

import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.dto.TransactionVSPlanDto;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;

import java.io.File;
import java.util.logging.Logger;

public class SendTransactionVSFromGroupVS {

    public static void main(String[] args) throws Exception {
        SimulationData simulationData = new SimulationData();
        simulationData.setGroupId(5L);
        simulationData.setServerURL("http://localhost:8080/CurrencyServer");
        Logger log = TestUtils.init(SendTransactionVSFromGroupVS.class, simulationData);
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);

        GroupVSDto groupVSDto = HttpHelper.getInstance().getData(
                GroupVSDto.class, currencyServer.getGroupURL(simulationData.getGroupId()), MediaTypeVS.JSON);
        TransactionVSPlanDto transactionVSPlanDto = JSON.getMapper().readValue(
                TestUtils.getFileFromResources("transactionsPlan/groupVS.json"), TransactionVSPlanDto.class);
        transactionVSPlanDto.setCurrencyServer(currencyServer);
        transactionVSPlanDto.setGroupVSDto(groupVSDto);



        File resultFile = new File("ResultPlan");
        JSON.getMapper().writeValue(resultFile, transactionVSPlanDto);

        transactionVSPlanDto.runGroupVSTransactions("TEST_GROUPVS_SEND_TRANSACTIONVS");
        TestUtils.finish("currencyResultMap: " + transactionVSPlanDto.getGroupVSBalance());
    }

}

