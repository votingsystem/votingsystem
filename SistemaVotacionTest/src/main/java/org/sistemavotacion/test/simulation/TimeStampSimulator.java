package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.modelo.SimulationData;
import java.io.File;
import org.sistemavotacion.test.simulation.callable.TimeStamper;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.util.SimulationUtils;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class TimeStampSimulator extends Simulator<SimulationData>  {
        
    private static Logger logger = LoggerFactory.getLogger(TimeStampSimulator.class);
    
    public static final int MAX_PENDING_RESPONSES = 10;
       
    private final ExecutorService requestExecutor;
    private static CompletionService<Respuesta> requestCompletionService;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    private SimulatorListener simulationListener;
    
    public TimeStampSimulator(SimulationData simulationData,
            SimulatorListener simulationListener) {
        super(simulationData);
        this.simulationListener = simulationListener;
        requestExecutor = Executors.newFixedThreadPool(100);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(requestExecutor);
    }

    public TimeStampSimulator(SimulationData simulationData) {
        super(simulationData);
        this.simulationData = simulationData;
        requestExecutor = Executors.newFixedThreadPool(100);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(requestExecutor);
    }
    
    private void initExecutors() {
        simulationData.setBegin(System.currentTimeMillis());
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    launchRequests();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    readResponses();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }
    
    private void launchRequests () throws Exception {
        logger.debug("--------------- launchRequests - NumRequestsProjected: " + 
                simulationData.getNumRequestsProjected());
        if(simulationData.getNumRequestsProjected() == 0) {
            logger.debug("launchRequests - WITHOUT REQUESTS PROJECTED");
            return;
        } 
        while(simulationData.getNumRequests() < 
                simulationData.getNumRequestsProjected()) {
            if((simulationData.getNumRequests() - simulationData.
                getNumRequestsColected()) <= simulationData.getMaxPendingResponses()) {
                String nifFrom = NifUtils.getNif(simulationData.
                        getAndIncrementNumRequests().intValue());
                requestCompletionService.submit(new TimeStamper(nifFrom, 
                        ContextoPruebas.INSTANCE.getUrlTimeStampServer()));
             } else Thread.sleep(300);
        }
        
     
    }
    
    private void readResponses() throws InterruptedException, 
            ExecutionException, Exception {
        logger.debug("---------------- readResponses - NumRequestsProjected: " + 
                simulationData.getNumRequestsProjected());
        for (int v = 0; v < simulationData.getNumRequestsProjected(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Response '" + v + "' statusCode: " + 
                    respuesta.getCodigoEstado() + " - msg: " + respuesta.getMensaje());
            if(respuesta.getCodigoEstado() == Respuesta.SC_OK) {
                simulationData.getAndIncrementNumRequestsOK();
            } else {
                simulationData.getAndIncrementNumRequestsERROR();
                addErrorMsg(respuesta.getMensaje());
            }
        }
        countDownLatch.countDown();
    }
    

    public static void main(String[] args) {
        try {

            SimulationData simulationData = null;
            if(args != null && args.length > 0) {
                logger.debug("args[0]");
                simulationData = SimulationData.parse(args[0]);
            } else {
                File jsonFile = File.createTempFile("TimeStampSimulation", ".json");
                jsonFile.deleteOnExit();
                FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("simulatorFiles/timeStampSimulationData.json"), jsonFile); 
                simulationData = SimulationData.parse(FileUtils.getStringFromFile(jsonFile));
            }
            TimeStampSimulator pruebasSellosDeTiempo = 
                    new TimeStampSimulator(simulationData, null);
            pruebasSellosDeTiempo.call();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }



    @Override public SimulationData call() throws Exception {
        String serverInfoURL = ContextoPruebas.getURLInfoServidor(
                simulationData.getAccessControlURL());
        logger.debug("init - serverInfoURL: " + serverInfoURL);
        InfoGetterWorker worker = new InfoGetterWorker(null, 
                serverInfoURL, null, null);
        worker.execute();
        Respuesta respuesta = worker.get();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            try {
                ActorConIP accessControl = ActorConIP.parse(worker.getMessage());
                String msg = SimulationUtils.checkActor(
                        accessControl, ActorConIP.Tipo.CONTROL_ACCESO);
                if(msg == null) {
                    ContextoPruebas.INSTANCE.setControlAcceso(accessControl);
                    initExecutors();
                    countDownLatch.await();
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else {
            logger.error("ERROR GETTING ACCESS REQUEST DATA: " + worker.getMessage());
        }        
        simulationData.setFinish(System.currentTimeMillis());
        if(requestExecutor != null) requestExecutor.shutdownNow();
        logger.debug("--------------- SIMULATION RESULT------------------");
        logger.debug("duracionStr: " + simulationData.getDurationStr());
        logger.debug("solicitudesOK: " + simulationData.getNumRequestsOK());
        logger.debug("solicitudesERROR: " + simulationData.getNumRequestsERROR());  
        String errorsMsg = getFormattedErrorList();
        if(errorsMsg != null) {
            logger.info(" ************* " + getErrorsList().size() + " ERRORS: \n" + 
                        errorsMsg);
        }
        logger.debug("------------------- FINISHED --------------------------");
        if(simulationListener != null)
            simulationListener.setSimulationResult(null);
        return simulationData;
    }
    
}
