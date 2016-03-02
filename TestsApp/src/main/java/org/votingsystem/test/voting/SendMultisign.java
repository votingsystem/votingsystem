package org.votingsystem.test.voting;

import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.callable.MultiSignTestSender;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SendMultisign {

    private static Logger log =  Logger.getLogger(SendMultisign.class.getName());

    private static SimulationData simulationData;
    private static ExecutorCompletionService completionService;

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        simulationData = new SimulationData();
        simulationData.setServerURL("https://192.168.1.5/AccessControl");
        simulationData.setMaxPendingResponses(10);
        simulationData.setNumRequestsProjected(1);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                simulationData.getServerURL()), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        ActorVS actorVS = ((ActorVSDto)responseVS.getMessage(ActorVSDto.class)).getActorVS();
        if(actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
            throw new ExceptionVS("Expected DEVELOPMENT environment but found " + actorVS.getEnvironmentVS());
        }
        ContextVS.getInstance().setDefaultServer(actorVS);
        initSimulation();
    }
    private static void initSimulation(){
        log.info("initSimulation");
        if(!(simulationData.getNumRequestsProjected() > 0)) {
            log.info("WITHOUT NumberOfRequestsProjected");
            return;
        }
        log.info("initSimulation - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        completionService = new ExecutorCompletionService<ResponseVS>(executorService);
        executorService.execute(new Runnable() {
            @Override public void run() {
                try {
                    sendRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        executorService.execute(new Runnable() {
            @Override public void run() {
                try {
                    waitForResponses();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void sendRequest() throws Exception {
        log.info("sendRequest - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        while(simulationData.getNumRequests() < simulationData.getNumRequestsProjected()) {
            if((simulationData.getNumRequests() - simulationData.
                    getNumRequestsCollected()) <= simulationData.getMaxPendingResponses()) {
                String nifFrom = NifUtils.getNif(simulationData.getAndIncrementNumRequests().intValue());
                completionService.submit(new MultiSignTestSender(nifFrom,
                        ContextVS.getInstance().getDefaultServer().getMultiSignedMessageTestServiceURL()));
            } else Thread.sleep(300);
        }
    }


    private static void waitForResponses() throws Exception {
        log.info("waitForResponses - NumRequestsProjected: " +
                simulationData.getNumRequestsProjected());
        while (simulationData.getNumRequestsProjected() > simulationData.getNumRequestsCollected()) {
            try {
                Future<ResponseVS> f = completionService.take();
                ResponseVS responseVS = f.get();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    simulationData.getAndIncrementNumRequestsOK();
                } else simulationData.finishAndExit(ResponseVS.SC_ERROR, "ERROR: " + responseVS.getMessage());
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                simulationData.finishAndExit(ResponseVS.SC_EXCEPTION, "EXCEPTION: " + ex.getMessage());
            }
        }
        simulationData.finishAndExit(ResponseVS.SC_OK, null);
    }
    
}



