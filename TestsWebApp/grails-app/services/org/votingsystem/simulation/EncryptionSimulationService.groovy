package org.votingsystem.simulation

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.simulation.callable.EncryptionTestSender
import org.votingsystem.simulation.model.SimulationData
import org.votingsystem.simulation.util.SimulationUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.NifUtils

import java.util.concurrent.*

class EncryptionSimulationService {

    public enum Status implements StatusVS<Status> {INIT_SIMULATION, SEND_REQUEST, FINISH_SIMULATION}

    private Long broadcastMessageInterval = 10000;
    private Locale locale = new Locale("es")

    def webSocketService
    def contextService
    def messageSource
    private String simulationStarter

    private List<String> errorList = new ArrayList<String>();
    private Set<String> synchronizedListenerSet;

    private static ExecutorService simulatorExecutor;
    private static CompletionService<ResponseVS> signCompletionService;
    private Timer broadcastTimer;
    private SimulationData simulationData;
    private EventVS eventVS;

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
                    } else initSimulation(messageJSON)
                    break;
                case Status.FINISH_SIMULATION:
                    if(simulationStarter?.equals(messageJSON.userId)) {
                        String message = messageSource.getMessage("simulationCancelledByUserMsg", null, locale) +
                                " - message: ${messageJSON.message}"
                        finishSimulation(new ResponseVS(ResponseVS.SC_CANCELLED, message));
                    }
                    break;
                default:
                    log.error("UNKNOWN STATUS ${messageJSON.status}")
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
        }
    }

    private void initSimulation(JSONObject simulationDataJSON) {
        log.debug("initSimulation ### Enter status INIT_SIMULATION")
        simulationStarter = simulationDataJSON.userId
        synchronizedListenerSet = Collections.synchronizedSet(new HashSet<String>())
        synchronizedListenerSet.add(simulationStarter)
        simulationData = SimulationData.parse(simulationDataJSON)
        log.debug("initSimulation - numRequestsProjected: " + simulationData.numRequestsProjected)
        contextService.init();
        simulationData.init(System.currentTimeMillis());
        startBroadcatsTimer();
        errorList = new ArrayList<String>();
        changeSimulationStatus(new ResponseVS(ResponseVS.SC_OK, Status.INIT_SIMULATION, null));
    }

    public void startBroadcatsTimer() throws Exception {
        log.debug("startBroadcatsTimer - interval between broadcasts: '${broadcastMessageInterval}' milliseconds");
        broadcastTimer = new Timer();
        broadcastTimer.schedule(new BroadcastTimerTask(simulationData, broadcastTimer), 0, broadcastMessageInterval);
    }

    class BroadcastTimerTask extends TimerTask {

        private SimulationData simulationData;
        private Timer launcher;

        public BroadcastTimerTask(SimulationData simulationData, Timer launcher) {
            this.simulationData = simulationData;
            this.launcher = launcher;
        }

        public void run() {
            //log.debug("======== BroadcastTimer run")
            if(ResponseVS.SC_PROCESSING == simulationData.getStatusCode()) {
                Map messageMap = [statusCode:ResponseVS.SC_PROCESSING, simulationData:simulationData.getDataMap()]
                Map broadcastResul = webSocketService.broadcastList(messageMap, synchronizedListenerSet);
                if(ResponseVS.SC_OK != broadcastResul.statusCode) {
                    broadcastResul.errorList.each {synchronizedListenerSet.remove(it)}
                }
            } else {
                Logger.getLogger(AccessRequestSimulationService.class).debug("Cancelling BroadcastTimerTask - statusCode(): "
                        + simulationData.getStatusCode() + " - message: " + simulationData.getMessage())
                launcher.cancel();
            }
        }
    }

    private void sendRequests(){
        log.debug("sendVotes ### Enter status SEND_REQUEST");
        if(!(simulationData.getNumRequestsProjected() > 0)) {
            log.debug("WITHOUT NumberOfRequestsProjected");
            return;
        }
        log.debug("--------------- launchRequests - NumRequestsProjected: " + simulationData.getNumRequestsProjected() +
                " - simulationData.getServerURL(): " + simulationData.getServerURL());

        final String serviceURL = simulationData.getServerURL() + "/encryptor";
        simulatorExecutor = Executors.newFixedThreadPool(100);
        signCompletionService = new ExecutorCompletionService<ResponseVS>(simulatorExecutor);
        try {
            simulatorExecutor.execute(new Runnable() {
                @Override public void run() { sendHttpRequest(serviceURL); }
            });
            simulatorExecutor.execute(new Runnable() {
                @Override public void run() { waitForResponses(); }
            });
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
            changeSimulationStatus(new ResponseVS(ResponseVS.SC_ERROR, Status.SEND_REQUEST, ex.getMessage()));
        }
    }

    public void sendHttpRequest (String serviceURL) throws Exception {
        log.debug(" ----------- sendRequests - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        while(simulationData.getNumRequests() < simulationData.getNumRequestsProjected()) {
            if((simulationData.getNumRequests() - simulationData.
                    getNumRequestsColected()) <= simulationData.getMaxPendingResponses()) {
                String nifFrom = NifUtils.getNif(simulationData.getAndIncrementNumRequests().intValue());
                signCompletionService.submit(new EncryptionTestSender(nifFrom, serviceURL));
            } else Thread.sleep(300);
        }
    }


    private void waitForResponses() throws Exception {
        log.debug(" -------------- waitForResponses - NumRequestsProjected: " +
                simulationData.getNumRequestsProjected());
        while (simulationData.getNumRequestsProjected() > simulationData.getNumRequestsColected()) {
            try {
                Future<ResponseVS> f = signCompletionService.take();
                ResponseVS responseVS = f.get();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    simulationData.getAndIncrementNumRequestsOK();
                } else {
                    simulationData.getAndIncrementNumRequestsERROR();
                    String msg = "Signature ERROR - msg: " + responseVS.getMessage();
                    errorList.add(msg);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                String msg = "Signature ERROR - msg: " + ex.getMessage();
                errorList.add(msg);
                simulationData.getAndIncrementNumRequestsERROR();
            }
        }
        ResponseVS responseVS = null;
        if(!errorList.isEmpty()) {
            String errorsMsg = SimulationUtils.getFormattedErrorList(errorList);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, Status.SEND_REQUEST, errorsMsg);
        } else responseVS = new ResponseVS(ResponseVS.SC_OK, Status.SEND_REQUEST, null)
        changeSimulationStatus(responseVS);
    }

    private void finishSimulation(ResponseVS responseVS) {
        log.debug(" --- finishSimulation Enter status FINISH_SIMULATION - status: ${responseVS.statusCode}")

        simulationData.finish(responseVS.getStatusCode(), System.currentTimeMillis());
        if(broadcastTimer != null) broadcastTimer.cancel();

        if(simulatorExecutor != null) simulatorExecutor.shutdown();

        log.info("Begin: " + DateUtils.getStringFromDate( simulationData.getBeginDate())  + " - Duration: " +
                simulationData.getDurationStr());
        log.info("------- SIMULATION RESULT for EventVS: " + eventVS?.getId());
        log.info("Number of projected requests: " + simulationData.getNumRequestsProjected());
        log.info("Number of completed requests: " + simulationData.getNumRequests());
        log.info("Number of signatures OK: " + simulationData.getNumRequestsOK());
        log.info("Number of signatures ERROR: " + simulationData.getNumRequestsERROR());

        String message = responseVS.getMessage();
        if(!errorList.isEmpty()) {
            String errorsMsg = SimulationUtils.getFormattedErrorList(errorList);
            if(message == null) message = errorsMsg;
            else message = message + "\n" + errorsMsg;
            log.info(" ************* " + errorList.size() + " ERRORS: \n" + errorsMsg);
        }
        simulationData.statusCode = responseVS.getStatusCode()
        simulationData.message = message
        responseVS.setStatus(Status.FINISH_SIMULATION)
        changeSimulationStatus(responseVS);
    }

    private void changeSimulationStatus (ResponseVS statusFromResponse) {
        log.debug("changeSimulationStatus - statusFrom: '${statusFromResponse.getStatus()}' " +
                " - statusCode: ${statusFromResponse.getStatusCode()}")
        if(ResponseVS.SC_OK != statusFromResponse.getStatusCode())
            log.debug("statusFromResponse message: ${statusFromResponse.getMessage()}")
        try {
            switch(statusFromResponse.getStatus()) {
                case Status.INIT_SIMULATION:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        sendRequests();
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.SEND_REQUEST:
                    finishSimulation(statusFromResponse);
                    break;
                case Status.FINISH_SIMULATION:
                    Map messageMap = [statusCode:statusFromResponse.statusCode,
                            status:statusFromResponse.status.toString(),
                            message:statusFromResponse.message, simulationData:simulationData.getDataMap()]
                    webSocketService.broadcastList(messageMap, synchronizedListenerSet)
                    break;
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
            finishSimulation(new ResponseVS(ResponseVS.SC_ERROR, statusFromResponse.getStatus() , ex.getMessage()));
        }
    }


}