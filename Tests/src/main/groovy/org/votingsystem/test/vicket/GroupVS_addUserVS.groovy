package org.votingsystem.test.vicket

import net.sf.json.JSONObject
import org.votingsystem.model.ContextVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VicketServer
import org.votingsystem.test.util.MockDNI
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.SimulationData
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.ExceptionVS

Map userBaseData = [userIndex:100]
Map simulationDataMap = [groupId:4, serverURL:"http://vickets:8086/Vickets", numRequestsProjected: 5, userBaseData:userBaseData]
isWithUserValidation = Boolean.TRUE


log = TestUtils.init(GroupVS_addUserVS.class, simulationDataMap)
SimulationData simulationData = TestUtils.getSimulationData()
SignatureService authoritySignatureService = SignatureService.getAuthoritySignatureService()

log.debug("initializeServer")
VicketServer vicketServer = TestUtils.fetchVicketServer(simulationData.getServerURL())
if(vicketServer.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != vicketServer.getEnvironmentVS()) {
    throw new ExceptionVS("SERVER NOT IN DEVELOPMENT MODE. Server mode:" + vicketServer.getEnvironmentVS());
}
ContextVS.getInstance().setDefaultServer(vicketServer)
JSONObject subscriptionData = TestUtils.getGroupVSSubscriptionData(vicketServer.getGroupURL(simulationData.getGroupId()));

log.debug("subscribeUsers")
List<MockDNI> userList = authoritySignatureService.subscribeUsers(subscriptionData, simulationData, vicketServer)

if(!isWithUserValidation) TestUtils.finish()

log.debug("activateUsers")
SignatureService representativeSignatureService = SignatureService.getUserVSSignatureService("00111222V", UserVS.Type.USER)
representativeSignatureService.validateUserVSSubscriptions(simulationDataMap.groupId, vicketServer,
        TestUtils.getUserVSMap(userList))

TestUtils.finish()
