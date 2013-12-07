package org.votingsystem.simulation

import com.itextpdf.text.pdf.PdfReader
import grails.converters.JSON
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.callable.PDFSignedSender
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.StatusVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VoteVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator

import org.votingsystem.simulation.callable.ServerInitializer
import org.votingsystem.simulation.callable.VoteSender
import org.votingsystem.simulation.model.SimulationData
import org.votingsystem.simulation.model.UserBaseSimulationData
import org.votingsystem.simulation.model.VotingSimulationData
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.PdfFormHelper
import org.votingsystem.util.DateUtils
import org.votingsystem.util.StringUtils

import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class ElectionSimulationService implements SimulatorListener<UserBaseSimulationData> {

    public enum Status implements StatusVS<Status> {INIT_SIMULATION, INIT_ACCESS_CONTROL, INIT_CONTROL_CENTER,
        PUBLISH_EVENT, CREATE_USER_BASE_DATA, SEND_VOTES, CHANGE_EVENT_STATE, REQUEST_BACKUP ,FINISH_SIMULATION, LISTEN}

    private Long broadcastMessageInterval = 10000;
    private Locale locale = new Locale("es")

	def webSocketService
	def messageSource
	def grailsApplication
    def userBaseDataSimulationService
	private String simulationStarter
    private List<String> synchronizedElectorList
	private List<String> errorList;
	private Set<String> synchronizedListenerSet;
	private ExecutorService simulatorExecutor;
	private CompletionService<ResponseVS> responseService;
	private Timer simulationTimer;
	private Timer broadcastTimer;
	private VotingSimulationData simulationData;
	private EventVS eventVS;
	private SignedMailGenerator signedMailGenerator
    private AtomicInteger simulationCounter = new AtomicInteger(0)

	
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
                "simulation_" + simulationCounter.getAndIncrement());
        synchronizedListenerSet = Collections.synchronizedSet(new HashSet<String>())
		simulationData = VotingSimulationData.parse(simulationDataJSON)
        errorList = Collections.synchronizedList(new ArrayList<String>());
        simulationStarter = simulationDataJSON.userId
        synchronizedListenerSet.add(simulationStarter)
        simulationData.init(System.currentTimeMillis());
        startBroadcatsTimer();
        changeSimulationStatus(new ResponseVS(ResponseVS.SC_OK, Status.INIT_SIMULATION, null));
	}

    public void startSimulationTimer(SimulationData simulationData) throws Exception {
        log.debug("startSimulationTimer")
        Long hoursMillis = 1000 * 60 * 60 * new Long(simulationData.getNumHoursProjected());
        Long minutesMillis = 1000 * 60 * new Long(simulationData.getNumMinutesProjected());
        Long secondMillis = 1000 * new Long(simulationData.getNumSecondsProjected());
        Long totalMillis = hoursMillis + minutesMillis + secondMillis;
        Long interval = totalMillis/simulationData.getNumRequestsProjected();
        log.debug("starting timer - interval between requests: " + interval + " milliseconds");
        simulationTimer = new Timer();
        simulationTimer.schedule(new SignTask(), 0, interval);
    }

    class SignTask extends TimerTask {
        public void run() {
            if(!synchronizedElectorList.isEmpty()) {
                int randomElector = new Random().nextInt(synchronizedElectorList.size());
                responseService.submit(new VoteSender(VoteVS.genRandomVote(ContextVS.VOTING_DATA_DIGEST, eventVS),
                        new UserVS(synchronizedElectorList.remove(randomElector))));
            } else simulationTimer.stop();
        }
    }

    public void startBroadcatsTimer() throws Exception {
        log.debug("startBroadcatsTimer - interval between broadcasts: '${broadcastMessageInterval}' milliseconds");
        if(broadcastTimer != null) broadcastTimer.cancel();
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
                Logger.getLogger(MultiSignSimulationService.class).debug("Cancelling BroadcastTimerTask - statusCode():"
                        + simulationData.getStatusCode() + " - message: " + simulationData.getMessage())
                launcher.cancel();
            }
        }
    }

    private void initAccessControl() {
        log.debug("initAccessControl ### Enter status INIT_ACCESS_CONTROl")
        ServerInitializer serverInitializer = new ServerInitializer(simulationData.getAccessControlURL(),
                ActorVS.Type.ACCESS_CONTROL);
        ResponseVS responseVS = serverInitializer.call();
        responseVS.setStatus(Status.INIT_ACCESS_CONTROL)
        changeSimulationStatus(responseVS)
    }

    private void initControlCenter() throws Exception {
        log.debug("initControlCenter ### Enter status INIT_CONTROl_CENTER");
        ServerInitializer serverInitializer = new ServerInitializer(simulationData.getControlCenterURL(),
                ActorVS.Type.CONTROL_CENTER);
        ResponseVS responseVS = serverInitializer.call();
        responseVS.setStatus(Status.INIT_CONTROL_CENTER)
        changeSimulationStatus(responseVS)
    }

	private void publishEvent(EventVS eventVS) throws Exception {
		log.debug("publishElection ### Enter status PUBLISH_EVENT");
		DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		String dateStr = formatter.format(date);
		String subject = "Simulation Event -> " + eventVS.getSubject() + " -> " + dateStr;
        eventVS.setSubject(subject);
        eventVS.setType(EventVS.Type.ELECTION)
        eventVS.setControlCenterVS(ContextVS.getInstance().getControlCenter());
		this.eventVS = eventVS;
		String eventStr = "${eventVS.getDataMap() as JSON}".toString();
        String urlPublishELection = ContextVS.getInstance().getAccessControl().getPublishElectionURL()
		String msgSubject = messageSource.getMessage("publishElectionMsgSubject", null, locale);
		KeyStore keyStore = ContextVS.getInstance().getUserTest().getKeyStore()
		PrivateKey privateKey = (PrivateKey)keyStore.getKey(
			ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD.toCharArray());
		Certificate[] chain = keyStore.getCertificateChain(ContextVS.END_ENTITY_ALIAS);
		signedMailGenerator = new SignedMailGenerator(privateKey, chain, ContextVS.DNIe_SIGN_MECHANISM);
		SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(
				ContextVS.getInstance().getUserTest().getEmail(),
				ContextVS.getInstance().getAccessControl().getNameNormalized(),
				eventStr, msgSubject,  null);
		SMIMESignedSender signedSender = new SMIMESignedSender(smimeDocument, urlPublishELection, null, null);
		ResponseVS responseVS = signedSender.call();
		if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] responseBytes = responseVS.getMessageBytes();
            ContextVS.getInstance().copyFile(responseBytes,"/electionSimulation", "ElectionPublishedReceipt")
            SMIMEMessageWrapper smimeDocumentReceipt = new SMIMEMessageWrapper(
                    new ByteArrayInputStream(responseBytes));
            this.eventVS = EventVSElection.populate(new JSONObject(smimeDocumentReceipt.getSignedContent()))
            //smimeDocumentReceipt.verify(ContextVS.getInstance().getSessionPKIXParameters());
		}
        responseVS.setStatus(Status.PUBLISH_EVENT)
        changeSimulationStatus(responseVS)
	}

    private List<String> getElectorList(UserBaseSimulationData userBaseData) {
        log.debug("getElectorList");
        int totalNumberElectors = userBaseData.getNumberElectors();
        List<String> result = new ArrayList<String>(totalNumberElectors);
        List<String> representativesList = new ArrayList<String>(userBaseData.getRepresentativeNifList());
        if(userBaseData.getNumRepresentativesWithVote() > 0) {
            for(int i = 0; i < userBaseData.getNumRepresentativesWithVote(); i++) {
                int randomRep = new Random().nextInt(representativesList.size());
                result.add(representativesList.remove(randomRep));
            }
            log.debug("Added to elector list '" + userBaseData.getNumRepresentativesWithVote() + "' representatives");
        }
        List<String> userWithRepresentativesList = new ArrayList<String>(userBaseData.getUsersWithRepresentativeList());
        for(int i = 0; i < userBaseData.getNumUsersWithRepresentativeWithVote(); i++) {
            int randomUser = new Random().nextInt(userWithRepresentativesList.size());
            result.add(userWithRepresentativesList.remove(randomUser));
        }
        log.debug("Added to elector list '" + userBaseData.getNumUsersWithRepresentativeWithVote() +
                "' users WITH representatives");
        List<String> userWithoutRepresentativesList = new ArrayList<String>(
                userBaseData.getUsersWithoutRepresentativeList());
        for(int i = 0; i < userBaseData.getNumUsersWithoutRepresentativeWithVote(); i++) {
            int randomUser = new Random().nextInt(userWithoutRepresentativesList.size());
            result.add(userWithoutRepresentativesList.remove(randomUser));
        }
        log.debug("Added to elector list '" + userBaseData.getNumUsersWithoutRepresentativeWithVote() +
                "' users WITHOUT representatives");
        return result;
    }
    
	private void sendVotes(UserBaseSimulationData userBaseSimulationData){
        log.debug("sendVotes ### Enter status SEND_VOTES ");
        synchronizedElectorList =  Collections.synchronizedList(getElectorList(userBaseSimulationData));
		if(!(synchronizedElectorList.size() > 0)) {
            changeSimulationStatus(new ResponseVS(ResponseVS.SC_ERROR,
                    Status.SEND_VOTES, "WITHOUT NumberOfRequestsProjected"))
		} else {
            simulatorExecutor = Executors.newFixedThreadPool(100);
            responseService = new ExecutorCompletionService<ResponseVS>(simulatorExecutor);
            simulationData.setNumOfElectors(new Integer(synchronizedElectorList.size()).longValue())
            simulatorExecutor.execute(new Runnable() {@Override public void run() {
                sendVoteRequests(userBaseSimulationData); } });
            simulatorExecutor.execute(new Runnable() { @Override public void run() { waitForVoteResponses(); } });
        }
	}

	public void sendVoteRequests (UserBaseSimulationData userBaseSimulationData) throws Exception {
        if(simulationData.isTimerBased()) startSimulationTimer(userBaseSimulationData);
        else {
            while(!synchronizedElectorList.isEmpty()) {
                if(!simulationData.waitingForVoteRequests()) {
                    int randomElector = new Random().nextInt(synchronizedElectorList.size());
                    responseService.submit(new VoteSender(VoteVS.genRandomVote(ContextVS.VOTING_DATA_DIGEST,
                            eventVS), new UserVS(synchronizedElectorList.remove(randomElector))));
                } else Thread.sleep(500);
            }
        }
	}
	
	private void waitForVoteResponses() throws Exception {
        log.debug(" - waitForVoteResponses - NumOfElectors: " + simulationData.getNumOfElectors());
        while (simulationData.hasPendingVotes()) {
            String nifFrom = null;
            try {
                Future<ResponseVS> f = responseService.take();
                ResponseVS responseVS = f.get();
                nifFrom = responseVS.getData()?.userVS?.getNif();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    VoteVS voteReceipt = responseVS.getData().voteVS;
                    simulationData.getAndIncrementNumVotingRequestsOK();
                } else {
                    if(responseVS.getStatus() != null) {
                        String accessRequestErrorMsg = "ACCESS REQUEST ERROR - nif: " + nifFrom +
                                " - msg: " + responseVS.getMessage();
                        log.error(accessRequestErrorMsg);
                        errorList.add(accessRequestErrorMsg)
                        simulationData.getAndIncrementNumAccessRequestsERROR();
                    } else {
                        String voteErrorMsg = "VOTING ERROR - nif: " + nifFrom + " - msg: " + responseVS.getMessage();
                        log.error(voteErrorMsg);
                        SMIMEMessageWrapper voteWithErrors = responseVS.getSmimeMessage();
                        if(voteWithErrors != null) {
                            File outputFile = new File(ContextVS.ERROR_DIR + File.separator + "VoteError_" + nifFrom);
                            voteWithErrors.writeTo(new FileOutputStream(outputFile));
                            log.error("VOTING ERROR file copy to file -> " + outputFile.getAbsolutePath());
                        } else log.error("VOTING ERROR - response without vote");
                        errorList.add(voteErrorMsg)
                        simulationData.getAndIncrementNumVotingRequestsERROR();
                    }
                }
            } catch (Exception ex) {
                String exMessage = "ERROR from nif '${nifFrom}' - msg: ${ex.getMessage()}"
                errorList.add(exMessage)
                log.error(exMessage, ex);
                simulationData.getAndIncrementNumVotingRequestsERROR();
            }
        }
        ResponseVS responseVS = null;
        if(simulationData.getNumAccessRequestsERROR() > 0 || simulationData.getNumVotingRequestsERROR() > 0) {
            responseVS = new ResponseVS(ResponseVS.SC_ERROR);
            log.error("Status.SEND_VOTES - ERROR: " + responseVS.getMessage())
        } else responseVS = new ResponseVS(ResponseVS.SC_OK)
        responseVS.setStatus(Status.SEND_VOTES)
        changeSimulationStatus(responseVS);
	}

    private void changeEventState() throws Exception {
        log.debug("changeEventState ### Enter status CHANGE_EVENT_STATE");
        Map cancelDataMap = eventVS.getChangeEventDataMap(ContextVS.getInstance().getAccessControl().getServerURL(),
                simulationData.getEventStateWhenFinished());
        String cancelDataStr = new JSONObject(cancelDataMap).toString()
        String msgSubject = messageSource.getMessage("cancelEventMsgSubject",
                [eventVS.getId()].toArray(), locale);
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(ContextVS.getInstance().getUserTest().getKeyStore(),
                ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD.toCharArray(), ContextVS.VOTE_SIGN_MECHANISM);
        SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage( ContextVS.getInstance().getUserTest().getEmail(),
                ContextVS.getInstance().getAccessControl().getNameNormalized(), cancelDataStr, msgSubject,  null);
        SMIMESignedSender worker = new SMIMESignedSender(smimeDocument,
                ContextVS.getInstance().getAccessControl().getCancelEventServiceURL(),null, null);
        ResponseVS responseVS = worker.call();
        responseVS.setStatus(Status.CHANGE_EVENT_STATE);
        changeSimulationStatus(responseVS);
    }

    private void requestBackup() throws Exception {
        log.debug("requestBackup ### Enter status REQUEST_BACKUP");
        byte[] requestBackupPDFBytes = PdfFormHelper.getBackupRequest(eventVS.getId().toString(),
                eventVS.getSubject(), simulationData.getBackupRequestEmail());
        KeyStore userTestKeyStore = ContextVS.getInstance().getUserTest().getKeyStore();
        PrivateKey signerPrivateKey = (PrivateKey)userTestKeyStore.getKey(
                ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD.toCharArray());
        Certificate[] signerCertChain = userTestKeyStore.getCertificateChain(ContextVS.END_ENTITY_ALIAS);

        PdfReader requestBackupPDF = new PdfReader(requestBackupPDFBytes);
        String urlBackupEvents = ContextVS.getInstance().getAccessControl().getBackupServiceURL();

        PDFSignedSender worker = new PDFSignedSender(urlBackupEvents, null, null, null, requestBackupPDF,
                signerPrivateKey, signerCertChain, null);
        ResponseVS responseVS = worker.call();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            String downloadServiceURL = ContextVS.getInstance().getAccessControl().getDownloadServiceURL(responseVS.getMessage());
            responseVS = HttpHelper.getInstance().getData(downloadServiceURL, ContentTypeVS.BACKUP.getName());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                log.debug("TODO validate backup");
                /*FutureTask<ResponseVS> future = new FutureTask<ResponseVS>(
                    new ZipBackupValidator(responseVS.getMessageBytes()));
                simulatorExecutor.execute(future);
                responseVS = future.get();
                log.debug("BackupRequestWorker - status: " + responseVS.getStatusCode());*/
            }
        }
        responseVS.setStatus(Status.REQUEST_BACKUP)
        changeSimulationStatus(responseVS);
    }

	private void finishSimulation(ResponseVS responseVS) {
		log.debug(" --- finishSimulation Enter status FINISH_SIMULATION - status: ${responseVS.statusCode}")
		simulationData.finish(responseVS.getStatusCode(), System.currentTimeMillis());
		if(simulationTimer != null) simulationTimer.cancel();
		if(broadcastTimer != null) broadcastTimer.cancel();
		if(simulatorExecutor != null) simulatorExecutor.shutdownNow();
        log.info("Begin: " + DateUtils.getStringFromDate( simulationData.getBeginDate())  + " - Duration: " +
				simulationData.getDurationStr());
        log.info("------- SIMULATION RESULT for EventVS: " + eventVS?.getId());
        log.info("NumOfElectors: " + simulationData.getNumOfElectors());
        log.info("NumVotingRequestsOK: " + simulationData.getNumVotingRequestsOK());
        log.info("NumAccessRequestsERROR: " + simulationData.getNumAccessRequestsERROR());
		log.info("NumVotingRequestsERROR: " + simulationData.getNumVotingRequestsERROR());
		if(!errorList.isEmpty()) {
			String errorsMsg = StringUtils.getFormattedErrorList(errorList);
			log.info(" ************* " + errorList.size() + " ERRORS: \n" + errorsMsg);
            responseVS.appendMessage(errorsMsg)
        }
		simulationData.setStatusCode(responseVS.getStatusCode())
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
                        initAccessControl();
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.INIT_ACCESS_CONTROL:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        initControlCenter();
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.INIT_CONTROL_CENTER:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        publishEvent(simulationData.getEventVS());
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.PUBLISH_EVENT:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        log.debug("### Enter status CREATE_USER_BASE_DATA");
                        userBaseDataSimulationService.initSimulation(simulationData.getUserBaseData(), this)
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.CREATE_USER_BASE_DATA:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        sendVotes(statusFromResponse.getData());
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.SEND_VOTES:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        if(simulationData.getEventStateWhenFinished() != null) {
                            changeEventState();
                        } else finishSimulation(statusFromResponse);
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.CHANGE_EVENT_STATE:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        if(simulationData.getBackupRequestEmail() != null) {
                            requestBackup();
                        }
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.REQUEST_BACKUP:
                    finishSimulation(statusFromResponse);
                    break;
                case Status.FINISH_SIMULATION:
                    Map messageMap = [statusCode:statusFromResponse.statusCode, service:this.getClass().getSimpleName(),
                            status:statusFromResponse.status.toString(),
                            message:statusFromResponse.message, simulationData:simulationData.getDataMap()]
                    webSocketService.broadcastList(messageMap, synchronizedListenerSet)
                    break;
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
            finishSimulation(new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage()));
        }
    }

    //we know this is only called by userBaseDataSimulationService
    @Override void processResponse(ResponseVS<UserBaseSimulationData> response) {
        log.debug("processResponse - statusCode: ${response.getStatusCode()}")
        response.setStatus(Status.CREATE_USER_BASE_DATA)
        changeSimulationStatus(response)
    }

}