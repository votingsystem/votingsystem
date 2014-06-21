package org.votingsystem.simulation

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.vickets.simulation.model.SimulationData
import org.vickets.simulation.model.UserBaseSimulationData
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.simulation.callable.ServerInitializer
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.DateUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.NifUtils
import org.votingsystem.util.StringUtils

import java.lang.management.ManagementFactory
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@Transactional
class VicketAddUsersToGroupSimulationService {

    public enum Status implements StatusVS<Status> {INIT_SIMULATION, INITIALIZE_SERVER, GET_GROUP_DATA,
        SUBSCRIBE_USERS, FINISH_SIMULATION, LISTEN}

    def grailsApplication
    def webSocketService
    def signatureVSService
    private Locale locale = new Locale("es")
    def messageSource

    private ExecutorService requestExecutor;

    private AtomicInteger simulationCounter = new AtomicInteger(0)

    private String simulationStarter
    private Set<String> synchronizedListenerSet;

    private List<String> userList;
    private VicketServer vicketServer;
    private JSONObject requestSubscribeData;

    private SimulationData simulationData;
    private List<String> errorList;


    public void processRequest(JSONObject messageJSON) {
        log.debug("--- processRequest - status: '${messageJSON?.status}'")
        try {
            Status status = Status.valueOf(messageJSON?.status);
            switch(status) {
                case Status.INIT_SIMULATION:
                    if(simulationData?.isRunning()) {
                        log.error("INIT_SIMULATION ERROR - Simulation Running")
                        Map responseMap = [userId: messageJSON.userId, message:"Simulation already running",
                                statusCode:ResponseVS.SC_ERROR, service:this.getClass().getSimpleName()]
                        webSocketService.processResponse(new JSONObject(responseMap))
                    } else {
                        initSimulation(messageJSON)
                    }
                    break;
                case Status.FINISH_SIMULATION:
                    if(!simulationData || !simulationData.isRunning()) {
                        log.error("SIMULATION ALREADY FINISHED")
                        return
                    }
                    if(simulationStarter?.equals(messageJSON.userId)) {
                        String message = messageSource.getMessage("simulationCancelledByUserMsg", null, locale) +
                                " - message: ${messageJSON.message}"
                        finishSimulation(new ResponseVS(ResponseVS.SC_CANCELLED, message));
                    }
                    break;
                case Status.LISTEN: synchronizedListenerSet.add(messageJSON.userId)
                    break;
                default:
                    log.error(" --- UNKNOWN STATUS ${status.toString()}")
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
        }
    }

    private void initSimulation(JSONObject simulationDataJSON) {
        log.debug("initSimulation ### Enter status INIT_SIMULATION")
        ContextVS.getInstance().initTestEnvironment("${grailsApplication.config.VotingSystem.simulationFilesBaseDir}/" +
                "vicket_add_users_to_group" + simulationCounter.getAndIncrement());
        synchronizedListenerSet = Collections.synchronizedSet(new HashSet<String>())
        requestExecutor = Executors.newFixedThreadPool(100);
        synchronizedListenerSet = Collections.synchronizedSet(new HashSet<String>())
        simulationData = SimulationData.parse(simulationDataJSON)
        errorList = Collections.synchronizedList(new ArrayList<String>());
        simulationStarter = simulationDataJSON.userId
        if(simulationDataJSON.locale) locale = new Locale(simulationDataJSON.locale)
        synchronizedListenerSet.add(simulationStarter)
        userList = new ArrayList<String>();
        simulationData.init(System.currentTimeMillis());
        log.debug("call - process:" + ManagementFactory.getRuntimeMXBean().getName());
        changeSimulationStatus(new ResponseVS(ResponseVS.SC_OK, Status.INIT_SIMULATION, null));
    }

    //For this service to work server cert must be installed as CA Authority
    private void initializeServer() {
        log.debug("initializeServer ### Enter INITIALIZE_SERVER status")
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(simulationData.getServerURL()), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            vicketServer = ActorVS.populate(new JSONObject(responseVS.getMessage()));
            if(vicketServer.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != vicketServer.getEnvironmentVS()) {
                responseVS = new ResponseVS(ResponseVS.SC_ERROR, "SERVER NOT IN DEVELOPMENT MODE. Server mode:" +
                        vicketServer.getEnvironmentVS());
            } else {
                responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                        vicketServer.getTimeStampServerURL()),ContentTypeVS.JSON);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    ActorVS timeStampServer = ActorVS.populate(new JSONObject(responseVS.getMessage()));
                    ContextVS.getInstance().setTimeStampServerCert(timeStampServer.getCertChain().iterator().next());
                }
            }
        }
        responseVS.setStatus(Status.INITIALIZE_SERVER)
        changeSimulationStatus(responseVS)
    }

    private void getGroupData() {
        ResponseVS responseVS = HttpHelper.getInstance().getData(vicketServer.getGroupURL(simulationData.getGroupId()), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            JSONObject groupDataJSON = new JSONObject(responseVS.getMessage())
            JSONObject representativeDataJSON = groupDataJSON.getJSONObject("representative")
            //{"operation":,"groupvs":{"id":4,"name":"NombreGrupo","representative":{"id":2,"nif":"07553172H"}}}
            requestSubscribeData = new JSONObject([operation:"VICKET_GROUP_SUBSCRIBE"])
            JSONObject groupDataJSON1 = new JSONObject([id:groupDataJSON.getLong("id"), name:groupDataJSON.getString("name")])
            JSONObject representativeDataJSON1 = new JSONObject([id:representativeDataJSON.getLong("id"),
                         nif:representativeDataJSON.getString("nif")])
            groupDataJSON1.put("representative", representativeDataJSON1)
            requestSubscribeData.put("groupvs", groupDataJSON1)
        }
        responseVS.setStatus(Status.GET_GROUP_DATA)
        changeSimulationStatus(responseVS)
    }


    private void subscribeUsers() {
        log.debug("subscribeUsers ### Enter status SUBSCRIBE_USERS - " +
                "Num. Users:" + simulationData.getUserBaseSimulationData().getNumUsers());
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
                String subject = messageSource.getMessage("subscribeToGroupMsg", null, locale)
                requestSubscribeData.put("UUID", UUID.randomUUID().toString())
                SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, ContextVS.END_ENTITY_ALIAS,
                        ContextVS.PASSWORD.toCharArray(), ContextVS.DNIe_SIGN_MECHANISM);
                SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(userNif, toUser,
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
        responseVS.setStatus(Status.SUBSCRIBE_USERS)
        changeSimulationStatus(responseVS);
    }

    private void finishSimulation(ResponseVS responseVS){
        log.debug("finishSimulation ### Enter FINISH_SIMULATION status - StatusCode: ${responseVS.getStatusCode()}")
            simulationData.finish(responseVS.getStatusCode(), System.currentTimeMillis());
        log.debug("--------------- UserBaseDataSimulationService -----------");
        log.info("Begin: " + DateUtils.getStringFromDate(simulationData.getBeginDate())  +
                " - Duration: " + simulationData.getDurationStr());
        log.info("num users: " + userList.size());
        if(!errorList.isEmpty()) {
            String errorsMsg = StringUtils.getFormattedErrorList(errorList);
            log.info(" ************* " + errorList.size() + " ERRORS: \n" + errorsMsg);
            responseVS.setMessage(errorsMsg)
        }
        log.debug("-------------------------------------------------------");
        responseVS.setStatus(Status.FINISH_SIMULATION)
        changeSimulationStatus (responseVS)
    }

    private void changeSimulationStatus (ResponseVS<UserBaseSimulationData> statusFromResponse) {
        log.debug("changeSimulationStatus - statusFrom: '${statusFromResponse.getStatus()}' " +
                " - statusCode: ${statusFromResponse.getStatusCode()}")
        if(ResponseVS.SC_OK != statusFromResponse.getStatusCode())
            log.debug("statusFromResponse message: ${statusFromResponse.getMessage()}")
        switch(statusFromResponse.getStatus()) {
            case Status.INIT_SIMULATION:
                if(ResponseVS.SC_OK != statusFromResponse.getStatusCode()) {
                    finishSimulation(statusFromResponse);
                } else {
                    initializeServer();
                }
                break;
            case Status.INITIALIZE_SERVER:
                if(ResponseVS.SC_OK != statusFromResponse.getStatusCode()) {
                    finishSimulation(statusFromResponse);
                } else {
                    try {
                        requestExecutor.execute(new Runnable() {@Override public void run() {getGroupData();}});
                    } catch(Exception ex) {
                        log.error(ex.getMessage(), ex);
                        errorList.add(ex.getMessage())
                        changeSimulationStatus(new ResponseVS(ResponseVS.SC_ERROR, Status.INITIALIZE_SERVER, ex.getMessage()));
                    }
                }
                break;
            case Status.GET_GROUP_DATA:
                if(ResponseVS.SC_OK != statusFromResponse.getStatusCode()) {
                    finishSimulation(statusFromResponse);
                } else {
                    try {
                        requestExecutor.execute(new Runnable() {@Override public void run() {subscribeUsers();}});
                    } catch(Exception ex) {
                        log.error(ex.getMessage(), ex);
                        errorList.add(ex.getMessage())
                        changeSimulationStatus(new ResponseVS(ResponseVS.SC_ERROR, Status.GET_GROUP_DATA, ex.getMessage()));
                    }
                }
                break;
            case Status.SUBSCRIBE_USERS:
                finishSimulation(statusFromResponse);
                break;
            case Status.FINISH_SIMULATION:
                Map messageMap = [statusCode:statusFromResponse.statusCode, service:this.getClass().getSimpleName(),
                        status:statusFromResponse.status.toString(),
                        message:statusFromResponse.message, simulationData:simulationData.getDataMap()]
                webSocketService.broadcastList(messageMap, synchronizedListenerSet)
                break;
        }
    }

}
