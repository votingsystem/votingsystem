package org.votingsystem.test.voting;

import com.google.common.collect.Sets;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.voting.EventVSChangeDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.FieldEventVS;
import org.votingsystem.test.callable.SignTask;
import org.votingsystem.test.callable.VoteSender;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.test.util.UserBaseSimulationData;
import org.votingsystem.test.util.VotingSimulationData;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.VoteHelper;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PublishAndSendElection {

    private static Logger log =  Logger.getLogger(PublishAndSendElection.class.getName());

    private static EventVS eventVS;
    private static VotingSimulationData simulationData;
    private static boolean isWithVoteCancellation = false;
    private static String publisherNIF = "00111222V";
    private static List<String> synchronizedElectorList;
    private static ExecutorService simulatorExecutor;
    private static ExecutorCompletionService responseService;
    private static final Map<String, VoteHelper> voteMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        eventVS = new EventVS();
        eventVS.setSubject("voting subject");
        eventVS.setContent(Base64.getEncoder().encodeToString("<p>election content</p>".getBytes()));
        eventVS.setDateBegin(new Date());
        eventVS.setFieldsEventVS(Sets.newHashSet(new FieldEventVS("field1", null), new FieldEventVS("field2", null)));

        UserBaseSimulationData userBaseSimulationData = new UserBaseSimulationData();
        userBaseSimulationData.setUserIndex(100);
        userBaseSimulationData.setNumUsersWithoutRepresentative(10);
        userBaseSimulationData.setNumUsersWithoutRepresentativeWithVote(10);
        userBaseSimulationData.setNumRepresentatives(5);
        userBaseSimulationData.setNumRepresentativesWithVote(1);
        userBaseSimulationData.setNumUsersWithRepresentative(0);
        userBaseSimulationData.setNumUsersWithRepresentativeWithVote(0);

        simulationData = new VotingSimulationData();
        simulationData.setServerURL("https://192.168.1.5/AccessControl");
        simulationData.setMaxPendingResponses(50);
        simulationData.setUserBaseData(userBaseSimulationData);
        //simulationData.setEventStateWhenFinished();
        //simulationData.setBackupRequestEmail();
        simulationData.setEventVS(eventVS);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                simulationData.getServerURL()), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        ActorVS actorVS = ((ActorVSDto)responseVS.getMessage(ActorVSDto.class)).getActorVS();
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
            simulationData.finishAndExit(ResponseVS.SC_OK, null);
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
                if(!simulationData.waitingForVoteRequests()) {
                    int randomElector = new Random().nextInt(synchronizedElectorList.size());
                    String electorNIF = synchronizedElectorList.remove(randomElector);
                    VoteHelper voteHelper = VoteHelper.genRandomVote(eventVS.getId(), eventVS.getUrl(), eventVS.getFieldsEventVS());
                    voteHelper.setNIF(electorNIF);
                    voteMap.put(voteHelper.getHashCertVSBase64(), voteHelper);
                    responseService.submit(new VoteSender(voteHelper));
                } else Thread.sleep(500);
            }
        }
    }

    private static void waitForVoteResponses() throws Exception {
        log.info("waitForVoteResponses - Num. votes: " + simulationData.getNumOfElectors());
        while (simulationData.hasPendingVotes()) {
            try {
                Future<ResponseVS<VoteHelper>> f = responseService.take();
                ResponseVS<VoteHelper> responseVS = f.get();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    VoteHelper voteHelper = f.get().getData();
                    if(isWithVoteCancellation) cancelVote(voteMap.get(voteHelper.getHashCertVSBase64()),
                            voteHelper.getNIF());
                    simulationData.getAndIncrementNumVotingRequestsOK();
                } else simulationData.finishAndExit(ResponseVS.SC_ERROR, "ERROR" + responseVS.getMessage());
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                simulationData.finishAndExit(ResponseVS.SC_ERROR, "EXCEPTION: " + ex.getMessage());
            }
        }
        if(simulationData.getEventStateWhenFinished() != null) changeEventState(publisherNIF);
        //if(simulationData.getBackupRequestEmail() != null) requestBackup(eventVS, publisherNIF);
        simulationData.finishAndExit(ResponseVS.SC_OK, null);
    }

    private static EventVS publishEvent(EventVS eventVS, String publisherNIF, String cmsMessageSubject) throws Exception {
        log.info("publishEvent");
        eventVS.setDateBegin(new Date());
        eventVS.setSubject(eventVS.getSubject()+ " -> " + DateUtils.getDayWeekDateStr(new Date(), "HH:mm:ss"));
        SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER);
        CMSSignedMessage cmsMessage = signatureService.signData(JSON.getMapper().writeValueAsString(
                new EventVSDto(eventVS)));
        cmsMessage = new MessageTimeStamper(cmsMessage,
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
        ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                ContextVS.getInstance().getAccessControl().getPublishElectionURL(), "eventURL");
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        String eventURL = ((List<String>)responseVS.getData()).iterator().next();
        ContextVS.getInstance().copyFile(responseVS.getMessageBytes(), "/electionSimulation", "ElectionPublishedReceipt");
        CMSSignedMessage message = responseVS.getCMS();
        responseVS = HttpHelper.getInstance().getData(eventURL, ContentTypeVS.JSON);
        EventVSDto eventVSJSON = JSON.getMapper().readValue(responseVS.getMessage(), EventVSDto.class);
        return eventVSJSON.getEventElection();
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
        SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER);
        CMSSignedMessage cmsMessage = signatureService.signData(JSON.getMapper().writeValueAsString(cancelData));
        cmsMessage = new MessageTimeStamper(cmsMessage,
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
        ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                ContextVS.getInstance().getAccessControl().getCancelEventServiceURL());
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
    }


    private static void cancelVote(VoteHelper voteHelper, String nif) throws Exception {
        SignatureService signatureService = SignatureService.getUserVSSignatureService(nif, UserVS.Type.USER);
        CMSSignedMessage cmsMessage = signatureService.signData(JSON.getMapper().writeValueAsString(
                voteHelper.getVoteCanceler()));
        cmsMessage = new MessageTimeStamper(cmsMessage, ContextVS.getInstance().getAccessControl()
                .getTimeStampServiceURL()).call();
        ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                ContextVS.getInstance().getAccessControl().getVoteCancelerServiceURL());
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
    }

}



