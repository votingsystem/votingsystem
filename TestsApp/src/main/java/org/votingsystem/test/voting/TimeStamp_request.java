package org.votingsystem.test.voting;

import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.callable.TimeStamperTestSender;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.NifUtils;

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
    private static SimulationData simulationData;
    private static ExecutorCompletionService completionService;

    public static void main(String[] args) throws Exception {
        simulationData = new SimulationData();
        simulationData.setServerURL("http://www.sistemavotacion.org/TimeStampServer");
        simulationData.setMaxPendingResponses(10);
        simulationData.setNumRequestsProjected(10);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);
        log = TestUtils.init(TimeStamp_request.class, simulationData);

        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                simulationData.getServerURL()), ContentTypeVS.JSON);
        if (ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        ActorVS actorVS = ((ActorVSDto)responseVS.getDto(ActorVSDto.class)).getActorVS();
        /*if (actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
            throw new ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
        }*/
        ContextVS.getInstance().setDefaultServer(actorVS);
        ContextVS.getInstance().setTimeStampServerCert(actorVS.getX509Certificate());
        if(!(simulationData.getNumRequestsProjected() > 0)) {
            log.info("NumRequestsProjected = 0");
            return;
        }
        log.info("initSimulation - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
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
        log.info("sendRequests - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        while(simulationData.getNumRequests() < simulationData.getNumRequestsProjected()) {
            if((simulationData.getNumRequests() - simulationData.
                    getNumRequestsCollected()) <= simulationData.getMaxPendingResponses()) {
                String nifFrom = NifUtils.getNif(simulationData.getAndIncrementNumRequests().intValue());
                completionService.submit(new TimeStamperTestSender(nifFrom, simulationData.getServerURL()));
            } else Thread.sleep(300);
        }
    }

    private static void waitForResponses() throws Exception {
        log.info("waitForResponses - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        while (simulationData.getNumRequestsProjected() > simulationData.getNumRequestsCollected()) {
            try {
                Future<ResponseVS> f = completionService.take();
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

}
    





