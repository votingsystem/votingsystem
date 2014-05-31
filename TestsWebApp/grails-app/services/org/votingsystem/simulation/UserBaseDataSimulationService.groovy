package org.votingsystem.simulation

import grails.transaction.Transactional
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.StatusVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.simulation.callable.RepresentativeDelegatorDataSender
import org.votingsystem.simulation.callable.RepresentativeTestDataSender
import org.votingsystem.simulation.model.UserBaseSimulationData
import org.votingsystem.util.DateUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.NifUtils
import org.votingsystem.util.StringUtils

import java.lang.management.ManagementFactory
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

@Transactional
class UserBaseDataSimulationService {

    public enum Status implements StatusVS<Status> {INIT_SIMULATION, INITIALIZE_SERVER, USERS_WITHOUT_REPRESENTATIVE,
            REPRESENTATIVES, DELEGATIONS, FINISH_SIMULATION}

    def grailsApplication

    private ExecutorService requestExecutor;
    private static CompletionService<ResponseVS> requestCompletionService;

    private final AtomicBoolean representativeDataTerminated = new AtomicBoolean(false);
    private final AtomicBoolean usersWithouRepresentativeTerminated = new AtomicBoolean(false);

    private SimulatorListener simulationListener;

    private List<String> representativeNifList;
    private List<String> userWithRepresentativeList;
    private List<String> userWithoutRepresentativeList;

    private UserBaseSimulationData simulationData;
    private List<String> errorList;


    private ResponseVS<UserBaseSimulationData> simulationResult
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public void initSimulation(UserBaseSimulationData simulationData, SimulatorListener listener) {
        log.debug("initSimulation ### Enter INIT_SIMULATION status")
        this.simulationData = simulationData;
        this.simulationListener = simulationListener;
        requestExecutor = Executors.newFixedThreadPool(100);
        representativeDataTerminated.set(false)
        usersWithouRepresentativeTerminated.set(false)
        requestCompletionService = new ExecutorCompletionService<ResponseVS>(requestExecutor);
        errorList = Collections.synchronizedList(new ArrayList<String>());
        representativeNifList = Collections.synchronizedList(new ArrayList<String>());
        userWithRepresentativeList = Collections.synchronizedList(new ArrayList<String>());
        userWithoutRepresentativeList = Collections.synchronizedList(new ArrayList<String>());
        log.debug("call - process:" + ManagementFactory.getRuntimeMXBean().getName());
        simulationData.init(System.currentTimeMillis());
        this.simulationListener = listener;
        changeSimulationStatus(new ResponseVS(ResponseVS.SC_OK, Status.INIT_SIMULATION, null));
    }

    private void initializeServer() {
        log.debug("initializeServer ### Enter INITIALIZE_SERVER status")
        String serviceURL = ContextVS.getInstance().getAccessControl().getUserBaseInitServiceURL()
        ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, null)
        responseVS.setStatus(Status.INITIALIZE_SERVER);
        changeSimulationStatus(responseVS);
    }


    private void initSimulationThreads() {
        try {
            requestExecutor.execute(new Runnable() {@Override public void run() {createRepresentatives();}});
            requestExecutor.execute(new Runnable() {@Override public void run() {waitForRepresentativeResponses();}});
            requestExecutor.execute(new Runnable() {@Override public void run() {createUsersWithoutRepresentative();}});

        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            errorList.add(ex.getMessage())
            changeSimulationStatus(new ResponseVS(ResponseVS.SC_ERROR, Status.INIT_SIMULATION, ex.getMessage()));
        }
    }



    private void createUsersWithoutRepresentative() {
        log.debug("createUsersWithoutRepresentative ### Enter status USERS_WITHOUT_REPRESENTATIVE - " +
                "Users without representative:" + simulationData.getNumUsersWithoutRepresentativeWithVote());
        ResponseVS responseVS = null;
        for(int i = 1; i <= simulationData.getNumUsersWithoutRepresentativeWithVote(); i++ ) {
            int userIndex = new Long(simulationData.getAndIncrementUserIndex()).intValue();
            try {
                String userNif = NifUtils.getNif(userIndex);
                KeyStore keyStore = ContextVS.getInstance().generateKeyStore(userNif);
                userWithoutRepresentativeList.add(userNif);
                Certificate[] chain = keyStore.getCertificateChain(ContextVS.END_ENTITY_ALIAS);
                X509Certificate usertCert = (X509Certificate) chain[0];
                byte[] usertCertPEMBytes = CertUtil.getPEMEncoded(usertCert);
                String certServiceURL = ContextVS.getInstance().getAccessControl().getUserCertServiceURL();
                responseVS =HttpHelper.getInstance().sendData(usertCertPEMBytes,ContentTypeVS.X509_USER,certServiceURL);
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                    log.error("ERROR nif: " + userNif + " - msg:" + responseVS.getMessage());
                    errorList.add(responseVS.getMessage());
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                errorList.add(ex.getMessage());
            }
            if(!errorList.isEmpty()) break;
            if((i % 50) == 0) log.debug("Created " + i + " of " +
                    simulationData.getNumUsersWithoutRepresentativeWithVote() + " mock DNIe certs");
        }
        if(!errorList.isEmpty()) responseVS = new ResponseVS(ResponseVS.SC_ERROR);
        else responseVS = new ResponseVS(ResponseVS.SC_OK)
        responseVS.setStatus(Status.USERS_WITHOUT_REPRESENTATIVE)
        changeSimulationStatus(responseVS);
    }

    public void createRepresentatives () throws Exception {
        log.debug("createRepresentatives ### Enter status REPRESENTATIVES - NumRepresentatives: " +
                simulationData.getNumRepresentatives());
        if(simulationData.getNumRepresentatives() > 0) {
            File representativeImage = grailsApplication.mainContext.getResource("images/icon_64/fa-user.png").getFile()
            while (simulationData.hasRepresesentativeRequestsPending()){
                if(!simulationData.waitingForRepresesentativeRequests()) {
                    requestCompletionService.submit(new RepresentativeTestDataSender(NifUtils.getNif(
                            new Long(simulationData.getAndIncrementUserIndex()).intValue()), representativeImage));
                    simulationData.getAndIncrementNumRepresentativeRequests();
                } else Thread.sleep(500);
            }
        } else log.debug("Simulation without representative requests")
    }

    public void waitForRepresentativeResponses() throws InterruptedException, ExecutionException, Exception {
        log.debug("- waitForRepresentativeResponses - simulationData.getNumRepresentatives: " +
                simulationData.getNumRepresentatives());
        for (int v = 0; v < simulationData.getNumRepresentatives(); v++) {
            Future<ResponseVS> f = requestCompletionService.take();
            final ResponseVS responseVS = f.get();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                representativeNifList.add(responseVS.getMessage());
                simulationData.getAndIncrementNumRepresentativeRequestsOK();
            } else {
                log.error("ERROR - statusCode: " + responseVS.getStatusCode() + " - msg: " + responseVS.getMessage());
                errorList.add("RepresentativeResponses error - " + responseVS.getMessage());
                simulationData.getAndIncrementNumRepresentativeRequestsERROR();
            }
        }
        ResponseVS responseVS = null;
        if(simulationData.getNumRepresentativeRequestsERROR() > 0) responseVS = new ResponseVS(ResponseVS.SC_ERROR);
        else responseVS = new ResponseVS(ResponseVS.SC_OK)
        responseVS.setStatus(Status.REPRESENTATIVES)
        changeSimulationStatus (responseVS)
    }

    public void createDelegations () throws Exception {
        log.debug("createDelegations - enter status DELEGATIONS - NumUsersWithRepresentative - " +
                simulationData.getNumUsersWithRepresentative());
        if(simulationData.getNumUsersWithRepresentative() > 0) {
            String serviceURL = ContextVS.getInstance().getAccessControl().getDelegationServiceURL();
            while (simulationData.getNumDelegationRequests() < simulationData.getNumUsersWithRepresentative()) {
                if((simulationData.getNumDelegationRequests() -simulationData.getNumDelegationRequestsColected()) <
                        simulationData.getMaxPendingResponses()) {
                    String userNIF = NifUtils.getNif(new Long(simulationData.getAndIncrementUserIndex()).intValue());
                    String representativeNIF = getRandomRepresentative();
                    requestCompletionService.submit(new RepresentativeDelegatorDataSender(userNIF,representativeNIF, serviceURL));
                    simulationData.getAndIncrementNumDelegationRequests();
                } else Thread.sleep(200);
            }
        } else log.debug("UserBaseData simulation without representative delegations")
    }

    private void initDelegationThreads() {
        log.debug("initDelegationThreads - enter status DELEGATIONS")
        try {
            requestExecutor.execute(new Runnable() { @Override public void run() {createDelegations();}});
            requestExecutor.execute(new Runnable() { @Override public void run() {waitForDelegationResponses(); }});
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
            changeSimulationStatus(new ResponseVS(ResponseVS.SC_ERROR, Status.DELEGATIONS, ex.getMessage()));
        }
    }

    public void waitForDelegationResponses() throws InterruptedException, ExecutionException, Exception {
        log.debug("waitForDelegationResponses - getNumUsersWithRepresentative: " +
                simulationData.getNumUsersWithRepresentative());
        if(simulationData.getNumRepresentatives() > 0) {
            for (int v = 0; v < simulationData.getNumUsersWithRepresentative(); v++) {
                Future<ResponseVS> f = requestCompletionService.take();
                final ResponseVS response = f.get();
                log.debug("Delegation response '" + v + "' - statusCode:" +
                        response.getStatusCode() + " - msg: " + response.getMessage());
                if(ResponseVS.SC_OK == response.getStatusCode()) {
                    userWithRepresentativeList.add(response.getMessage());
                    simulationData.getAndIncrementNumDelegationsOK();
                } else {
                    simulationData.getAndIncrementNumDelegationsERROR();
                    errorList.add(response.getMessage());
                }
            }
        }
        ResponseVS responseVS = null;
        if(!errorList.isEmpty()) {
            String errorsMsg = StringUtils.getFormattedErrorList(errorList);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, errorsMsg);
        } else responseVS = new ResponseVS(ResponseVS.SC_OK)
        responseVS.setStatus(Status.DELEGATIONS)
        changeSimulationStatus (responseVS)
    }

    private String getRandomRepresentative() {
        if(representativeNifList.isEmpty()) return null;
        // In real life, the Random object should be rather more shared than this
        int randomSelected = new Random().nextInt(representativeNifList.size());
        return representativeNifList.get(randomSelected);
    }

    private void finishSimulation(ResponseVS responseVS){
        log.debug("finishSimulation ### Enter FINISH_SIMULATION status - StatusCode: ${responseVS.getStatusCode()}")
        simulationData.finish(responseVS.getStatusCode(), System.currentTimeMillis());
        simulationData.setUsersWithRepresentativeList(userWithRepresentativeList);
        simulationData.setUsersWithoutRepresentativeList(userWithoutRepresentativeList);
        simulationData.setRepresentativeNifList(representativeNifList);
        if(requestExecutor != null) requestExecutor.shutdownNow();
        log.debug("--------------- UserBaseDataSimulationService -----------");
        log.info("Begin: " + DateUtils.getStringFromDate(
                simulationData.getBeginDate())  + " - Duration: " + simulationData.getDurationStr());
        log.info("numRepresentativeRequests: " + simulationData.getNumRepresentativeRequests());
        log.info("numRepresentativeRequestsOK: " + simulationData.getNumRepresentativeRequestsOK());
        log.info("numRepresentativeRequestsERROR: " + simulationData.getNumRepresentativeRequestsERROR());
        log.info("delegationRequests: " + simulationData.getNumDelegationRequests());
        log.info("delegationRequestsOK: " + simulationData.getNumDelegationsOK());
        log.info("delegationRequestsERROR: " + simulationData.getNumDelegationsERROR());
        log.info("userWithoutRepresentative: " + userWithoutRepresentativeList.size());
        if(!errorList.isEmpty()) {
            String errorsMsg = StringUtils.getFormattedErrorList(errorList);
            log.info(" ************* " + errorList.size() + " ERRORS: \n" + errorsMsg);
            responseVS.setMessage(errorsMsg)
        }
        log.debug("-------------------------------------------------------");
        responseVS.setData(simulationData);
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
                } else initSimulationThreads();
                break;
            case Status.USERS_WITHOUT_REPRESENTATIVE:
                if(ResponseVS.SC_OK != statusFromResponse.getStatusCode()) {
                    finishSimulation(statusFromResponse);
                } else usersWithouRepresentativeTerminated.set(true)
                break;
            case Status.REPRESENTATIVES:
                if(ResponseVS.SC_OK != statusFromResponse.getStatusCode()) {
                    finishSimulation(statusFromResponse);
                } else initDelegationThreads();
                break;
            case Status.DELEGATIONS:
                if(ResponseVS.SC_OK != statusFromResponse.getStatusCode()) {
                    finishSimulation(statusFromResponse);
                } else representativeDataTerminated.set(true)
                break;
            case Status.FINISH_SIMULATION:
                if(simulationListener != null) simulationListener.processResponse(statusFromResponse)
                else log.debug("### Null simulationListener")
                return
                break;
        }
        if(usersWithouRepresentativeTerminated.get() && representativeDataTerminated.get()) {
            finishSimulation(new ResponseVS(ResponseVS.SC_OK));
        }
    }

}
