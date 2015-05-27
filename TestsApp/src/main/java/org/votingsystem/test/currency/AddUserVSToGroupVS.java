package org.votingsystem.test.currency;

import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.util.List;
import java.util.logging.Logger;

public class AddUserVSToGroupVS {

    private static Logger log =  Logger.getLogger(AddUserVSToGroupVS.class.getName());

    public static void main(String[] args) throws Exception {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        UserBaseSimulationData userBaseSimulationData = new UserBaseSimulationData();
        userBaseSimulationData.setUserIndex(100L);
        SimulationData simulationData = new SimulationData();
        simulationData.setGroupId(106L);
        simulationData.setServerURL("http://localhost:8080/CurrencyServer");
        simulationData.setNumRequestsProjected(5);
        simulationData.setUserBaseSimulationData(userBaseSimulationData);
        Boolean isWithUserValidation = Boolean.TRUE;

        SignatureService authoritySignatureService = SignatureService.getAuthoritySignatureService();

        log.info("initializeServer");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(simulationData.getServerURL());
        if(currencyServer.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != currencyServer.getEnvironmentVS()) {
            throw new ExceptionVS("SERVER NOT IN DEVELOPMENT MODE. Server mode:" + currencyServer.getEnvironmentVS());
        }
        ContextVS.getInstance().setDefaultServer(currencyServer);
        String groupVSURL = currencyServer.getGroupURL(simulationData.getGroupId());
        GroupVSDto groupVSDto = HttpHelper.getInstance().getData(GroupVSDto.class, groupVSURL, MediaTypeVS.JSON);
        groupVSDto.setOperation(TypeVS.CURRENCY_GROUP_SUBSCRIBE);
        log.info("subscribeUsers");
        List<DNIBundle> userList = authoritySignatureService.subscribeUsers(groupVSDto, simulationData, currencyServer);
        if(!isWithUserValidation) simulationData.finishAndExit(ResponseVS.SC_OK, null);
        log.info("activateUsers");
        SignatureService representativeSignatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER);
        representativeSignatureService.validateUserVSSubscriptions(simulationData.getGroupId(), currencyServer, userList);
        simulationData.finishAndExit(ResponseVS.SC_OK, null);
    }

}

