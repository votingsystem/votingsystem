package org.votingsystem.test.voting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TimeStampVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.test.callable.TimeStamperTestSender;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStamp_request {
    
    private static Logger log;
    private static ExecutorCompletionService completionService;

    public static void main(String[] args) throws Exception {
        TimeStampVS timeStampVS = new TimeStampVS();

        Map simulationDataMap = new HashMap<>();
        simulationDataMap.put("serverURL", "http://www.sistemavotacion.org/TimeStampServer");//http://www.sistemavotacion.org/TimeStampServer
        simulationDataMap.put("maxPendingResponses", 10);
        simulationDataMap.put("numRequestsProjected", 10);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationDataMap.put("timer", timerMap);
        log = TestUtils.init(TimeStamp_request.class, simulationDataMap);

        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                TestUtils.getSimulationData().getServerURL()), ContentTypeVS.JSON);
        if (ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        Map<String, Object> dataMap = new ObjectMapper().readValue(
                responseVS.getMessage(), new TypeReference<HashMap<String, Object>>() {});
        ActorVS actorVS = ActorVS.parse(dataMap);
        /*if (actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
            throw new ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
        }*/
        ContextVS.getInstance().setDefaultServer(actorVS);
        ContextVS.getInstance().setTimeStampServerCert(actorVS.getX509Certificate());
        if(!(TestUtils.getSimulationData().getNumRequestsProjected() > 0)) {
            log.info("NumRequestsProjected = 0");
            return;
        }
        log.info("initSimulation - NumRequestsProjected: " + TestUtils.getSimulationData().getNumRequestsProjected());
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        completionService = new ExecutorCompletionService<ResponseVS>(executorService);
        executorService.execute(() -> {
            try {
                sendRequests();
            } catch (Exception e) { e.printStackTrace();  }
        });
        executorService.execute(() -> {
            try {
                waitForResponses();
            } catch (Exception e) { e.printStackTrace();  }
        });
    }

    public static void sendRequests() throws Exception {
        log.info("sendRequests - NumRequestsProjected: " + TestUtils.getSimulationData().getNumRequestsProjected());
        while(TestUtils.getSimulationData().getNumRequests() < TestUtils.getSimulationData().getNumRequestsProjected()) {
            if((TestUtils.getSimulationData().getNumRequests() - TestUtils.getSimulationData().
                    getNumRequestsCollected()) <= TestUtils.getSimulationData().getMaxPendingResponses()) {
                String nifFrom = NifUtils.getNif(TestUtils.getSimulationData().getAndIncrementNumRequests().intValue());
                completionService.submit(new TimeStamperTestSender(nifFrom, TestUtils.getSimulationData().getServerURL()));
            } else Thread.sleep(300);
        }
    }

    private static void waitForResponses() throws Exception {
        log.info("waitForResponses - NumRequestsProjected: " + TestUtils.getSimulationData().getNumRequestsProjected());
        while (TestUtils.getSimulationData().getNumRequestsProjected() > TestUtils.getSimulationData().getNumRequestsCollected()) {
            try {
                Future<ResponseVS> f = completionService.take();
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

}
    





