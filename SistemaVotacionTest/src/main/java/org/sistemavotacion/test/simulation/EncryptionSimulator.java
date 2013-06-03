package org.sistemavotacion.test.simulation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.simulation.launcher.EncryptorLauncher;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EncryptionSimulator extends Simulator<SimulationData> 
        implements VotingSystemWorkerListener, ActionListener {
        
    private static Logger logger = LoggerFactory.getLogger(EncryptionSimulator.class);
    
    private final ExecutorService requestExecutor;
    private static CompletionService<Respuesta> requestCompletionService;
    
    List<String> representativeNifList = new ArrayList<String>();
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    private String requestURL = null;
    private SimulationData simulationData = null;
    
    private SimulatorListener simulationListener;

    public EncryptionSimulator(SimulationData simulationData, 
            SimulatorListener simulationListener) {
        super(simulationData);
        this.simulationListener = simulationListener;
        this.simulationData = simulationData;
        logger.debug("NumRequestsProjected:" + simulationData.getNumRequestsProjected());
        this.requestURL = ContextoPruebas.getURLEncryptor(
                simulationData.getAccessControlURL());
        logger.debug("requestURL:" + requestURL);
        requestExecutor = Executors.newFixedThreadPool(100);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(
                requestExecutor);
    }

    @Override public void init() throws Exception { 
        String urlInfoServer = ContextoPruebas.getURLInfoServidor(
                simulationData.getAccessControlURL());
        logger.debug("init - urlInfoServer: " + urlInfoServer);
        new InfoGetterWorker(null, urlInfoServer, null, this).execute();
        countDownLatch.await();
        finish();
    }

    private void launchSimulationThreads() {
        logger.debug("launchSimulationThreads");
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
        //        countDownLatch.await(); //
    }
    
    public void launchRequests () throws Exception {
        logger.debug("- launchRequests - NumRequestsProjected: " + 
                simulationData.getNumRequestsProjected());
        if(simulationData.getNumRequestsProjected() == 0) {
            logger.debug("launchRequests - WITHOUT REQUESTS PROJECTED");
            return;
        } 
        simulationData.setBegin(System.currentTimeMillis());
        if(simulationData.isTimerBased()) startTimer(this);
        else{
            while(simulationData.getNumRequests() < 
                simulationData.getNumRequestsProjected()) {
                if((simulationData.getNumRequests() - simulationData.
                    getNumRequestsColected()) <= simulationData.getMaxPendingResponses()) {
                    logger.debug("launchRequests - WITHOUT REQUESTS PROJECTED");
                    String nifFrom = NifUtils.getNif(simulationData.
                            getAndIncrementNumRequests().intValue());
                    requestCompletionService.submit(new EncryptorLauncher(
                            nifFrom, requestURL));
                 } else Thread.sleep(300);
            }
        }

    }
    
    public void readResponses() throws InterruptedException, 
            ExecutionException, Exception {
        logger.debug("--------------------- readResponses ");
        for (int v = 0; v < simulationData.getNumRequestsProjected(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Respuesta '" + v + "' statusCode: " + 
                    respuesta.getCodigoEstado());
            if(respuesta.getCodigoEstado() == Respuesta.SC_OK) {
                simulationData.getAndIncrementNumRequestsOK();
            } else {
                String msg = " - ERROR msg: " + respuesta.getMensaje();
                addErrorMsg(msg);
                logger.error(msg);
                simulationData.getAndIncrementNumRequestsERROR();
            }
        }
        countDownLatch.countDown();
    }
    
    @Override public SimulationData getData() {
        return simulationData;
    }
        
    @Override  public SimulationData finish() throws Exception{
        logger.debug("finish");
        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        requestExecutor.shutdownNow();       
        if(simulationListener != null) {           
            simulationListener.setSimulationResult(this);
        } else { 
            logger.debug("--------------- SIMULATION RESULT----------------------"); 
            simulationData.setFinish(System.currentTimeMillis());
            logger.debug("duracionStr: " + simulationData.getDurationStr());
            logger.debug("NumRequests: " + simulationData.getNumRequests());
            logger.debug("NumRequestsOK: " + simulationData.getNumRequestsOK());
            logger.debug("NumRequestsERROR: " + simulationData.getNumRequestsERROR());
            String errorsMsg = getFormattedErrorList();
            if(errorsMsg != null) {
                logger.info(" ************* " + getErrorsList().size() + " ERRORS: \n" + 
                            errorsMsg);
            }
            logger.debug("------------------- FINISHED --------------------------");
            System.exit(0);
        }
        return simulationData;
    }

    @Override
    public void process(List<String> messages) { }

    @Override
    public void showResult(VotingSystemWorker worker) {         
        if(Respuesta.SC_OK == worker.getStatusCode()) {
            try {
                ActorConIP accessControl = ActorConIP.parse(worker.getMessage());
                if(ActorConIP.EnvironmentMode.TEST !=  
                            accessControl.getEnvironmentMode()) {
                    String msg = "SERVER NOT IN TEST MODE. Server mode:" + 
                            accessControl.getEnvironmentMode();
                    logger.error("### ERROR - " + msg);
                    addErrorMsg(msg);
                    countDownLatch.countDown();
                }
                ContextoPruebas.INSTANCE.setControlAcceso(accessControl);
                launchSimulationThreads();
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                String msg = "ERROR GETTING ACCESS CONTROL INFO:" + 
                           ex.getMessage(); 
                logger.error("### ERROR - " + msg);
                addErrorMsg(msg);
                countDownLatch.countDown();
            }
        } else {
            String msg = "ERROR GETTING ACCESS CONTROL INFO:" +  worker.getMessage(); 
            logger.error("### ERROR - " + msg);
            addErrorMsg(msg);
            countDownLatch.countDown();
        }
    }
    

    
    public static void main(String[] args) throws Exception {
        SimulationData simulationData = null;
        if(args != null && args.length > 0) {
            logger.debug("args[0]");
            simulationData = SimulationData.parse(args[0]);
        } else {
            File jsonFile = File.createTempFile("encryptionSimulationData", ".json");
            jsonFile.deleteOnExit();
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("simulatorFiles/encryptionSimulationData.json"), jsonFile); 
            simulationData = SimulationData.parse(FileUtils.getStringFromFile(jsonFile));
        }
        EncryptionSimulator simuHelper = new EncryptionSimulator(simulationData, null);
        simuHelper.init();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        logger.debug("timer numHoursProjected:" + simulationData.getNumHoursProjected() + 
                "- numMinutesProjected" + simulationData.getNumMinutesProjected() + 
                " - NumSecondsProjected: " + + simulationData.getNumSecondsProjected());
        if (ae.getSource().equals(timer)) {
            if((simulationData.getNumRequestsProjected() - 
                    simulationData.getNumRequestsColected()) > 0) {
                String nifFrom = NifUtils.getNif(simulationData.
                        getAndIncrementNumRequests().intValue());
                try {
                    requestCompletionService.submit(new EncryptorLauncher(    
                            nifFrom, requestURL));
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } else timer.stop();
        }
    }

}
