package org.votingsystem.test.voting

import com.itextpdf.text.pdf.PdfReader
import net.sf.json.JSONSerializer
import org.votingsystem.callable.PDFSignedSender
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.callable.ClaimSignedSender
import org.votingsystem.test.callable.SignTask
import org.votingsystem.test.callable.VoteSender
import org.votingsystem.test.model.SimulationData
import org.votingsystem.test.model.UserBaseSimulationData
import org.votingsystem.test.model.VotingSimulationData
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.*

import java.security.KeyStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future

publisherNIF = "00111222V"
Map eventDataMap = [subject:"Claim subject", content:"<p>Election content</p>", UUID:UUID.randomUUID().toString(),
                    dateBegin:"2014/10/17 00:00:00", dateFinish:"2014/10/22 00:00:00",  fieldsEventVS:["field1", "field2"]]

Map userBaseDataMap = [userIndex:100, numUsersWithoutRepresentative:1, numUsersWithoutRepresentativeWithVote:1,
                       numRepresentatives:0, numRepresentativesWithVote:0,
                       numUsersWithRepresentative:0, numUsersWithRepresentativeWithVote:0]

// whenFinishChangeEventStateTo: one of EventVS.State,
Map simulationDataMap = [accessControlURL:"http://sistemavotacion.org/AccessControl", maxPendingResponses:10,
                         userBaseData:userBaseDataMap, whenFinishChangeEventStateTo:"CANCELLED",
                         backupRequestEmail:"", event:eventDataMap,
                         dateBeginDocument:"2014/10/17 00:00:00", dateFinishDocument:"2014/10/19 00:00:00",
                         timer:[active:false, time:"00:00:10"]]


log = TestUtils.init(Election_publishAndSend.class, VotingSimulationData.parse(JSONSerializer.toJSON(simulationDataMap)))

ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
        TestUtils.simulationData.getAccessControlURL()),ContentTypeVS.JSON);
if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
ActorVS actorVS = ActorVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
if(!(actorVS instanceof AccessControlVS)) throw new ExceptionVS("Expected access control but found " + actorVS.getType().toString());
if(actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
    throw new ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
}
ContextVS.getInstance().setAccessControl(actorVS);
eventVS = publishEvent(TestUtils.simulationData.getEventVS(), publisherNIF, "publishElectionMsgSubject");
simulatorExecutor = Executors.newFixedThreadPool(100);

CountDownLatch userBaseDataLatch = new CountDownLatch(1);

((VotingSimulationData)TestUtils.simulationData).userBaseData.sendData(userBaseDataLatch)

userBaseDataLatch.await()

sendVotes(((VotingSimulationData)TestUtils.simulationData).userBaseData)

private void sendVotes(UserBaseSimulationData userBaseSimulationData){
    log.debug("sendVotes");
    synchronizedElectorList =  Collections.synchronizedList(userBaseSimulationData.getElectorList());
    if(synchronizedElectorList.isEmpty()) {
        if(TestUtils.simulationData.getBackupRequestEmail() != null) {requestBackup(eventVS, publisherNIF)}
        else TestUtils.finish("Empty elector list")
    } else {
        simulatorExecutor = Executors.newFixedThreadPool(100);
        responseService = new ExecutorCompletionService<ResponseVS>(simulatorExecutor);
        ((VotingSimulationData)TestUtils.simulationData).setNumOfElectors(
                new Integer(synchronizedElectorList.size()).longValue())
        simulatorExecutor.execute(new Runnable() {@Override public void run() {
            sendVoteRequests(userBaseSimulationData); } });
        simulatorExecutor.execute(new Runnable() { @Override public void run() { waitForVoteResponses(); } });
    }
}

public void sendVoteRequests (UserBaseSimulationData userBaseSimulationData) throws Exception {
    if(TestUtils.simulationData.isTimerBased()) startSimulationTimer(userBaseSimulationData);
    else {
        while(!synchronizedElectorList.isEmpty()) {
            if(!((VotingSimulationData)TestUtils.simulationData).waitingForVoteRequests()) {
                int randomElector = new Random().nextInt(synchronizedElectorList.size());
                String electorNif = synchronizedElectorList.remove(randomElector)
                responseService.submit(new VoteSender(VoteVS.genRandomVote(ContextVS.VOTING_DATA_DIGEST, eventVS),
                        electorNif));
            } else Thread.sleep(500);
        }
    }
}

private void waitForVoteResponses() throws Exception {
    VotingSimulationData simulationData = ((VotingSimulationData)TestUtils.simulationData)
    log.debug("waitForVoteResponses - Num. votes: " + simulationData.getNumOfElectors());
    while (simulationData.hasPendingVotes()) {
        try {
            String nifFrom = null;
            Future<ResponseVS> f = responseService.take();
            ResponseVS responseVS = f.get();
            nifFrom = responseVS.getData()?.userVS?.getNif();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                VoteVS voteReceipt = responseVS.getData().voteVS;
                simulationData.getAndIncrementNumVotingRequestsOK();
            } else TestUtils.finishWithError("ERROR", responseVS.getMessage(), TestUtils.simulationData.getNumRequestsOK())
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
            TestUtils.finishWithError("EXCEPTION", ex.getMessage(), TestUtils.simulationData.getNumRequestsOK())
        }
    }
    if(simulationData.getEventStateWhenFinished() != null) changeEventState(publisherNIF);
    else if(simulationData.getBackupRequestEmail() != null) requestBackup(eventVS, publisherNIF);
    TestUtils.finish("Num. votes: " + simulationData.getNumOfElectors());
}

private EventVS publishEvent(EventVS eventVS, String publisherNIF, String smimeMessageSubject) throws Exception {
    log.debug("publishEvent");
    eventVS.setSubject(eventVS.getSubject()+ " -> " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()));
    SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER)
    SMIMEMessage smimeMessage = signatureService.getTimestampedSignedMimeMessage(publisherNIF,
            ContextVS.getInstance().getAccessControl().getNameNormalized(),
            JSONSerializer.toJSON(eventVS.getDataMap()).toString(), smimeMessageSubject)
    SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage,
            ContextVS.getInstance().getAccessControl().getPublishElectionURL(),
            ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null, null,
            "eventURL");
    ResponseVS responseVS = signedSender.call();
    if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
    String eventURL = ((List<String>)responseVS.getData()).iterator().next()
    byte[] responseBytes = responseVS.getMessageBytes();
    ContextVS.getInstance().copyFile(responseBytes, "/electionSimulation", "ElectionPublishedReceipt")
    SMIMEMessage dnieMimeMessage = new SMIMEMessage(new ByteArrayInputStream(responseBytes));
    //dnieMimeMessage.verify(ContextVS.getInstance().getSessionPKIXParameters());
    responseVS = HttpHelper.getInstance().getData(eventURL, ContentTypeVS.JSON);
    return EventVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
}

public void startSimulationTimer(SimulationData simulationData) throws Exception {
    Long interval = simulationData.getDurationInMillis()/simulationData.getNumRequestsProjected();
    log.debug("startSimulationTimer - interval between requests: "+ interval + " milliseconds");
    simulationTimer = new Timer();
    simulationTimer.schedule(new SignTask(simulationTimer, synchronizedSignerList, this), 0, interval);
}

private void changeEventState(String publisherNIF) throws Exception {
    log.debug("changeEventState");
    Map cancelDataMap = eventVS.getChangeEventDataMap(ContextVS.getInstance().getAccessControl().getServerURL(),
            TestUtils.simulationData.getEventStateWhenFinished());
    String smimeMessageSubject = "cancelEventMsgSubject"
    SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER)
    SMIMEMessage smimeMessage = signatureService.getTimestampedSignedMimeMessage(publisherNIF,
            ContextVS.getInstance().getAccessControl().getNameNormalized(),
            JSONSerializer.toJSON(cancelDataMap).toString(), smimeMessageSubject)
    SMIMESignedSender worker = new SMIMESignedSender(smimeMessage,
            ContextVS.getInstance().getAccessControl().getCancelEventServiceURL(),
            ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
            ContentTypeVS.JSON_SIGNED, null, null);
    ResponseVS responseVS = worker.call();
    if(ResponseVS.SC_OK != responseVS.statusCode) throw new ExceptionVS(responseVS.getMessage())
}

private void requestBackup(EventVS eventVS, String nif) throws Exception {
    log.debug("requestBackup");
    byte[] requestBackupPDFBytes = PdfFormHelper.getBackupRequest(eventVS.getId().toString(), eventVS.getSubject(),
            TestUtils.simulationData.getBackupRequestEmail());
    PdfReader requestBackupPDF = new PdfReader(requestBackupPDFBytes);
    SignatureService signatureService = SignatureService.getUserVSSignatureService(nif, UserVS.Type.USER)
    String urlBackupEvents = ContextVS.getInstance().getAccessControl().getBackupServiceURL();
    PDFSignedSender worker = new PDFSignedSender(urlBackupEvents,
            ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(), null, null, null,
            requestBackupPDF, signatureService.getPrivateKey(), signatureService.getCertSignerChain(), null);
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
    } else throw new ExceptionVS(responseVS.getMessage())
}