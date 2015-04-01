package org.votingsystem.test.currency;

import org.votingsystem.model.CurrencyServer;
import org.votingsystem.model.UserVS;
import org.votingsystem.test.util.MockDNI;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EnvironmentVS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GroupVS_addUserVS {

    public static void main(String[] args) throws Exception {
        Map userBaseData = new HashMap<>();
        userBaseData.put("userIndex", 100);
        Map simulationDataMap = new HashMap<>();
        simulationDataMap.put("groupId",5L);
        simulationDataMap.put("serverURL","http://currency:8086/CurrencyServer");
        simulationDataMap.put("numRequestsProjected", 5);
        simulationDataMap.put("userBaseData",userBaseData);
        Boolean isWithUserValidation = Boolean.TRUE;

        Logger log = TestUtils.init(GroupVS_addUserVS.class, simulationDataMap);
        SimulationData simulationData = TestUtils.getSimulationData();
        SignatureService authoritySignatureService = SignatureService.getAuthoritySignatureService();

        log.info("initializeServer");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(simulationData.getServerURL());
        if(currencyServer.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != currencyServer.getEnvironmentVS()) {
            throw new ExceptionVS("SERVER NOT IN DEVELOPMENT MODE. Server mode:" + currencyServer.getEnvironmentVS());
        }
        ContextVS.getInstance().setDefaultServer(currencyServer);
        Map subscriptionData = TestUtils.getGroupVSSubscriptionData(currencyServer.getGroupURL(simulationData.getGroupId()));
        log.info("subscribeUsers");
        List<MockDNI> userList = authoritySignatureService.subscribeUsers(subscriptionData, simulationData, currencyServer);
        if(!isWithUserValidation) TestUtils.finish(null);
        log.info("activateUsers");
        SignatureService representativeSignatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER);
        representativeSignatureService.validateUserVSSubscriptions((Long)simulationDataMap.get("groupId"), currencyServer,
                TestUtils.getUserVSMap(userList));
        TestUtils.finish(null);
    }

}

