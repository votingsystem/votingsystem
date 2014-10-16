package org.votingsystem.test.vicket

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.ContextVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.VicketServer
import org.votingsystem.test.model.SimulationData
import org.votingsystem.test.util.MockDNI
import org.votingsystem.test.util.SignatureVSService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS

log = TestUtils.init(AddUsersToGroup.class)

Map userBaseData = [numUsers: 2, userIndex:280]
Map simulationDataMap = [groupId:133, serverURL:"http://vickets:8086/Vickets", userBaseData:userBaseData]
isWithUserValidation = Boolean.TRUE

SignatureVSService authoritySignatureVSService = SignatureVSService.getAuthoritySignatureVSService()
simulationData = SimulationData.parse(JSONSerializer.toJSON(simulationDataMap))
simulationData.init(System.currentTimeMillis());

log.debug("initializeServer")
VicketServer vicketServer = TestUtils.fetchVicketServer(simulationData.getServerURL())
if(vicketServer.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != vicketServer.getEnvironmentVS()) {
    throw new ExceptionVS("SERVER NOT IN DEVELOPMENT MODE. Server mode:" + vicketServer.getEnvironmentVS());
}
ContextVS.getInstance().setDefaultServer(vicketServer)
JSONObject subscriptionData = TestUtils.getGroupVSData(vicketServer.getGroupURL(simulationData.getGroupId()));

log.debug("subscribeUsers")
userList = authoritySignatureVSService.subscribeUsers(subscriptionData, simulationData, vicketServer)

if(!isWithUserValidation) finishSimulation()

log.debug("activateUsers")
SignatureVSService representativeSignatureService = SignatureVSService.getUserVSSignatureVSService("./certs/Cert_UserVS_00111222V.jks")
representativeSignatureService.validateUserVSSubscriptions(simulationDataMap.groupId, vicketServer, getUserVSMap(userList))
finishSimulation()


private Map<String, MockDNI> getUserVSMap(List<MockDNI> userList) {
    Map<String, MockDNI> result = new HashMap<>();
    for(MockDNI mockDNI:userList) {
        result.put(mockDNI.getNif(), mockDNI);
    }
    return result
}

private void finishSimulation() {
    simulationData.finish(ResponseVS.SC_OK, System.currentTimeMillis());
    log.debug("--------------- finishSimulation AddUsersToGroup - isWithUserValidation: $isWithUserValidation --------");
    log.info("Begin: ${DateUtils.getDateStr(simulationData.getBeginDate())} - Duration: ${simulationData.getDurationStr()}")
    log.info("num users: " + userList.size());
    log.debug("------------------------------------------------------------------------------------------------------");
}