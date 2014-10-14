package org.votingsystem.test.vicket


import groovy.util.logging.Log4j
import groovy.util.logging.Slf4j
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.VicketServer
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.test.model.SimulationData
import org.votingsystem.test.util.SignatureVSService
import org.votingsystem.test.util.TestHelper
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.NifUtils
import org.votingsystem.util.StringUtils

import java.security.KeyStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

Logger log = TestHelper.init(AddUsersToGroup.class)

Map userBaseData = [numUsers: 10, userIndex:100 ]
Map simulationDataMap = [groupId:4, serverURL:"http://vickets:8086/Vickets", userBaseData:userBaseData]


@Log4j
class SimulationHelper {

    SimulationData simulationData
    ExecutorService requestExecutor = Executors.newFixedThreadPool(100);
    List<String> errorList = Collections.synchronizedList(new ArrayList<String>());
    List<String> userList = new ArrayList<String>();
    VicketServer vicketServer
    JSONObject requestSubscribeData
    SignatureVSService signatureVSService = SignatureVSService.getAuthoritySignatureVSService()

    SimulationHelper(Map simulationDataMap) {
        simulationData = SimulationData.parse(JSONSerializer.toJSON(simulationDataMap))
    }

    private void finishSimulation(ResponseVS responseVS){
        log.debug("finishSimulation - StatusCode: ${responseVS.getStatusCode()}")
        simulationData.finish(responseVS.getStatusCode(), System.currentTimeMillis());
        log.debug("--------------- UserBaseDataSimulationService -----------");
        log.info("Begin: " + DateUtils.getDateStr(simulationData.getBeginDate())  +
                " - Duration: " + simulationData.getDurationStr());
        log.info("num users: " + userList.size());
        if(!errorList.isEmpty()) {
            String errorsMsg = StringUtils.getFormattedErrorList(errorList);
            log.info(" ************* " + errorList.size() + " ERRORS: \n" + errorsMsg);
            responseVS.setMessage(errorsMsg)
        }
        log.debug("-------------------------------------------------------");
    }

    private ResponseVS getGroupData(Long groupId) {
        ResponseVS responseVS = HttpHelper.getInstance().getData(vicketServer.getGroupURL(groupId), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            JSONObject dataJSON = JSONSerializer.toJSON(responseVS.getMessage())
            JSONObject groupDataJSON =  dataJSON.getJSONObject("userVS")
            JSONObject representativeDataJSON = groupDataJSON.getJSONObject("representative")
            //{"operation":,"groupvs":{"id":4,"name":"NombreGrupo","representative":{"id":2,"nif":"07553172H"}}}
            requestSubscribeData = new JSONObject([operation:"VICKET_GROUP_SUBSCRIBE"])
            JSONObject groupDataJSON1 = new JSONObject([id:groupDataJSON.getLong("id"), name:groupDataJSON.getString("name")])
            JSONObject representativeDataJSON1 = new JSONObject([id:representativeDataJSON.getLong("id"),
                                                                 nif:representativeDataJSON.getString("nif")])
            groupDataJSON1.put("representative", representativeDataJSON1)
            requestSubscribeData.put("groupvs", groupDataJSON1)
        }
        return responseVS
    }

    private ResponseVS subscribeUsers() {
        log.debug("subscribeUser - Num. Users:" + simulationData.getUserBaseSimulationData().getNumUsers());
        ResponseVS responseVS = null;
        int fromFirstUser = simulationData.getUserBaseSimulationData().getUserIndex().intValue()
        int toLastUser = simulationData.getUserBaseSimulationData().getUserIndex().intValue() +
                simulationData.getUserBaseSimulationData().getNumUsers()
        for(int i = fromFirstUser; i < toLastUser; i++ ) {
            int userIndex = new Long(simulationData.getUserBaseSimulationData().getAndIncrementUserIndex()).intValue();
            try {
                String userNif = NifUtils.getNif(userIndex);
                KeyStore mockDnie = signatureVSService.generateKeyStore(userNif);
                userList.add(userNif);
                String toUser = vicketServer.getNameNormalized();
                String subject = "subscribeToGroupMsg - subscribeToGroupMsg"
                requestSubscribeData.put("UUID", UUID.randomUUID().toString())
                SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, ContextVS.END_ENTITY_ALIAS,
                        ContextVS.PASSWORD.toCharArray(), ContextVS.DNIe_SIGN_MECHANISM);
                SMIMEMessage smimeMessage = signedMailGenerator.genMimeMessage(userNif, toUser,
                        requestSubscribeData.toString(), subject);
                SMIMESignedSender worker = new SMIMESignedSender(smimeMessage,
                        vicketServer.getSubscribeUserToGroupURL(simulationData.getGroupId()),
                        vicketServer.getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null, null);
                responseVS = worker.call();
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                    log.error("ERROR nif: " + userNif + " - msg:" + responseVS.getMessage());
                    errorList.add(responseVS.getMessage());
                    simulationData.getUserBaseSimulationData().getAndIncrementnumUserRequestsERROR();
                } else simulationData.getUserBaseSimulationData().getAndIncrementnumUserRequestsOK();
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                errorList.add(ex.getMessage());
            }
            if(!errorList.isEmpty()) break;
            if((i % 50) == 0) log.debug("Created " + i + " of " +
                    simulationData.getUserBaseSimulationData().getNumUsers() + " mock DNIe certs");
        }
        if(!errorList.isEmpty()) responseVS = new ResponseVS(ResponseVS.SC_ERROR);
        else responseVS = new ResponseVS(ResponseVS.SC_OK)
        return responseVS
    }

    public void run() {
        simulationData.init(System.currentTimeMillis());
        //initializeServer
        log.debug("initializeServer")
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(simulationData.getServerURL()), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            vicketServer = ActorVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
            if(vicketServer.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != vicketServer.getEnvironmentVS()) {
                responseVS = new ResponseVS(ResponseVS.SC_ERROR, "SERVER NOT IN DEVELOPMENT MODE. Server mode:" +
                        vicketServer.getEnvironmentVS());
            } else {
                responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                        vicketServer.getTimeStampServerURL()),ContentTypeVS.JSON);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    ActorVS timeStampServer = ActorVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
                    ContextVS.getInstance().setTimeStampServerCert(timeStampServer.getCertChain().iterator().next());
                }
            }
            ResponseVS response = getGroupData(simulationData.getGroupId());
            if(ResponseVS.SC_OK != response.statusCode) throw new ExceptionVS(responseVS.getMessage())
            response = subscribeUsers();
            finishSimulation(response);
        } else {
            log.error("ERROR initializing server: " + responseVS.getMessage())
            System.exit(0);
        }
    }
}

SimulationHelper simulationHelper = new SimulationHelper(simulationDataMap)
simulationHelper.run()
return