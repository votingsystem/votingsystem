package org.votingsystem.test.currency;

import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AddUserVSToGroupVS {

    public static void main(String[] args) throws Exception {
        UserBaseSimulationData userBaseSimulationData = new UserBaseSimulationData();
        userBaseSimulationData.setUserIndex(600L);
        SimulationData simulationData = new SimulationData();
        simulationData.setGroupId(5L);
        simulationData.setServerURL("http://localhost:8080/CurrencyServer");
        simulationData.setNumRequestsProjected(5);
        simulationData.setUserBaseSimulationData(userBaseSimulationData);
        Boolean isWithUserValidation = Boolean.TRUE;

        Logger log = TestUtils.init(AddUserVSToGroupVS.class, simulationData);
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
        List<MockDNI> userList = authoritySignatureService.subscribeUsers(groupVSDto, simulationData, currencyServer);
        if(!isWithUserValidation) TestUtils.finish(null);
        log.info("activateUsers");
        SignatureService representativeSignatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER);
        representativeSignatureService.validateUserVSSubscriptions(simulationData.getGroupId(), currencyServer,
                TestUtils.getUserVSMap(userList));
        TestUtils.finish(null);
    }

}

