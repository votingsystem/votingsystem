package org.votingsystem.test.voting

import net.sf.json.JSONSerializer
import org.votingsystem.model.*
import org.votingsystem.test.callable.EncryptionTestSender
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.NifUtils

import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

Map simulationDataMap = [serverURL:"http://sistemavotacion.org/AccessControl", maxPendingResponses:10,
                         numRequestsProjected:2, timer:[active:false, time:"00:00:10"]]

log = TestUtils.init(Encryption_send.class, simulationDataMap)

ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
        TestUtils.simulationData.getServerURL()),ContentTypeVS.JSON);
if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
ActorVS actorVS = ActorVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
if(actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
    throw new ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
}
ContextVS.getInstance().setDefaultServer(actorVS)
initSimulation()

private void initSimulation(){
    log.debug("initSimulation");
    if(!(TestUtils.simulationData.getNumRequestsProjected() > 0)) {
        log.debug("WITHOUT NumberOfRequestsProjected");
        return;
    }
    log.debug("initSimulation - NumRequestsProjected: " + TestUtils.simulationData.getNumRequestsProjected() +
            " -serverURL" + TestUtils.simulationData.getServerURL());
    ExecutorService executorService = Executors.newFixedThreadPool(100);
    completionService = new ExecutorCompletionService<ResponseVS>(executorService);
    executorService.execute(new Runnable() {
        @Override public void run() { sendRequests(
                ContextVS.getInstance().getDefaultServer().getEncryptionServiceURL()); }
    });
    executorService.execute(new Runnable() {
        @Override public void run() { waitForResponses(); }
    });
}

public void sendRequests (String serviceURL) throws Exception {
    log.debug("sendRequests - NumRequestsProjected: " + TestUtils.simulationData.getNumRequestsProjected());
    while(TestUtils.simulationData.getNumRequests() < TestUtils.simulationData.getNumRequestsProjected()) {
        if((TestUtils.simulationData.getNumRequests() - TestUtils.simulationData.
                getNumRequestsColected()) <= TestUtils.simulationData.getMaxPendingResponses()) {
            String nifFrom = NifUtils.getNif(TestUtils.simulationData.getAndIncrementNumRequests().intValue());
            completionService.submit(new EncryptionTestSender(nifFrom, serviceURL,
            ContextVS.getInstance().getDefaultServer().getX509Certificate()));
        } else Thread.sleep(300);
    }
}


private void waitForResponses() throws Exception {
    log.debug("waitForResponses - NumRequestsProjected: " + TestUtils.simulationData.getNumRequestsProjected());
    while (TestUtils.simulationData.getNumRequestsProjected() > TestUtils.simulationData.getNumRequestsColected()) {
        try {
            Future<ResponseVS> f = completionService.take();
            ResponseVS responseVS = f.get();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                TestUtils.simulationData.getAndIncrementNumRequestsOK();
            } else TestUtils.finishWithError("ERROR", responseVS.getMessage(), TestUtils.simulationData.getNumRequestsOK())
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
            TestUtils.finishWithError("EXCEPTION", ex.getMessage(), TestUtils.simulationData.getNumRequestsOK())
        }
    }
    TestUtils.finish("OK - Num. requests completed: " + TestUtils.simulationData.getNumRequestsOK());
}