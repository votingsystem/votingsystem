package org.votingsystem.simulation

import java.security.KeyStore
import java.security.PrivateKey
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.List;
import java.util.Locale;
import java.util.TimerTask;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean
import java.security.cert.Certificate;
import java.security.cert.X509Certificate

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.springframework.beans.factory.InitializingBean
import org.votingsystem.model.EventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.simulation.callable.ManifestSigner
import org.votingsystem.simulation.callable.PDFSignedSender
import org.votingsystem.simulation.callable.SMIMESignedSender
import org.votingsystem.simulation.callable.ServerInitializer
import org.votingsystem.model.*
import org.votingsystem.util.*
import org.votingsystem.simulation.model.*
import org.votingsystem.simulation.util.PdfFormHelper;
import org.votingsystem.simulation.util.SimulationUtils;

import com.itextpdf.text.pdf.PdfReader;

import java.util.Timer

class ManifestSimulationService {
	
	def webSocketService
	def contextService
	def messageSource
	private String simulationStarter

	private Locale locale = new Locale("es")
	private Long timeStart
	private Long timeEnd
	private AtomicBoolean simulationRunning = new AtomicBoolean(false)
	
	private EventVSBase.Estado nextEventState = null;
	
	private List<String> signerList = null;
	private List<String> errorList = new ArrayList<String>();
	private static final Set<String> simulationListener = new HashSet<String>();
	
	private static ExecutorService simulatorExecutor;
	private static CompletionService<ResponseVS> signManifestCompletionService;
	private Timer simulationTimer;
	private Timer broadcastTimer;
	private SimulationData simulationData;
	private EventVS event;
	private byte[] pdfBytes;
	
    def serviceMethod() { }
	
	public void processRequest(SimulationOperation operation, JSONObject messageJSON) {
		log.debug("--- processRequest - operation: '${operation?.toString()}'")
		switch(operation) {
			case SimulationOperation.INIT_SIMULATION:
				if(!simulationRunning.get()) {
					initSimulation(messageJSON)
				} else {
					messageJSON.statusCode = ResponseVS.SC_SIMULATION_RUNNING
					webSocketService.processResponse(messageJSON)
				}
	
				break;
			case SimulationOperation.CANCEL_SIMULATION:
				if(simulationStarter?.equals(messageJSON.userId)) {
					stopSimulation();
				}
				break;
			case SimulationOperation.LISTEN:
				simulationListener.add(messageJSON.userId)
				break;
			default:
				log.error("UNKNOWN OPERATION ${messageJSON.operation}")
		}		
	}
	
	private void stopSimulation () {
		log.debug("stopSimulation")
	}
	
	private void initSimulation(JSONObject simulationDataJSON) {
		simulationStarter = simulationDataJSON.userId
		simulationListener.removeAll(simulationListener)//init listeners
		simulationListener.add(simulationStarter)
		simulationData = SimulationData.parse(simulationDataJSON)
		startBroadcatsTimer();
		//simulationRunning.set(true)
		log.debug("initSimulation - numRequestsProjected: " + simulationData.numRequestsProjected)
		contextService.init();
		
		simulationData.setBegin(System.currentTimeMillis());
		ServerInitializer accessControlInitializer = new ServerInitializer(simulationData.getAccessControlURL(), 
			ActorVS.Type.ACCESS_CONTROL);
		ResponseVS responseVS = accessControlInitializer.call();
		if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
			publishManifest(simulationData.getEvento());
	    }
		
		simulationDataJSON.statusCode = ResponseVS.SC_SIMULATION_INITIATED
		webSocketService.processResponse(simulationDataJSON)
	}
	
	private void publishManifest(EventVS event) throws Exception {
		log.debug("publishManifest");
		DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		String dateStr = formatter.format(date);
		nextEventState = event.getNextState();
		String subject = event.getAsunto()+ " -> " + dateStr;
		event.setAsunto(subject);
		this.event = event;
		String eventStr = new JSONObject(event.getDataMap()).toString();
		String urlPublishManifest = contextService.getAccessControl().getServerURL() + "/eventoFirma"
		ResponseVS responseVS = contextService.getHttpHelper().sendByteArray(eventStr.getBytes(), 
			ContentTypeVS.JSON, urlPublishManifest, "eventId")


		if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
			pdfBytes = responseVS.getMessageBytes();
			String eventId = ((List<String>)responseVS.getData()).iterator().next();
			String urlToSendDocument = urlPublishManifest +	"/" + eventId;
			event.setId(Long.valueOf(eventId));
			PdfReader manifestToSign = new PdfReader(pdfBytes);
			String reason = null;
			String location = null;
			KeyStore userTestKeyStore = contextService.getUserTest().getKeyStore();
			PrivateKey privateKey = (PrivateKey)userTestKeyStore.getKey(
				ContextService.END_ENTITY_ALIAS, ContextService.PASSWORD.toCharArray());
			Certificate[] signerCertChain = userTestKeyStore.getCertificateChain(ContextService.END_ENTITY_ALIAS);
			X509Certificate destinationCert = contextService.getAccessControl().getCertificate();	
			PDFSignedSender pdfSenderWorker =
					new PDFSignedSender(urlToSendDocument, reason, location, null,
					manifestToSign, privateKey, signerCertChain,
					destinationCert);
			responseVS = pdfSenderWorker.call();
			if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
				log.debug("Event published - message: - ${responseVS.getMessage()}");
				initExecutors();//Manifest published, we now initialize user base data.
			} else log.error(responseVS.getMessage());
			
		}
	}
	
	private void initExecutors(){
		if(!(simulationData.getNumRequestsProjected() > 0)) {
			log.debug("WITHOUT NumberOfRequestsProjected");
			return;
		}
		signerList = new ArrayList<String>();
		for(int i = 0; i < simulationData.getNumRequestsProjected(); i++) {
			signerList.add(NifUtils.getNif(i));
		}
		
		simulatorExecutor = Executors.newFixedThreadPool(100);
		signManifestCompletionService =
				new ExecutorCompletionService<ResponseVS>(simulatorExecutor);
		
		simulatorExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					launchRequests();
				} catch (Exception ex) {
					log.error(ex.getMessage(), ex);
				}
			}
		});
		simulatorExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					readResponses();
				} catch (Exception ex) {
					log.error(ex.getMessage(), ex);
				}
			}
		});
	}

	public void launchRequests () throws Exception {
		log.debug(" ----------- launchRequests - NumRequestsProjected: " +
				simulationData.getNumRequestsProjected());
		if(simulationData.isTimerBased()) startSimulationTimer(simulationData, this);
		else {
			while(!signerList.isEmpty()) {
				if((simulationData.getNumRequests() -
						simulationData.getNumRequestsColected()) <
						simulationData.getMaxPendingResponses()) {
					int randomSigner = new Random().nextInt(signerList.size());
					launchSignature(signerList.remove(randomSigner));
				} else Thread.sleep(200);
			}
		}
	}
	
	private void readResponses() throws Exception {
		log.debug(" -------------- readResponses - NumRequestsProjected: " +
				simulationData.getNumRequestsProjected());
		while (simulationData.getNumRequestsProjected() >
				simulationData.getNumRequestsColected()) {
			try {
				Future<ResponseVS> f = signManifestCompletionService.take();
				ResponseVS respuesta = f.get();
				if (ResponseVS.SC_OK == respuesta.getStatusCode()) {
					simulationData.getAndIncrementNumRequestsOK();
				} else {
					simulationData.getAndIncrementNumRequestsERROR();
					String msg = "Signature ERROR - msg: " + respuesta.getMessage();
					errorList.add(msg);
				}
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex);
				String msg = "Signature ERROR - msg: " + ex.getMessage();
				errorList.add(msg);
				simulationData.getAndIncrementNumRequestsERROR();
			}
		}
		if(nextEventState != null) setNextEventState();
		if(nextEventState != null && simulationData.
				getBackupRequestEmail() != null) requestBackup();
		finishSimulation();
	}
	
	private void finishSimulation() {
		log.debug(" -------------- finishSimulation ")
		log.debug("- call - shutdown executors");
		
		simulationData.setFinish(System.currentTimeMillis());
		if(simulationTimer != null) simulationTimer.cancel();
		if(broadcastTimer != null) broadcastTimer.cancel();
		
		if(simulatorExecutor != null) simulatorExecutor.shutdownNow();
		
		log.debug("------- SIMULATION RESULT - Event: " + event.getId());
		simulationData.setFinish(System.currentTimeMillis());
		simulationData.setFinish(System.currentTimeMillis());
				log.info("Begin: " + DateUtils.getStringFromDate(
				simulationData.getBeginDate())  + " - Duration: " +
				simulationData.getDurationStr());
		log.info("Number of projected requests: " +
				simulationData.getNumRequestsProjected());
		log.info("Number of completed requests: " + simulationData.getNumRequests());
		log.info("Number of signatures OK: " + simulationData.getNumRequestsOK());
		log.info("Number of signatures ERROR: " + simulationData.getNumRequestsERROR());
		
		if(!errorList.isEmpty()) {
			String errorsMsg = SimulationUtils.getFormattedErrorList(errorList);
			log.info(" ************* " + errorList.size() + " ERRORS: \n" + errorsMsg);
		}
		
		simulationData.statusCode = ResponseVS.SC_TERMINATED
		
		webSocketService.broadcastList(new JSONObject(simulationData.getDataMap()), simulationListener)
	}
	
	private void setNextEventState() throws Exception {
		log.debug("setNextEventState");
		Map cancelDataMap = event.getChangeEventDataMap(
			contextService.getAccessControl().getServerURL(), nextEventState);
		String cancelDataStr = new JSONObject(cancelDataMap).toString()
		String msgSubject = messageSource.getMessage("cancelEventMsgSubject", 
			[event.getId()].toArray(), locale);
		SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
				contextService.getUserTest().getKeyStore(),
				contextService.END_ENTITY_ALIAS,
				contextService.PASSWORD.toCharArray(),
				contextService.VOTE_SIGN_MECHANISM);
		SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(
				contextService.getUserTest().getEmail(),
				contextService.getAccessControl().getNombreNormalizado(),
				cancelDataStr, msgSubject,  null);
		SMIMESignedSender worker = new SMIMESignedSender(
				null, smimeDocument, contextService.getCancelEventURL(),
				null, null);
		ResponseVS responseVS = worker.call();
		if(ResponseVS.SC_OK != responseVS.getStatusCode())
			log.error(responseVS.getMessage());
	}
	
	private void requestBackup() throws Exception {
		log.debug("requestBackup");
		byte[] requestBackupPDFBytes = PdfFormHelper.getBackupRequest(
			event.getEventoId().toString(), event.getAsunto(),
							simulationData.getBackupRequestEmail());

		KeyStore userTestKeyStore = contextService.getUserTest().getKeyStore();
		PrivateKey signerPrivateKey = (PrivateKey)userTestKeyStore.getKey(
				ContextService.END_ENTITY_ALIAS, ContextService.PASSWORD.toCharArray());
		Certificate[] signerCertChain = userTestKeyStore.getCertificateChain(ContextService.END_ENTITY_ALIAS);
			
		PdfReader requestBackupPDF = new PdfReader(requestBackupPDFBytes);
		String urlBackupEvents = contextService.getAccessControl().getServerURL() + "/" + "solicitudCopia";
		
		PDFSignedSender worker = new PDFSignedSender(null, urlBackupEvents,
				null, null, null, requestBackupPDF, signerPrivateKey,
				signerCertChain, null);
		ResponseVS responseVS = worker.call();
		if(ResponseVS.SC_OK == respuesta.getStatusCode()) {
			String downloadServiceURL = contextService.getAccessControl().getServerURL() +  
				"/solicitudCopia/download/" + respuesta.getMessage();
			responseVS = contextService.getHttpHelper().getData(downloadServiceURL, "");
			if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
				log.debug("TODO validate backup");
				/*FutureTask<ResponseVS> future = new FutureTask<ResponseVS>(
					new ZipBackupValidator(respuesta.getMessageBytes()));
				simulatorExecutor.execute(future);
				respuesta = future.get();
				log.debug("BackupRequestWorker - status: " + respuesta.getStatusCode());*/
			} else log.error(responseVS.getMessage());
		} else log.error(responseVS.getMessage());
	}
	
	public void startSimulationTimer(SimulationData simulationData) throws Exception {
		log.debug("startSimulationTimer")
		Long hoursMillis = 1000 * 60 * 60 * new Long(
				simulationData.getNumHoursProjected());
		Long minutesMillis = 1000 * 60 * new Long(
				simulationData.getNumMinutesProjected());
		Long secondMillis = 1000 * new Long(
				simulationData.getNumSecondsProjected());
		Long totalMillis = hoursMillis + minutesMillis + secondMillis;
		Long interval = totalMillis/simulationData.getNumRequestsProjected();
		log.debug("starting timer - interval between requests: "
				+ interval + " milliseconds");
		simulationTimer = new Timer();
		simulationTimer.schedule(new SignTask(), 0, interval);
	}
	
	class SignTask extends TimerTask {
		public void run() {
			if(!signerList.isEmpty()) {
				int randomSigner = new Random().nextInt(signerList.size());
				try {
					launchSignature(signerList.remove(randomSigner));
				} catch (Exception ex) {
					log.error(ex.getMessage(), ex);
				}
			} else simulationTimer.stop();
		}
	}
	
	
	public void startBroadcatsTimer() throws Exception {
		Long broadcastInterval = 5000;
		log.debug("starting startBroadcastTimer - interval between broadcasts: '"
				+ broadcastInterval + "' milliseconds");
		broadcastTimer = new Timer();
		broadcastTimer.schedule(new BroadcastTimerTask(), 0, broadcastInterval);
	}
	
	class BroadcastTimerTask extends TimerTask {
		public void run() {
			//log.debug("======== BroadcastTimer run")
			Map broadcastResul = webSocketService.broadcastList(
				new JSONObject(simulationData?.getDataMap()), simulationListener);
			if(ResponseVS.SC_OK != broadcastResul.statusCode) {
				broadcastResul.errorList.each {
					simulationListener.remove(it)
				}
			}
		}
	}
	
	private void launchSignature(String nif) throws Exception {
		String reason = null;
		String location = null;
		String urlSignManifest = contextService.getAccessControl().getServerURL() + "/recolectorFirma/" + event.getId()
		PdfReader manifestToSign = new PdfReader(pdfBytes);
		signManifestCompletionService.submit(new ManifestSigner(nif,
			urlSignManifest, manifestToSign, reason, location));
		simulationData.getAndIncrementNumRequests();
	}
	
}