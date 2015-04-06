package org.votingsystem.test.voting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.json.ActorVSJSON;
import org.votingsystem.json.EventVSElectionJSON;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.callable.SignTask;
import org.votingsystem.test.callable.VoteSender;
import org.votingsystem.test.util.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Election_publishAndSend {

    private static Logger log;

    private static EventVS eventVS;
    private static boolean isWithVoteCancellation = false;
    private static String publisherNIF = "00111222V";
    private static List<String> synchronizedElectorList;
    private static ExecutorService simulatorExecutor;
    private static ExecutorCompletionService responseService;
    private static ExecutorCompletionService signClaimCompletionService;

    public static void main(String[] args) throws Exception {
        Map eventDataMap = new HashMap<>();
        eventDataMap.put("subject", "voting subject");
        eventDataMap.put("content", "<p>election content</p>");
        eventDataMap.put("UUID", UUID.randomUUID().toString());
        eventDataMap.put("fieldsEventVS", Arrays.asList("field1", "field2"));

        Map userBaseDataMap = new HashMap<>();
        userBaseDataMap.put("userIndex", 100);
        userBaseDataMap.put("numUsersWithoutRepresentative", 1);
        userBaseDataMap.put("numUsersWithoutRepresentativeWithVote", 1);
        userBaseDataMap.put("numRepresentatives", 2);
        userBaseDataMap.put("numRepresentativesWithVote",2);
        userBaseDataMap.put("numUsersWithRepresentative",10);
        userBaseDataMap.put("numUsersWithRepresentativeWithVote", 3);

        Map simulationDataMap = new HashMap<>();
        simulationDataMap.put("accessControlURL","http://localhost:8080/AccessControl");
        simulationDataMap.put("maxPendingResponses", 50);
        simulationDataMap.put("userBaseData", userBaseDataMap);
        simulationDataMap.put("whenFinishChangeEventStateTo", "");
        simulationDataMap.put("backupRequestEmail", "");
        simulationDataMap.put("event", eventDataMap);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationDataMap.put("timer", timerMap);

        log = TestUtils.init(Election_publishAndSend.class, VotingSimulationData.parse(simulationDataMap));
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                TestUtils.getSimulationData().getAccessControlURL()), ContentTypeVS.JSON);
        Map<String, Object> dataMap = new ObjectMapper().readValue(
                responseVS.getMessage(), new TypeReference<HashMap<String, Object>>() {});
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        ActorVS actorVS = ActorVS.parse(dataMap);
        if(!(actorVS instanceof AccessControlVS)) throw new ExceptionVS("Expected access control but found " +
                actorVS.getType().toString());
        if(actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
            throw new ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
        }
        ContextVS.getInstance().setAccessControl((AccessControlVS) actorVS);
        eventVS = publishEvent(TestUtils.getSimulationData().getEventVS(), publisherNIF, "publishElectionMsgSubject");
        simulatorExecutor = Executors.newFixedThreadPool(100);
        CountDownLatch userBaseDataLatch = new CountDownLatch(1);
        ((VotingSimulationData)TestUtils.getSimulationData()).getUserBaseData().sendData(userBaseDataLatch);
        userBaseDataLatch.await();
        sendVotes(((VotingSimulationData)TestUtils.getSimulationData()).getUserBaseData());
    }

    private static void sendVotes(UserBaseSimulationData userBaseSimulationData) throws Exception {
        log.info("sendVotes");
        synchronizedElectorList =  Collections.synchronizedList(userBaseSimulationData.getElectorList());
        if(synchronizedElectorList.isEmpty()) {
            /*if(TestUtils.getSimulationData().getBackupRequestEmail() != null) {
                try {
                    requestBackup(eventVS, publisherNIF);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }*/
            TestUtils.finish("Empty elector list");
        } else {
            simulatorExecutor = Executors.newFixedThreadPool(100);
            responseService = new ExecutorCompletionService<ResponseVS>(simulatorExecutor);
            ((VotingSimulationData)TestUtils.getSimulationData()).setNumOfElectors(
                    new Integer(synchronizedElectorList.size()).longValue());
            simulatorExecutor.execute(new Runnable() {@Override public void run() {
                try {
                    sendVoteRequests(userBaseSimulationData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } });
            simulatorExecutor.execute(new Runnable() { @Override public void run() {
                try {
                    waitForVoteResponses();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } });
        }
    }

    public static void sendVoteRequests(UserBaseSimulationData userBaseSimulationData) throws Exception {
        if(TestUtils.getSimulationData().isTimerBased()) startSimulationTimer(userBaseSimulationData);
        else {
            while(!synchronizedElectorList.isEmpty()) {
                if(!((VotingSimulationData)TestUtils.getSimulationData()).waitingForVoteRequests()) {
                    int randomElector = new Random().nextInt(synchronizedElectorList.size());
                    String electorNif = synchronizedElectorList.remove(randomElector);
                    responseService.submit(new VoteSender(VoteVS.genRandomVote(ContextVS.VOTING_DATA_DIGEST, eventVS),
                            electorNif));
                } else Thread.sleep(500);
            }
        }
    }

    private static void waitForVoteResponses() throws Exception {
        VotingSimulationData simulationData = ((VotingSimulationData)TestUtils.getSimulationData());
        log.info("waitForVoteResponses - Num. votes: " + simulationData.getNumOfElectors());
        while (simulationData.hasPendingVotes()) {
            try {
                Future<ResponseVS> f = responseService.take();
                ResponseVS responseVS = f.get();
                String nifFrom = null;
                if(responseVS.getData() != null) nifFrom = ((UserVS)((Map)responseVS.getData()).get("userVS")).getNif();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    VoteVS voteReceipt = (VoteVS) ((Map)responseVS.getData()).get("voteVS");
                    if(isWithVoteCancellation) cancelVote(voteReceipt, nifFrom);
                    simulationData.getAndIncrementNumVotingRequestsOK();
                } else TestUtils.finishWithError("ERROR", responseVS.getMessage(),
                        TestUtils.getSimulationData().getNumRequestsOK());
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                TestUtils.finishWithError("EXCEPTION", ex.getMessage(), TestUtils.getSimulationData().getNumRequestsOK());
            }
        }
        if(simulationData.getEventStateWhenFinished() != null) changeEventState(publisherNIF);
        //if(simulationData.getBackupRequestEmail() != null) requestBackup(eventVS, publisherNIF);
        TestUtils.finish("Num. votes: " + simulationData.getNumOfElectors());
    }

    private static EventVS publishEvent(EventVS eventVS, String publisherNIF, String smimeMessageSubject) throws Exception {
        log.info("publishEvent");
        eventVS.setDateBegin(new Date());
        eventVS.setSubject(eventVS.getSubject()+ " -> " + DateUtils.getDayWeekDateStr(new Date()));
        SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER);
        SMIMEMessage smimeMessage = signatureService.getSMIME(publisherNIF,
                ContextVS.getInstance().getAccessControl().getName(), new ObjectMapper().writeValueAsString(
                new EventVSElectionJSON(eventVS)), smimeMessageSubject);
        SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage,
                ContextVS.getInstance().getAccessControl().getPublishElectionURL(),
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null, null,
                "eventURL");
        ResponseVS responseVS = signedSender.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        String eventURL = ((List<String>)responseVS.getData()).iterator().next();
        byte[] responseBytes = responseVS.getMessageBytes();
        ContextVS.getInstance().copyFile(responseBytes, "/electionSimulation", "ElectionPublishedReceipt");
        SMIMEMessage dnieMimeMessage = new SMIMEMessage(new ByteArrayInputStream(responseBytes));
        responseVS = HttpHelper.getInstance().getData(eventURL, ContentTypeVS.JSON);
        Map<String, Object> dataMap = new ObjectMapper().readValue(
                responseVS.getMessage(), new TypeReference<HashMap<String, Object>>() {});
        return EventVS.parse(dataMap);
    }

    public static void startSimulationTimer(SimulationData simulationData) throws Exception {
        Long interval = simulationData.getDurationInMillis()/simulationData.getNumRequestsProjected();
        log.info("startSimulationTimer - interval between requests: " + interval + " milliseconds");
        Timer simulationTimer = new Timer();
        simulationTimer.schedule(new SignTask(simulationTimer, synchronizedElectorList, null), 0, interval);
    }

    private static void changeEventState(String publisherNIF) throws Exception {
        log.info("changeEventState");
        Map cancelDataMap = eventVS.getChangeEventDataMap(ContextVS.getInstance().getAccessControl().getServerURL(),
                TestUtils.getSimulationData().getEventStateWhenFinished());
        String smimeMessageSubject = "cancelEventMsgSubject";
        SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER);
        SMIMEMessage smimeMessage = signatureService.getSMIME(publisherNIF, ContextVS.getInstance().getAccessControl().
                getName(), new ObjectMapper().writeValueAsString(cancelDataMap), smimeMessageSubject);
        SMIMESignedSender worker = new SMIMESignedSender(smimeMessage,
                ContextVS.getInstance().getAccessControl().getCancelEventServiceURL(),
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED, null, null);
        ResponseVS responseVS = worker.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
    }


    private static void cancelVote(VoteVS voteVS, String nif) throws Exception {
        Map cancelDataMap = new HashMap<String, String>();
        cancelDataMap.put("operation", TypeVS.CANCEL_VOTE.toString());
        cancelDataMap.put("originHashAccessRequest", voteVS.getOriginHashAccessRequest());
        cancelDataMap.put("hashAccessRequestBase64", voteVS.getAccessRequestHashBase64());
        cancelDataMap.put("originHashCertVote", voteVS.getOriginHashCertVote());
        cancelDataMap.put("hashCertVSBase64", voteVS.getHashCertVSBase64());
        cancelDataMap.put("UUID", UUID.randomUUID().toString());
        SignatureService signatureService = SignatureService.getUserVSSignatureService(nif, UserVS.Type.USER);
        SMIMEMessage smimeMessage = signatureService.getSMIME(nif, ContextVS.getInstance().getAccessControl().getName(),
                new ObjectMapper().writeValueAsString(cancelDataMap), "cancelVote");
        SMIMESignedSender worker = new SMIMESignedSender(smimeMessage,
                ContextVS.getInstance().getAccessControl().getVoteCancelerServiceURL(),
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED, null, null);
        ResponseVS responseVS = worker.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
    }
}



