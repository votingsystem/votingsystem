package org.votingsystem.test.voting;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.callable.SMIMESignedSender;
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
    private static Timer simulationTimer;
    private static List synchronizedSignerList;
    private static ExecutorService simulatorExecutor;
    private static ExecutorCompletionService signClaimCompletionService;

    public static void main(String[] args) throws Exception {
        String publisherNIF = "00111222V";
        Map eventDataMap = new HashMap<>();
        eventDataMap.put("subject", "Claim subject");
        eventDataMap.put("content", "<p>Claim content</p>");
        eventDataMap.put("UUID", UUID.randomUUID().toString());
        eventDataMap.put("dateBegin", "2014/10/17 00:00:00");
        eventDataMap.put("dateFinish", "2014/11/25 00:00:00");
        eventDataMap.put("fieldsEventVS", Arrays.asList("field1", "field2"));

        Map simulationDataMap = new HashMap<>();
        simulationDataMap.put("accessControlURL", "http://sistemavotacion.org/AccessControl");
        simulationDataMap.put("maxPendingResponses", 10);
        simulationDataMap.put("numRequestsProjected", 2);
        simulationDataMap.put("whenFinishChangeEventStateTo", "");
        simulationDataMap.put("backupRequestEmail", "");
        simulationDataMap.put("event", eventDataMap);
        simulationDataMap.put("dateBeginDocument", "2014/10/17 00:00:00");
        simulationDataMap.put("dateFinishDocument", "2014/11/25 00:00:00");
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationDataMap.put("timer", timerMap);
        // whenFinishChangeEventStateTo: one of EventVS.State,
        log = TestUtils.init(Claim_publishAndSend.class, simulationDataMap);
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                TestUtils.getSimulationData().getAccessControlURL()),ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new org.votingsystem.throwable.ExceptionVS(responseVS.getMessage());

        Map<String, Object> dataMap = new ObjectMapper().readValue(
                responseVS.getMessage(), new TypeReference<HashMap<String, Object>>() {});
        ActorVS actorVS = ActorVS.parse(dataMap);
        if(!(actorVS instanceof AccessControlVS)) throw new org.votingsystem.throwable.ExceptionVS("Expected access control but found " + 
                actorVS.getType().toString());
        if(actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
            throw new org.votingsystem.throwable.ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
        }
        ContextVS.getInstance().setAccessControl((AccessControlVS) actorVS);
        eventVS = publishEvent(TestUtils.getSimulationData().getEventVS(), publisherNIF, "publishClaimMsgSubject");
        simulatorExecutor = Executors.newFixedThreadPool(100);
        sendClaims();
    }

    private static void sendClaims(){
        log.info("sendClaims");
        if(!(TestUtils.getSimulationData().getNumRequestsProjected() > 0)) {
            log.info("WITHOUT NumberOfRequestsProjected");
            return;
        }
        List<String> signerList = new ArrayList<String>();
        for(int i = 0; i < TestUtils.getSimulationData().getNumRequestsProjected(); i++) {
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
        log.info("sendRequests - NumRequestsProjected: " + TestUtils.getSimulationData().getNumRequestsProjected());
        if(TestUtils.getSimulationData().isTimerBased()) startSimulationTimer(TestUtils.getSimulationData());
        else {
            while(!synchronizedSignerList.isEmpty()) {
                if((TestUtils.getSimulationData().getNumRequests() - TestUtils.getSimulationData().getNumRequestsCollected()) <
                        TestUtils.getSimulationData().getMaxPendingResponses()) {
                    int randomSigner = new Random().nextInt(synchronizedSignerList.size());
                    launchSignature((String) synchronizedSignerList.remove(randomSigner));
                } else Thread.sleep(200);
            }
        }
    }

    public static void launchSignature(String nif) throws Exception {
        signClaimCompletionService.submit(new ClaimSignedSender(nif, eventVS.getId()));
        TestUtils.getSimulationData().getAndIncrementNumRequests();
    }

    private static void waitForResponses() throws Exception {
        log.info("waitForResponses - NumRequestsProjected: " + TestUtils.getSimulationData().getNumRequestsProjected());
        while (TestUtils.getSimulationData().getNumRequestsProjected() > TestUtils.getSimulationData().getNumRequestsCollected()) {
            try {
                Future<ResponseVS> f = signClaimCompletionService.take();
                ResponseVS responseVS = f.get();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    TestUtils.getSimulationData().getAndIncrementNumRequestsOK();
                } else TestUtils.finishWithError("ERROR", responseVS.getMessage(), TestUtils.getSimulationData().getNumRequestsOK());
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                TestUtils.finishWithError("EXCEPTION", ex.getMessage(), TestUtils.getSimulationData().getNumRequestsOK());
            }
        }
        TestUtils.finish("OK - Num. requests completed: " + TestUtils.getSimulationData().getNumRequestsOK());
    }

    private static EventVS publishEvent(EventVS eventVS, String publisherNIF, String smimeMessageSubject) throws Exception {
        log.info("publishEvent");
        eventVS.setSubject(eventVS.getSubject()+ " -> " + DateUtils.getDayWeekDateStr(new Date()));
        SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER);

        SMIMEMessage smimeMessage = signatureService.getSMIME(publisherNIF, ContextVS.getInstance().getAccessControl().getName(),
                new ObjectMapper().writeValueAsString(eventVS.getDataMap()), smimeMessageSubject);
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
        Map<String, Object> dataMap = new ObjectMapper().readValue(
                responseVS.getMessage(), new TypeReference<HashMap<String, Object>>() {});
        return EventVS.parse(dataMap);
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
                TestUtils.getSimulationData().getEventStateWhenFinished());
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