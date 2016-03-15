package org.votingsystem.test.currency;

import org.votingsystem.dto.currency.GroupDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.util.List;
import java.util.logging.Logger;

public class AddUserToGroup {

    private static Logger log =  Logger.getLogger(AddUserToGroup.class.getName());

    private static boolean subscribeNewUserList = true;

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        UserBaseSimulationData userBaseSimulationData = new UserBaseSimulationData();
        userBaseSimulationData.setUserIndex(100L);
        SimulationData simulationData = new SimulationData();
        simulationData.setGroupId(4L);
        simulationData.setServerURL("https://192.168.1.5/CurrencyServer");
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
        String groupURL = currencyServer.getGroupURL(simulationData.getGroupId());
        GroupDto groupDto = HttpHelper.getInstance().getData(GroupDto.class, groupURL, MediaTypeVS.JSON);
        groupDto.setOperation(TypeVS.CURRENCY_GROUP_SUBSCRIBE);
        if(subscribeNewUserList) {
            log.info("subscribeUsers");
            List<DNIBundle> userList = authoritySignatureService.subscribeUsers(groupDto, simulationData, currencyServer);
            if(!isWithUserValidation) simulationData.finishAndExit(ResponseVS.SC_OK, null);
        }
        log.info("activateUsers");
        SignatureService representativeSignatureService = SignatureService.getUserSignatureService(
                "Currency_07553172H", User.Type.USER);
        representativeSignatureService.validateUserSubscriptions(simulationData.getGroupId(), currencyServer);
        simulationData.finishAndExit(ResponseVS.SC_OK, null);
    }

}

