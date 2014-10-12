package org.votingsystem.simulation

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.vickets.simulation.model.SimulationData
import org.vickets.simulation.model.UserBaseSimulationData
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.simulation.callable.ServerInitializer
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
class VicketUserBaseDataSimulationService {

    public enum Status implements StatusVS<Status> {INIT_SIMULATION, INITIALIZE_SERVER,
        CREATE_USER_BASE_DATA, FINISH_SIMULATION, LISTEN}

    def grailsApplication
    def webSocketService
    private Locale locale = new Locale("es")
    def messageSource
    def signatureVSService

    private ExecutorService requestExecutor;

    private AtomicInteger simulationCounter = new AtomicInteger(0)

    private String simulationStarter
    private Set<String> synchronizedListenerSet;

    private List<String> userList;
    private ActorVS vicketServer;

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
        synchronizedListenerSet.add(simulationStarter)
        simulationData.init(System.currentTimeMillis());
        log.debug("call - process:" + ManagementFactory.getRuntimeMXBean().getName());
        changeSimulationStatus(new ResponseVS(ResponseVS.SC_OK, Status.INIT_SIMULATION, null));
    }

    private void initializeServer() {
        log.debug("initializeServer ### Enter INITIALIZE_SERVER status")
        //Prepare UserBase Data
        //String serviceURL = simulationData.getUserBaseSimulationData().getUserBaseInitServiceURL()
        ServerInitializer serverInitializer = new ServerInitializer(simulationData.getServerURL(), ActorVS.Type.VICKETS);
        userList = new ArrayList<String>();
        ResponseVS responseVS = serverInitializer.call();
        vicketServer = responseVS.getData();
        responseVS.setStatus(Status.INITIALIZE_SERVER)
        changeSimulationStatus(responseVS)
    }

    private void createUsers() {
        log.debug("createUsers ### Enter status CREATE_USER_BASE_DATA - " +
                "Num. Users:" + simulationData.getUserBaseSimulationData().getNumUsers());
        ResponseVS responseVS = null;
        for(int i = 1; i <= simulationData.getUserBaseSimulationData().getNumUsers(); i++ ) {
            int userIndex = new Long(simulationData.getUserBaseSimulationData().getAndIncrementUserIndex()).intValue();
            try {
                String userNif = NifUtils.getNif(userIndex);
                KeyStore keyStore = signatureVSService.generateKeyStore(userNif)

                userList.add(userNif);
                Certificate[] chain = keyStore.getCertificateChain(ContextVS.END_ENTITY_ALIAS);
                X509Certificate usertCert = (X509Certificate) chain[0];
                byte[] usertCertPEMBytes = CertUtils.getPEMEncoded(usertCert);
                String certServiceURL = vicketServer.getUserCertServiceURL();
                responseVS =HttpHelper.getInstance().sendData(usertCertPEMBytes,ContentTypeVS.X509_USER,certServiceURL);
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
        responseVS.setStatus(Status.CREATE_USER_BASE_DATA)
        changeSimulationStatus(responseVS);
    }

    private void finishSimulation(ResponseVS responseVS){
        log.debug("finishSimulation ### Enter FINISH_SIMULATION status - StatusCode: ${responseVS.getStatusCode()}")
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
                } else initializeServer();
                break;
            case Status.INITIALIZE_SERVER:
                if(ResponseVS.SC_OK != statusFromResponse.getStatusCode()) {
                    finishSimulation(statusFromResponse);
                } else {
                    try {
                        requestExecutor.execute(new Runnable() {@Override public void run() {createUsers();}});
                    } catch(Exception ex) {
                        log.error(ex.getMessage(), ex);
                        errorList.add(ex.getMessage())
                        changeSimulationStatus(new ResponseVS(ResponseVS.SC_ERROR, Status.INIT_SIMULATION, ex.getMessage()));
                    }
                }
                break;
            case Status.CREATE_USER_BASE_DATA:
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
