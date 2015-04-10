package org.votingsystem.test.currency;

import org.votingsystem.model.CurrencyServer;
import org.votingsystem.model.UserVS;
import org.votingsystem.test.util.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EnvironmentVS;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GroupVS_addUserVS {

    public static void main(String[] args) throws Exception {
        UserBaseSimulationData userBaseSimulationData = new UserBaseSimulationData();
        userBaseSimulationData.setUserIndex(100L);
        SimulationData simulationData = new SimulationData();
        simulationData.setGroupId(5L);
        simulationData.setServerURL("http://localhost:8080/CurrencyServer");
        simulationData.setNumRequestsProjected(5);
        simulationData.setUserBaseSimulationData(userBaseSimulationData);
        Boolean isWithUserValidation = Boolean.TRUE;

        Logger log = TestUtils.init(GroupVS_addUserVS.class, simulationData);
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
        representativeSignatureService.validateUserVSSubscriptions(simulationData.getGroupId(), currencyServer,
                TestUtils.getUserVSMap(userList));
        TestUtils.finish(null);
    }

}

