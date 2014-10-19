package org.votingsystem.test.voting

import com.itextpdf.text.pdf.PdfReader
import net.sf.json.JSONSerializer
import org.votingsystem.callable.PDFSignedSender
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.callable.ClaimSignedSender
import org.votingsystem.test.callable.SignTask
import org.votingsystem.test.model.SimulationData
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.*

import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future

String publisherNIF = "00111222V"
Map eventDataMap = [subject:"Claim subject", content:"<p>Claim content</p>", UUID:UUID.randomUUID().toString(),
        dateBegin:"2014/10/17 00:00:00", dateFinish:"2014/10/25 00:00:00",  fieldsEventVS:["field1", "field2"]]

// whenFinishChangeEventStateTo: one of EventVS.State,
Map simulationDataMap = [accessControlURL:"http://sistemavotacion.org/AccessControl", maxPendingResponses:10,
                      numRequestsProjected:2, whenFinishChangeEventStateTo:"",backupRequestEmail:"", event:eventDataMap,
                      dateBeginDocument:"2014/10/17 00:00:00", dateFinishDocument:"2014/10/25 00:00:00",
                      timer:[active:true, time:"00:00:10"]]

log = TestUtils.init(Claim_publishAndSend.class, simulationDataMap)

ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
        TestUtils.simulationData.getAccessControlURL()),ContentTypeVS.JSON);
if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
ActorVS actorVS = ActorVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
if(!(actorVS instanceof AccessControlVS)) throw new ExceptionVS("Expected access control but found " + actorVS.getType().toString());
if(actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
    throw new ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
}
ContextVS.getInstance().setAccessControl(actorVS);
eventVS = publishEvent(TestUtils.simulationData.getEventVS(), publisherNIF, "publishClaimMsgSubject");
simulatorExecutor = Executors.newFixedThreadPool(100);
sendClaims()

private void sendClaims(){
    log.debug("sendClaims");
    if(!(TestUtils.simulationData.getNumRequestsProjected() > 0)) {
        log.debug("WITHOUT NumberOfRequestsProjected");
        return;
    }
    List<String> signerList = new ArrayList<String>();
    for(int i = 0; i < TestUtils.simulationData.getNumRequestsProjected(); i++) {
        signerList.add(NifUtils.getNif(i));
    }
    synchronizedSignerList = Collections.synchronizedList(signerList)
    signClaimCompletionService = new ExecutorCompletionService<ResponseVS>(simulatorExecutor);
    simulatorExecutor.execute(new Runnable() {
        @Override public void run() { sendRequests(); }
    });
    simulatorExecutor.execute(new Runnable() {
        @Override public void run() { waitForResponses(); }
    });
}

public void sendRequests () throws Exception {
    log.debug("sendRequests - NumRequestsProjected: " + TestUtils.simulationData.getNumRequestsProjected());
    if(TestUtils.simulationData.isTimerBased()) startSimulationTimer(TestUtils.simulationData);
    else {
        while(!synchronizedSignerList.isEmpty()) {
            if((TestUtils.simulationData.getNumRequests() - TestUtils.simulationData.getNumRequestsColected()) <
                    TestUtils.simulationData.getMaxPendingResponses()) {
                int randomSigner = new Random().nextInt(synchronizedSignerList.size());
                launchSignature(synchronizedSignerList.remove(randomSigner));
            } else Thread.sleep(200);
        }
    }
}

private void launchSignature(String nif) throws Exception {
    signClaimCompletionService.submit(new ClaimSignedSender(nif, eventVS.getId()));
    TestUtils.simulationData.getAndIncrementNumRequests();
}

private void waitForResponses() throws Exception {
    log.debug("waitForResponses - NumRequestsProjected: " + TestUtils.simulationData.getNumRequestsProjected());
    while (TestUtils.simulationData.getNumRequestsProjected() > TestUtils.simulationData.getNumRequestsColected()) {
        try {
            Future<ResponseVS> f = signClaimCompletionService.take();
            ResponseVS responseVS = f.get();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                TestUtils.simulationData.getAndIncrementNumRequestsOK();
            } else TestUtils.finishWithError("ERROR", responseVS.getMessage(), TestUtils.simulationData.getNumRequestsOK())
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
            TestUtils.finishWithError("EXCEPTION", ex.getMessage(), TestUtils.simulationData.getNumRequestsOK())
        }
    }
    TestUtils.finish("OK - Num. requests completed: " + TestUtils.simulationData.getNumRequestsOK())
}

private EventVS publishEvent(EventVS eventVS, String publisherNIF, String smimeMessageSubject) throws Exception {
    log.debug("publishEvent");
    eventVS.setSubject(eventVS.getSubject()+ " -> " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()));
    SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER)
    SMIMEMessage smimeMessage = signatureService.getTimestampedSignedMimeMessage(publisherNIF,
            ContextVS.getInstance().getAccessControl().getNameNormalized(),
            JSONSerializer.toJSON(eventVS.getDataMap()).toString(), smimeMessageSubject)
    SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage,
            ContextVS.getInstance().getAccessControl().getPublishClaimURL(),
            ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null, null,
            "eventURL");
    ResponseVS responseVS = signedSender.call();
    if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
    String eventURL = ((List<String>)responseVS.getData()).iterator().next()
    byte[] responseBytes = responseVS.getMessageBytes();
    ContextVS.getInstance().copyFile(responseBytes, "/claimSimulation", "ClaimPublishedReceipt")
    SMIMEMessage dnieMimeMessage = new SMIMEMessage(new ByteArrayInputStream(responseBytes));
    //dnieMimeMessage.verify(ContextVS.getInstance().getSessionPKIXParameters());
    responseVS = HttpHelper.getInstance().getData(eventURL, ContentTypeVS.JSON);
    return EventVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
}


public void startSimulationTimer(SimulationData simulationData) throws Exception {
    Long interval = simulationData.getDurationInMillis()/simulationData.getNumRequestsProjected();
    log.debug("startSimulationTimer - interval between requests: "+ interval + " milliseconds");
    simulationTimer = new Timer();
    SignTask.Launcher launcher = new SignTask.Launcher() {
        @Override void processTask(String param) {
            launchSignature(param)
        }
    };
    simulationTimer.schedule(new SignTask(simulationTimer, synchronizedSignerList, launcher), 0, interval);
}

private void changeEventState(String publisherNIF) throws Exception {
    log.debug("changeEventState");
    Map cancelDataMap = eventVS.getChangeEventDataMap(ContextVS.getInstance().getAccessControl().getServerURL(),
            TestUtils.simulationData.getEventStateWhenFinished());
    String smimeMessageSubject ="cancelEventMsgSubject"
    SignatureService signatureService = SignatureService.getUserVSSignatureService(publisherNIF, UserVS.Type.USER)
    SMIMEMessage smimeMessage = signatureService.getTimestampedSignedMimeMessage(publisherNIF,
            ContextVS.getInstance().getAccessControl().getNameNormalized(),
            JSONSerializer.toJSON(cancelDataMap).toString(), smimeMessageSubject)
    SMIMESignedSender worker = new SMIMESignedSender(smimeMessage,
            ContextVS.getInstance().getAccessControl().getCancelEventServiceURL(),
            ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
            ContentTypeVS.JSON_SIGNED, null, null);
    ResponseVS responseVS = worker.call();
    if(ResponseVS.SC_OK == responseVS.statusCode) throw new ExceptionVS(responseVS.getMessage())
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