package org.votingsystem.simulation

import org.apache.log4j.Logger
import org.votingsystem.callable.PDFSignedSender
import org.votingsystem.callable.SMIMESignedSender

import java.security.KeyStore
import java.security.PrivateKey
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.CompletionService
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future
import java.security.cert.Certificate

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.model.EventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.simulation.callable.ClaimSignedSender
import org.votingsystem.simulation.callable.ServerInitializer
import org.votingsystem.model.*
import org.votingsystem.util.*
import org.votingsystem.simulation.model.*
import org.votingsystem.util.PdfFormHelper;

import com.itextpdf.text.pdf.PdfReader;
import grails.converters.JSON

class ClaimSimulationService {

    public enum Status implements StatusVS<Status> {INIT_SIMULATION, INIT_ACCESS_CONTROL, PUBLISH_EVENT, SEND_CLAIMS,
        CHANGE_EVENT_STATE, REQUEST_BACKUP ,FINISH_SIMULATION, LISTEN}

    private Long broadcastMessageInterval = 10000;
    private Locale locale = new Locale("es")
	
	def webSocketService
	def messageSource
	def grailsApplication
	private String simulationStarter

	private List<String> synchronizedSignerList = null;
	private List<String> errorList = new ArrayList<String>();
	private Set<String> synchronizedListenerSet;
	
	private static ExecutorService simulatorExecutor;
	private static CompletionService<ResponseVS> signClaimCompletionService;
	private Timer simulationTimer;
	private Timer broadcastTimer;
	private SimulationData simulationData;
	private EventVS eventVS;
	private SignedMailGenerator signedMailGenerator

	
	public void processRequest(JSONObject messageJSON) {
		log.debug(" --- processRequest - for status: '${messageJSON?.status}'")
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
                case Status.LISTEN:
                    synchronizedListenerSet.add(messageJSON.userId)
                    break;
                default:
                    log.error("UNKNOWN OPERATION ${messageJSON.status}")
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
        }
	}
	
	private void initSimulation(JSONObject simulationDataJSON) {
        log.debug("initSimulation - Enter status INIT_SIMULATION")
		errorList = new ArrayList<String>();
		simulationStarter = simulationDataJSON.userId
        synchronizedListenerSet = Collections.synchronizedSet(new HashSet<String>())
		synchronizedListenerSet.add(simulationStarter)
		simulationData = SimulationData.parse(simulationDataJSON)
		ContextVS.getInstance().initTestEnvironment("${grailsApplication.config.VotingSystem.simulationFilesBaseDir}");
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
        log.debug("starting timer - interval between requests: "+ interval + " milliseconds");
        simulationTimer = new Timer();
        simulationTimer.schedule(new SignTask(), 0, interval);
    }

    class SignTask extends TimerTask {
        public void run() {
            if(!synchronizedSignerList.isEmpty()) {
                int randomSigner = new Random().nextInt(synchronizedSignerList.size());
                launchSignature(synchronizedSignerList.remove(randomSigner));
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
                Logger.getLogger(MultiSignSimulationService.class).debug("Cancelling BroadcastTimerTask - statusCode(): "
                        + simulationData.getStatusCode() + " - message: " + simulationData.getMessage())
                launcher.cancel();
            }
        }
    }

    private void initAccessControl() {
        log.debug("initAccessControl ### Enter INIT_ACCESS_CONTROl status")
        ServerInitializer accessControlInitializer = new ServerInitializer(simulationData.getAccessControlURL(),
                ActorVS.Type.ACCESS_CONTROL);
        ResponseVS responseVS = accessControlInitializer.call();
        responseVS.setStatus(Status.INIT_ACCESS_CONTROL)
        changeSimulationStatus(responseVS)
    }
	
	private void publishEvent(EventVS event) throws Exception {
        log.debug("publishEvent ### Enter status PUBLISH_EVENT");
		DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		String dateStr = formatter.format(date);
		String subject = event.getSubject()+ " -> " + dateStr;
		event.setSubject(subject);
		this.eventVS = event;
		String eventStr = "${eventVS.getDataMap() as JSON}".toString();
		String urlPublishClaim = ContextVS.getInstance().getAccessControl().getPublishClaimURL()
		String msgSubject = messageSource.getMessage("publishClaimMsgSubject",null, locale);
		KeyStore keyStore = ContextVS.getInstance().getUserTest().getKeyStore()
		PrivateKey privateKey = (PrivateKey)keyStore.getKey(
			ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD.toCharArray());
		Certificate[] chain = keyStore.getCertificateChain(ContextVS.END_ENTITY_ALIAS);
		signedMailGenerator = new SignedMailGenerator(
			privateKey, chain, ContextVS.DNIe_SIGN_MECHANISM);
		SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(
				ContextVS.getInstance().getUserTest().getEmail(),
				ContextVS.getInstance().getAccessControl().getNameNormalized(),
				eventStr, msgSubject,  null);
		SMIMESignedSender signedSender = new SMIMESignedSender(smimeDocument, urlPublishClaim,
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED, null, null);
		ResponseVS responseVS = signedSender.call();
		if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
			try {
				byte[] responseBytes = responseVS.getMessageBytes();
				ContextVS.getInstance().copyFile(responseBytes, "/claimSimulation", "ClaimPublishedReceipt")
				SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(
						new ByteArrayInputStream(responseBytes));
				dnieMimeMessage.verify(ContextVS.getInstance().getSessionPKIXParameters());
                EventVS eventToSign = EventVS.populate(new JSONObject(dnieMimeMessage.getSignedContent()));
                eventVS.setId(eventToSign.getId());
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex);
                responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage())
			}
		}
        responseVS.setStatus(Status.PUBLISH_EVENT)
        changeSimulationStatus(responseVS)
	}

    private void sendClaims(){
        log.debug("sendVotes ### Enter status SEND_CLAIMS");
		if(!(simulationData.getNumRequestsProjected() > 0)) {
			log.debug("WITHOUT NumberOfRequestsProjected");
			return;
		}
		List<String> signerList = new ArrayList<String>();
		for(int i = 0; i < simulationData.getNumRequestsProjected(); i++) {
			signerList.add(NifUtils.getNif(i));
		}
        synchronizedSignerList = Collections.synchronizedList(signerList)
		
		simulatorExecutor = Executors.newFixedThreadPool(100);
		signClaimCompletionService = new ExecutorCompletionService<ResponseVS>(simulatorExecutor);
        try {
            simulatorExecutor.execute(new Runnable() {
                @Override public void run() { sendClaimRequests(); }
            });
            simulatorExecutor.execute(new Runnable() {
                @Override public void run() { waitForClaimResponses(); }
            });
        }catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            changeSimulationStatus(new ResponseVS(ResponseVS.SC_ERROR, Status.SEND_CLAIMS, ex.getMessage()));
        }
	}

	public void sendClaimRequests () throws Exception {
		log.debug(" ----------- sendClaimRequests - NumRequestsProjected: " +
				simulationData.getNumRequestsProjected());
		if(simulationData.isTimerBased()) startSimulationTimer(simulationData, this);
		else {
			while(!synchronizedSignerList.isEmpty()) {
				if((simulationData.getNumRequests() -
						simulationData.getNumRequestsColected()) <
						simulationData.getMaxPendingResponses()) {
					int randomSigner = new Random().nextInt(synchronizedSignerList.size());
					launchSignature(synchronizedSignerList.remove(randomSigner));
				} else Thread.sleep(200);
			}
		}
	}

    private void launchSignature(String nif) throws Exception {
        signClaimCompletionService.submit(new ClaimSignedSender(nif, eventVS.getId()));
        simulationData.getAndIncrementNumRequests();
    }
	
	private void waitForClaimResponses() throws Exception {
		log.debug(" -------------- waitForClaimResponses - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
		while (simulationData.getNumRequestsProjected() > simulationData.getNumRequestsColected()) {
			try {
				Future<ResponseVS> f = signClaimCompletionService.take();
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
            String errorsMsg = StringUtils.getFormattedErrorList(errorList);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, Status.SEND_CLAIMS, errorsMsg);
        } else responseVS = new ResponseVS(ResponseVS.SC_OK, Status.SEND_CLAIMS, null)
        changeSimulationStatus(responseVS);
	}

    private void changeEventState() throws Exception {
        log.debug("changeEventState ### Enter status CHANGE_EVENT_STATE");
        Map cancelDataMap = eventVS.getChangeEventDataMap(ContextVS.getInstance().getAccessControl().getServerURL(),
                simulationData.getEventStateWhenFinished());
        String cancelDataStr = new JSONObject(cancelDataMap).toString()
        String msgSubject = messageSource.getMessage("cancelEventMsgSubject", [eventVS.getId()].toArray(), locale);
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(ContextVS.getInstance().getUserTest().getKeyStore(),
                ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD.toCharArray(), ContextVS.getInstance().VOTE_SIGN_MECHANISM);
        SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(ContextVS.getInstance().getUserTest().getEmail(),
                ContextVS.getInstance().getAccessControl().getNameNormalized(), cancelDataStr, msgSubject,  null);
        SMIMESignedSender worker = new SMIMESignedSender(smimeDocument,
                ContextVS.getInstance().getAccessControl().getCancelEventServiceURL(),
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED, null, null);
        ResponseVS responseVS = worker.call();
        responseVS.setStatus(Status.CHANGE_EVENT_STATE);
        if(ResponseVS.SC_OK == responseVS.statusCode) responseVS.setMessage("Event state changed")
        changeSimulationStatus(responseVS);
    }

    private void requestBackup() throws Exception {
        log.debug("requestBackup ### Enter status REQUEST_BACKUP");
        byte[] requestBackupPDFBytes = PdfFormHelper.getBackupRequest(eventVS.getId().toString(), eventVS.getSubject(),
                simulationData.getBackupRequestEmail());

        KeyStore userTestKeyStore = ContextVS.getInstance().getUserTest().getKeyStore();
        PrivateKey signerPrivateKey = (PrivateKey)userTestKeyStore.getKey(
                ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD.toCharArray());
        Certificate[] signerCertChain = userTestKeyStore.getCertificateChain(ContextVS.END_ENTITY_ALIAS);

        PdfReader requestBackupPDF = new PdfReader(requestBackupPDFBytes);
        String urlBackupEvents = ContextVS.getInstance().getAccessControl().getBackupServiceURL();

        PDFSignedSender worker = new PDFSignedSender(urlBackupEvents,
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(), null, null, null,
                requestBackupPDF, signerPrivateKey, signerCertChain, null);
        ResponseVS responseVS = worker.call();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            String downloadServiceURL = ContextVS.getInstance().getAccessControl().getDownloadServiceURL(responseVS.getMessage());
            responseVS = HttpHelper.getInstance().getData(downloadServiceURL, ContentTypeVS.BACKUP);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                log.debug("TODO validate backup");
                responseVS.setMessage(ContentTypeVS.ZIP.getName());
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

        log.info("Begin: " + DateUtils.getStringFromDate(simulationData.getBeginDate())  + " - Duration: " +
				simulationData.getDurationStr());
        log.info("------- SIMULATION RESULT for EventVS: " + eventVS?.getId());
        log.info("numRequestsProjected: " + simulationData.getNumRequestsProjected());
		log.info("numRequests: " + simulationData.getNumRequests());
		log.info("numRequestsOK: " + simulationData.getNumRequestsOK());
		log.info("numRequestsERROR: " + simulationData.getNumRequestsERROR());

        String message = responseVS.getMessage();
        if(!errorList.isEmpty()) {
            String errorsMsg = StringUtils.getFormattedErrorList(errorList);
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
                        initAccessControl();
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.INIT_ACCESS_CONTROL:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        publishEvent(simulationData.getEventVS());
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.PUBLISH_EVENT:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        Thread.sleep(1000);//to avoid timestamping race issues
                        sendClaims();
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.SEND_CLAIMS:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        if(simulationData.getEventStateWhenFinished() != null) {
                            changeEventState();
                        } else if(simulationData.getBackupRequestEmail() != null) {
                            requestBackup();
                        } else finishSimulation(statusFromResponse);
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.CHANGE_EVENT_STATE:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode() &&
                            simulationData.getBackupRequestEmail() != null) {
                        requestBackup();
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.REQUEST_BACKUP:
                    finishSimulation(statusFromResponse);
                    break;
                case Status.FINISH_SIMULATION:
                    Map messageMap = [statusCode:statusFromResponse.statusCode,
                            status:statusFromResponse.getStatus().toString(),
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