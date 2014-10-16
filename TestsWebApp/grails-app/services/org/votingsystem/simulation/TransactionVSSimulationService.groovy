package org.votingsystem.simulation

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.vickets.simulation.model.SimulationData
import org.vickets.simulation.model.UserBaseSimulationData
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.simulation.callable.ServerInitializer
import org.votingsystem.util.DateUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.NifUtils
import org.votingsystem.util.StringUtils

import java.lang.management.ManagementFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@Transactional
class TransactionVSSimulationService {

    public enum Status implements StatusVS<Status> {INIT_SIMULATION, INITIALIZE_SERVER,
        MAKE_DEPOSIT, FINISH_SIMULATION, LISTEN}

    def grailsApplication
    def webSocketService
    def signatureVSService
    private Locale locale = new Locale("es")
    def messageSource

    private ExecutorService requestExecutor;

    private AtomicInteger simulationCounter = new AtomicInteger(0)

    private String simulationStarter
    private Set<String> synchronizedListenerSet;

    private VicketServer vicketServer;

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
        log.debug(" initSimulation ### Enter status INIT_SIMULATION")
        ContextVS.getInstance().initTestEnvironment("${grailsApplication.config.VotingSystem.simulationFilesBaseDir}/" +
                "vicket_user_base_data_" + simulationCounter.getAndIncrement());
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
        ResponseVS responseVS = null;
        try {
            String userURL = "${simulationData.getServerURL()}/userVS/${simulationData.getReceptorId()}"
            responseVS = HttpHelper.getInstance().getData(userURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                UserVS userVS = UserVS.parse(new JSONObject(responseVS.getMessage()));
                ServerInitializer serverInitializer = new ServerInitializer(simulationData.getServerURL(), ActorVS.Type.VICKETS);
                responseVS = serverInitializer.call();
                vicketServer = responseVS.getData();
                responseVS.setStatus(Status.INITIALIZE_SERVER)
            }

        } catch(Exception ex) {
            responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, ex.getMessage())
            responseVS.setStatus(Status.INITIALIZE_SERVER)
            log.error(ex.getMessage(), ex)
        } finally {changeSimulationStatus(responseVS)}
    }

    private void makeTransactionVS() {
        log.debug("makeTransactionVS ### Enter status MAKE_DEPOSIT");
        ResponseVS responseVS = null;
        try {
            String userNif = NifUtils.getNif(8888888);
            KeyStore keyStore = signatureVSService.generateKeyStore(userNif)
            Certificate[] chain = keyStore.getCertificateChain(ContextVS.END_ENTITY_ALIAS);
            PrivateKey privateKey = (PrivateKey)keyStore.getKey(
                    ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD.toCharArray());
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(privateKey, chain,
                    ContextVS.DNIe_SIGN_MECHANISM);
            String msgSubject =  messageSource.getMessage("transactionvsMsgSubject", null, locale)
            Map signatureContentMap = [amount:simulationData.getTransactionVSAmount().toString(),
                    "UUID": UUID.randomUUID().toString(), currency:simulationData.getCurrency().toString(),
                    subject:simulationData.getSubject(),
                    typeVS:TypeVS.FROM_BANKVS, IBAN:"ESkk bbbb gggg xxcc cccc cccc"]
            String signatureContentStr = new JSONObject(signatureContentMap).toString()
            log.debug("makeTransactionVS ${signatureContentStr}")
            SMIMEMessage smimeDocument = signedMailGenerator.genMimeMessage(userNif,
                    vicketServer.getNameNormalized(),signatureContentStr , msgSubject);
            SMIMESignedSender signedSender = new SMIMESignedSender(smimeDocument, vicketServer.getTransactionVSServiceURL(),
                    vicketServer.getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null, null);
            responseVS = signedSender.call();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            errorList.add(ex.getMessage());
        } finally {
            responseVS.setStatus(Status.MAKE_DEPOSIT)
            changeSimulationStatus(responseVS);
        }
    }

    private void finishSimulation(ResponseVS responseVS){
        log.debug("finishSimulation ### Enter FINISH_SIMULATION status - StatusCode: ${responseVS.getStatusCode()}")
            simulationData.finish(responseVS.getStatusCode(), System.currentTimeMillis());
        log.debug("--------------- UserBaseDataSimulationService -----------");
        log.info("Begin: " + DateUtils.getDateStr(simulationData.getBeginDate())  +
                " - Duration: " + simulationData.getDurationStr());
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
                        requestExecutor.execute(new Runnable() {@Override public void run() {makeTransactionVS();}});
                    } catch(Exception ex) {
                        log.error(ex.getMessage(), ex);
                        errorList.add(ex.getMessage())
                        changeSimulationStatus(new ResponseVS(ResponseVS.SC_ERROR, Status.INIT_SIMULATION, ex.getMessage()));
                    }
                }
                break;
            case Status.MAKE_DEPOSIT:
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
