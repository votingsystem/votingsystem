package org.votingsystem.test.voting

import net.sf.json.JSONSerializer
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.test.callable.TimeStamperTestSender
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.NifUtils

import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

//http://www.sistemavotacion.org/TimeStampServer
Map simulationDataMap = [serverURL:"http://www.sistemavotacion.org/TimeStampServer", maxPendingResponses:10,
                         numRequestsProjected:1, timer:[active:false, time:"00:00:10"]]

log = TestUtils.init(Multisign_send.class, simulationDataMap)

ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
        TestUtils.simulationData.getServerURL()),ContentTypeVS.JSON);
if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
ActorVS actorVS = ActorVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
if(actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
    throw new ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
}
ContextVS.getInstance().setDefaultServer(actorVS)
ContextVS.getInstance().setTimeStampServerCert(actorVS.getX509Certificate())

SignatureService authoritySignatureService = SignatureService.getAuthoritySignatureService()
String serviceURL = "${TestUtils.simulationData.getServerURL()}/certificateVS/addCertificateAuthority"
byte[] rootCACertPEMBytes = CertUtils.getPEMEncoded (authoritySignatureService.certSigner);
Map requestMap = [operation: TypeVS.CERT_CA_NEW.toString(), certChainPEM: new String(rootCACertPEMBytes, "UTF-8"),
                  info: "Autority from Test Web App '${Calendar.getInstance().getTime()}'"]
responseVS = HttpHelper.getInstance().sendData(JSONSerializer.toJSON(requestMap).toString().getBytes(),
        ContentTypeVS.JSON, serviceURL)

if (ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())

initSimulation()

private void initSimulation(){
    log.debug("initSimulation");
    if(!(TestUtils.simulationData.getNumRequestsProjected() > 0)) {
        log.debug("WITHOUT NumberOfRequestsProjected");
        return;
    }
    log.debug("initSimulation - NumRequestsProjected: " + TestUtils.simulationData.getNumRequestsProjected());
    ExecutorService executorService = Executors.newFixedThreadPool(100);
    signCompletionService = new ExecutorCompletionService<ResponseVS>(executorService);
    executorService.execute(new Runnable() {
        @Override public void run() { sendRequests(); }
    });
    executorService.execute(new Runnable() {
        @Override public void run() { waitForResponses(); }
    });
}

public void sendRequests () throws Exception {
    log.debug("sendRequests - NumRequestsProjected: " + TestUtils.simulationData.getNumRequestsProjected());
    while(TestUtils.simulationData.getNumRequests() < TestUtils.simulationData.getNumRequestsProjected()) {
        if((TestUtils.simulationData.getNumRequests() - TestUtils.simulationData.
                getNumRequestsColected()) <= TestUtils.simulationData.getMaxPendingResponses()) {
            String nifFrom = NifUtils.getNif(TestUtils.simulationData.getAndIncrementNumRequests().intValue());
            signCompletionService.submit(new TimeStamperTestSender(nifFrom, TestUtils.simulationData.getServerURL()));
        } else Thread.sleep(300);
    }
}


private void waitForResponses() throws Exception {
    log.debug("waitForResponses - NumRequestsProjected: " +
            TestUtils.simulationData.getNumRequestsProjected());
    while (TestUtils.simulationData.getNumRequestsProjected() > TestUtils.simulationData.getNumRequestsColected()) {
        Future<ResponseVS> f = signCompletionService.take();
        ResponseVS responseVS = f.get();
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            TestUtils.simulationData.getAndIncrementNumRequestsOK();
        } else throw new ExceptionVS(responseVS.getMessage())
    }
    TestUtils.finish("Num. requests completed: " + TestUtils.simulationData.getAndIncrementNumRequestsOK());
}