package org.votingsystem.test.vicket


import groovy.util.logging.Log4j
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.VicketServer
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.test.model.SimulationData
import org.votingsystem.test.util.MockDNI
import org.votingsystem.test.util.SignatureVSService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.NifUtils
import org.votingsystem.util.StringUtils

import java.security.KeyStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

Logger log = TestUtils.init(AddUsersToGroup.class)

Map userBaseData = [numUsers: 10, userIndex:300 ]
Map simulationDataMap = [groupId:39, serverURL:"http://vickets:8086/Vickets", userBaseData:userBaseData]

@Log4j
class SimulationHelper {

    SimulationData simulationData
    List<MockDNI> userList = null;
    VicketServer vicketServer
    SignatureVSService authoritySignatureVSService = SignatureVSService.getAuthoritySignatureVSService()

    SimulationHelper(Map simulationDataMap) {
        simulationData = SimulationData.parse(JSONSerializer.toJSON(simulationDataMap))
    }

    private void finishSimulation(){
        simulationData.finish(ResponseVS.SC_OK, System.currentTimeMillis());
        log.debug("--------------- finishSimulation AddUsersToGroup -----------");
        log.info("Begin: " + DateUtils.getDateStr(simulationData.getBeginDate())  +
                " - Duration: " + simulationData.getDurationStr());
        log.info("num users: " + userList.size());
        log.debug("------------------------------------------------------------");
    }

    public void run() {
        simulationData.init(System.currentTimeMillis());
        log.debug("initializeServer")
        vicketServer = TestUtils.fetchVicketServer(simulationData.getServerURL())
        if(vicketServer.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != vicketServer.getEnvironmentVS()) {
            throw new ExceptionVS("SERVER NOT IN DEVELOPMENT MODE. Server mode:" +
                    vicketServer.getEnvironmentVS());
        }
        ContextVS.getInstance().setDefaultServer(vicketServer)
        JSONObject subscriptionData = TestUtils.getGroupVSData(vicketServer.getGroupURL(simulationData.getGroupId()));
        userList = authoritySignatureVSService.subscribeUsers(subscriptionData, simulationData, vicketServer)
        finishSimulation();
    }

}

SimulationHelper simulationHelper = new SimulationHelper(simulationDataMap)
simulationHelper.run()
return