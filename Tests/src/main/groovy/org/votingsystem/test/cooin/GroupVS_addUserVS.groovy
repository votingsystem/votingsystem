package org.votingsystem.test.cooin

import net.sf.json.JSONObject
import org.votingsystem.model.ContextVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.CooinServer
import org.votingsystem.test.util.MockDNI
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.SimulationData
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.ExceptionVS

Map userBaseData = [userIndex:100]
Map simulationDataMap = [groupId:5, serverURL:"http://cooins:8086/Cooins", numRequestsProjected: 5, userBaseData:userBaseData]
isWithUserValidation = Boolean.TRUE

log = TestUtils.init(GroupVS_addUserVS.class, simulationDataMap)
SimulationData simulationData = TestUtils.getSimulationData()
SignatureService authoritySignatureService = SignatureService.getAuthoritySignatureService()

log.debug("initializeServer")
CooinServer cooinServer = TestUtils.fetchCooinServer(simulationData.getServerURL())
if(cooinServer.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != cooinServer.getEnvironmentVS()) {
    throw new ExceptionVS("SERVER NOT IN DEVELOPMENT MODE. Server mode:" + cooinServer.getEnvironmentVS());
}
ContextVS.getInstance().setDefaultServer(cooinServer)
JSONObject subscriptionData = TestUtils.getGroupVSSubscriptionData(cooinServer.getGroupURL(simulationData.getGroupId()));

log.debug("subscribeUsers")
List<MockDNI> userList = authoritySignatureService.subscribeUsers(subscriptionData, simulationData, cooinServer)

if(!isWithUserValidation) TestUtils.finish()

log.debug("activateUsers")
SignatureService representativeSignatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER)
representativeSignatureService.validateUserVSSubscriptions(simulationDataMap.groupId, cooinServer,
        TestUtils.getUserVSMap(userList))

TestUtils.finish()