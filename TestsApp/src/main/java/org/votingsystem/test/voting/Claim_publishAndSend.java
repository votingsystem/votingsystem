package org.votingsystem.test.voting;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.EventVSDto;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.callable.ClaimSignedSender;
import org.votingsystem.test.callable.SignTask;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.*;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Claim_publishAndSend {

    private static Logger log;
    
    private static EventVS eventVS;
    private static SimulationData simulationData;
    private static Timer simulationTimer;
    private static List synchronizedSignerList;
    private static ExecutorService simulatorExecutor;
    private static ExecutorCompletionService signClaimCompletionService;

    public static void main(String[] args) throws Exception {
        String publisherNIF = "00111222V";
        eventVS = new EventVS();
        eventVS.setSubject("Claim subject");
        eventVS.setContent("<p>Claim content</p>");
        eventVS.setDateBegin(new Date());
        eventVS.setFieldsEventVS(new HashSet<>(Arrays.asList(new FieldEventVS("field1", null), new FieldEventVS("field2", null))));
        simulationData = new SimulationData();
        simulationData.setAccessControlURL("http://sistemavotacion.org/AccessControl");
        simulationData.setMaxPendingResponses(10);
        simulationData.setNumRequestsProjected(2);
        //simulationData.setEventStateWhenFinished();
        //simulationData.setBackupRequestEmail();
        simulationData.setEventVS(eventVS);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);
        log = TestUtils.init(Claim_publishAndSend.class, simulationData);
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                simulationData.getAccessControlURL()),ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new org.votingsystem.throwable.ExceptionVS(responseVS.getMessage());
        ActorVS actorVS = ((ActorVSDto)responseVS.getDto(ActorVSDto.class)).getActorVS();
        if(!(actorVS instanceof AccessControlVS)) throw new org.votingsystem.throwable.ExceptionVS("Expected access control but found " + 
                actorVS.getType().toString());
        if(actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
            throw new org.votingsystem.throwable.ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
        }
        ContextVS.getInstance().setAccessControl((AccessControlVS) actorVS);
        eventVS = publishEvent(simulationData.getEventVS(), publisherNIF, "publishClaimMsgSubject");
        simulatorExecutor = Executors.newFixedThreadPool(100);
        sendClaims();
    }

    private static void sendClaims(){
        log.info("sendClaims");
        if(!(simulationData.getNumRequestsProjected() > 0)) {
            log.info("WITHOUT NumberOfRequestsProjected");
            return;
        }
        List<String> signerList = new ArrayList<String>();
        for(int i = 0; i < simulationData.getNumRequestsProjected(); i++) {
            signerList.add(NifUtils.getNif(i));
        }
        synchronizedSignerList = Collections.synchronizedList(signerList);
        signClaimCompletionService = new ExecutorCompletionService<ResponseVS>(simulatorExecutor);
        simulatorExecutor.execute(new Runnable() {
            @Override public void run() {
                try {
                    sendRequests();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        simulatorExecutor.execute(new Runnable() {
            @Override public void run() {
                try {
                    waitForResponses();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void sendRequests() throws Exception {
        log.info("sendRequests - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        if(simulationData.isTimerBased()) startSimulationTimer(simulationData);
        else {
            while(!synchronizedSignerList.isEmpty()) {
                if((simulationData.getNumRequests() - simulationData.getNumRequestsCollected()) <
                        simulationData.getMaxPendingResponses()) {
                    int randomSigner = new Random().nextInt(synchronizedSignerList.size());
                    launchSignature((String) synchronizedSignerList.remove(randomSigner));
                } else Thread.sleep(200);
            }
        }
    }

    public static void launchSignature(String nif) throws Exception {
        signClaimCompletionService.submit(new ClaimSignedSender(nif, eventVS.getId()));
        simulationData.getAndIncrementNumRequests();
    }

    private static void waitForResponses() throws Exception {
        log.info("waitForResponses - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        while (simulationData.getNumRequestsProjected() > simulationData.getNumRequestsCollected()) {
            try {
                Future<ResponseVS> f = signClaimCompletionService.take();
                ResponseVS responseVS = f.get();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    simulationData.getAndIncrementNumRequestsOK();
                } else TestUtils.finishWithError("ERROR", responseVS.getMessage(), simulationData.getNumRequestsOK());
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                TestUtils.finishWithError("EXCEPTION", ex.getMessage(), simulationData.getNumRequestsOK());
            }
        }
        TestUtils.finish("OK - Num. requests completed: " + simulationData.getNumRequestsOK());
    }

    private static EventVS publishEvent(EventVS eventVS, String publisherNIF, String smimeMessageSubject) throws Exception {
        log.info("publishEvent");
        eventVS.setSubject(eventVS.getSubject()+ " -> " + DateUtils.getDayWeekDateStr(new Date()));
        SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER);

        SMIMEMessage smimeMessage = signatureService.getSMIME(publisherNIF, ContextVS.getInstance().getAccessControl().getName(),
                new ObjectMapper().writeValueAsString(EventVSDto.formatToSign(eventVS)), smimeMessageSubject);
        SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage,
                ContextVS.getInstance().getAccessControl().getPublishClaimURL(),
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null, null,
                "eventURL");
        ResponseVS responseVS = signedSender.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new org.votingsystem.throwable.ExceptionVS(responseVS.getMessage());
        String eventURL = ((List<String>)responseVS.getData()).iterator().next();
        byte[] responseBytes = responseVS.getMessageBytes();
        ContextVS.getInstance().copyFile(responseBytes, "/claimSimulation", "ClaimPublishedReceipt");
        SMIMEMessage dnieMimeMessage = new SMIMEMessage(new ByteArrayInputStream(responseBytes));
        responseVS = HttpHelper.getInstance().getData(eventURL, ContentTypeVS.JSON);
        eventVS = new ObjectMapper().readValue(responseVS.getMessage(), EventVS.class);
        return eventVS;
    }


    public static void startSimulationTimer(SimulationData simulationData) throws Exception {
        Long interval = simulationData.getDurationInMillis()/simulationData.getNumRequestsProjected();
        log.info("startSimulationTimer - interval between requests: " + interval + " milliseconds");
        simulationTimer = new  Timer();
        SignTask.Launcher launcher = new SignTask.Launcher() {
            @Override public void processTask(String param) {
                try {
                    launchSignature(param);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        simulationTimer.schedule(new SignTask(simulationTimer, synchronizedSignerList, launcher), 0, interval);
    }

    private void changeEventState(String publisherNIF) throws Exception {
        log.info("changeEventState");
        Map cancelDataMap = eventVS.getChangeEventDataMap(ContextVS.getInstance().getAccessControl().getServerURL(),
                simulationData.getEventStateWhenFinished());
        String smimeMessageSubject ="cancelEventMsgSubject";
        SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER);
        SMIMEMessage smimeMessage = signatureService.getSMIME(publisherNIF, ContextVS.getInstance().getAccessControl().
                getName(), new ObjectMapper().writeValueAsString(cancelDataMap), smimeMessageSubject);
        SMIMESignedSender worker = new SMIMESignedSender(smimeMessage,
                ContextVS.getInstance().getAccessControl().getCancelEventServiceURL(),
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED, null, null);
        ResponseVS responseVS = worker.call();
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) throw new org.votingsystem.throwable.ExceptionVS(responseVS.getMessage());
    }

}