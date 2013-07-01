package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.modelo.SimulationData;
import java.io.File;
import org.sistemavotacion.test.simulation.callable.TimeStamperTest;
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
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.test.simulation.callable.ServerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class TimeStampSimulator extends Simulator<SimulationData> {
        
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
                simulationData.getNumRequestsProjected() + " - eventId: " + 
                simulationData.getEventId());
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
                requestCompletionService.submit(new TimeStamperTest(nifFrom, 
                        ContextoPruebas.INSTANCE.getUrlTimeStampTestService(), 
                        simulationData.getEventId()));
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
            if(simulationListener != null) 
                simulationListener.processResponse(respuesta);
            logger.debug("Response '" + (v +1) + "' statusCode: " + 
                    respuesta.getCodigoEstado());
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
        System.exit(0);
    }

    @Override public Respuesta<SimulationData> call() throws Exception {
        String serverInfoURL = ContextoPruebas.getURLInfoServidor(
                simulationData.getAccessControlURL());
        logger.debug("init - serverInfoURL: " + serverInfoURL);
        simulationData.setBegin(System.currentTimeMillis());
        ServerInitializer accessControlInitializer = 
                new ServerInitializer(simulationData.getAccessControlURL(),
                ActorConIP.Tipo.CONTROL_ACCESO);
        Respuesta respuesta = accessControlInitializer.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            initExecutors();
            countDownLatch.await();
        } else logger.error(respuesta.getMensaje());
        simulationData.setFinish(System.currentTimeMillis());
        if(requestExecutor != null) requestExecutor.shutdownNow();
        logger.debug("--------------- SIMULATION RESULT------------------");
        logger.info("Begin: " + DateUtils.getStringFromDate(
                simulationData.getBeginDate())  + " - Duration: " + 
                simulationData.getDurationStr());
        logger.debug("solicitudesOK: " + simulationData.getNumRequestsOK());
        logger.debug("solicitudesERROR: " + simulationData.getNumRequestsERROR());  
        String errorsMsg = getFormattedErrorList();
        if(errorsMsg != null) {
            logger.info(" ************* " + getErrorList().size() + " ERRORS: \n" + 
                        errorsMsg);
        }
        respuesta = new Respuesta(Respuesta.SC_FINALIZADO, simulationData);
        if(simulationListener != null) 
            simulationListener.processResponse(respuesta);
        return respuesta;
    }

    
}
