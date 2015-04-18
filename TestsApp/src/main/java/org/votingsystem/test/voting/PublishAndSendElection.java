package org.votingsystem.test.voting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.voting.EventVSChangeDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.FieldEventVS;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.callable.SignTask;
import org.votingsystem.test.callable.VoteSender;
import org.votingsystem.test.dto.VoteResultDto;
import org.votingsystem.test.util.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PublishAndSendElection {

    private static Logger log;

    private static EventVS eventVS;
    private static VotingSimulationData simulationData;
    private static boolean isWithVoteCancellation = false;
    private static String publisherNIF = "00111222V";
    private static List<String> synchronizedElectorList;
    private static ExecutorService simulatorExecutor;
    private static ExecutorCompletionService responseService;
    private static ExecutorCompletionService signClaimCompletionService;

    public static void main(String[] args) throws Exception {
        eventVS = new EventVS();
        eventVS.setSubject("voting subject");
        eventVS.setContent("<p>election content</p>");
        eventVS.setDateBegin(new Date());
        eventVS.setFieldsEventVS(new HashSet<>(Arrays.asList(new FieldEventVS("field1", null), new FieldEventVS("field2", null))));

        UserBaseSimulationData userBaseSimulationData = new UserBaseSimulationData();
        userBaseSimulationData.setUserIndex(200);
        userBaseSimulationData.setNumUsersWithoutRepresentative(10);
        userBaseSimulationData.setNumUsersWithoutRepresentativeWithVote(8);
        userBaseSimulationData.setNumRepresentatives(3);
        userBaseSimulationData.setNumRepresentativesWithVote(2);
        userBaseSimulationData.setNumUsersWithRepresentative(10);
        userBaseSimulationData.setNumUsersWithRepresentativeWithVote(2);

        simulationData = new VotingSimulationData();
        simulationData.setServerURL("http://localhost:8080/AccessControl");
        simulationData.setMaxPendingResponses(50);
        simulationData.setUserBaseData(userBaseSimulationData);
        //simulationData.setEventStateWhenFinished();
        //simulationData.setBackupRequestEmail();
        simulationData.setEventVS(eventVS);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);
        log = TestUtils.init(PublishAndSendElection.class, simulationData);
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                simulationData.getServerURL()), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        ActorVS actorVS = ((ActorVSDto)responseVS.getDto(ActorVSDto.class)).getActorVS();
        if(!(actorVS instanceof AccessControlVS)) throw new ExceptionVS("Expected access control but found " +
                actorVS.getType().toString());
        if(actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
            throw new ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
        }
        ContextVS.getInstance().setAccessControl((AccessControlVS) actorVS);
        //EventVSJSON eventVSJSON = publishEvent(simulationData.getEventVS(), publisherNIF, "publishElectionMsgSubject");
        eventVS = publishEvent(simulationData.getEventVS(), publisherNIF, "publishElectionMsgSubject");
        simulatorExecutor = Executors.newFixedThreadPool(100);
        CountDownLatch userBaseDataLatch = new CountDownLatch(1);
        simulationData.getUserBaseData().sendData(userBaseDataLatch);
        userBaseDataLatch.await();
        sendVotes(((VotingSimulationData)simulationData).getUserBaseData());
    }

    private static void sendVotes(UserBaseSimulationData userBaseSimulationData) throws Exception {
        log.info("sendVotes");
        synchronizedElectorList =  Collections.synchronizedList(userBaseSimulationData.getElectorList());
        if(synchronizedElectorList.isEmpty()) {
            /*if(simulationData.getBackupRequestEmail() != null) {
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
            ((VotingSimulationData)simulationData).setNumOfElectors(
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
        if(simulationData.isTimerBased()) startSimulationTimer(userBaseSimulationData);
        else {
            while(!synchronizedElectorList.isEmpty()) {
                if(!((VotingSimulationData)simulationData).waitingForVoteRequests()) {
                    int randomElector = new Random().nextInt(synchronizedElectorList.size());
                    String electorNif = synchronizedElectorList.remove(randomElector);
                    responseService.submit(new VoteSender(VoteVS.genRandomVote(ContextVS.VOTING_DATA_DIGEST, eventVS),
                            electorNif));
                } else Thread.sleep(500);
            }
        }
    }

    private static void waitForVoteResponses() throws Exception {
        log.info("waitForVoteResponses - Num. votes: " + simulationData.getNumOfElectors());
        while (simulationData.hasPendingVotes()) {
            try {
                Future<ResponseVS<VoteResultDto>> f = responseService.take();
                ResponseVS<VoteResultDto> responseVS = f.get();
                String nifFrom = null;
                if(responseVS.getData() != null) nifFrom = responseVS.getData().getElectorNIF();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    VoteVS voteReceipt = responseVS.getData().getVoteVS();
                    if(isWithVoteCancellation) cancelVote(voteReceipt, nifFrom);
                    simulationData.getAndIncrementNumVotingRequestsOK();
                } else TestUtils.finishWithError("ERROR", responseVS.getMessage(),
                        simulationData.getNumRequestsOK());
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                TestUtils.finishWithError("EXCEPTION", ex.getMessage(), simulationData.getNumRequestsOK());
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
                        new EventVSDto(eventVS)), smimeMessageSubject);
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
        EventVSDto eventVSJSON = JSON.getMapper().readValue(responseVS.getMessage(), EventVSDto.class);
        return eventVSJSON.getEventVSElection();
    }

    public static void startSimulationTimer(SimulationData simulationData) throws Exception {
        Long interval = simulationData.getDurationInMillis()/simulationData.getNumRequestsProjected();
        log.info("startSimulationTimer - interval between requests: " + interval + " milliseconds");
        Timer simulationTimer = new Timer();
        simulationTimer.schedule(new SignTask(simulationTimer, synchronizedElectorList, null), 0, interval);
    }

    private static void changeEventState(String publisherNIF) throws Exception {
        log.info("changeEventState");
        EventVSChangeDto cancelData = new EventVSChangeDto(eventVS, ContextVS.getInstance().getAccessControl().getServerURL(),
                TypeVS.EVENT_CANCELLATION, simulationData.getEventStateWhenFinished());
        String smimeMessageSubject = "cancelEventMsgSubject";
        SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER);
        SMIMEMessage smimeMessage = signatureService.getSMIME(publisherNIF, ContextVS.getInstance().getAccessControl().
                getName(), new ObjectMapper().writeValueAsString(cancelData), smimeMessageSubject);
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
        cancelDataMap.put("hashAccessRequestBase64", voteVS.getHashAccessRequestBase64());
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



