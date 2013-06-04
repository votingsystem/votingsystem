package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.simulation.launcher.RepresentativeRequestLauncher;
import org.sistemavotacion.test.simulation.launcher.RepresentativeDelegationLauncher;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.modelo.UserBaseSimulationData;
import org.sistemavotacion.util.NifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class UserBaseDataSimulator extends Simulator<UserBaseSimulationData> {
    
    private static Logger logger = LoggerFactory.getLogger(UserBaseDataSimulator.class);
    
    public static final int MAX_PENDING_RESPONSES = 10;
    
    private final ExecutorService requestExecutor;
    private static CompletionService<Respuesta> requestCompletionService;
    
    SimulatorListener<UserBaseSimulationData> simulationListener;
    
    private UserBaseSimulationData simulationData;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    List<String> representativeNifList = new ArrayList<String>();
    List<String> userWithRepresentativeList = new ArrayList<String>();
    List<String> userWithoutRepresentativeList = new ArrayList<String>();
    

    public UserBaseDataSimulator(UserBaseSimulationData simulationData, 
            SimulatorListener<UserBaseSimulationData> simulationListener) {
        super(simulationData);
        this.simulationListener = simulationListener;
        this.simulationData = simulationData;
        requestExecutor = Executors.newFixedThreadPool(100);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(requestExecutor);
    }
    
    @Override public void init() {
        logger.debug("init");
        simulationData.setBegin(System.currentTimeMillis());
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    launchRepresentativeRequests();                    
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
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    crearMockDNIeUsersWithoutRepresentative();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }

    private void crearMockDNIeUsersWithoutRepresentative() {
        logger.debug("crearMockDNIeUsersWithoutRepresentative - users without representative:" +  
                simulationData.getNumVotesUsersWithoutRepresentative());
        for(int i = 0; i < simulationData.getNumVotesUsersWithoutRepresentative(); i++ ) {
            int userIndex = new Long(simulationData.getAndIncrementUserIndex()).intValue();
            try {
                String userNif = NifUtils.getNif(userIndex);
                ContextoPruebas.INSTANCE.crearMockDNIe(userNif);
                userWithoutRepresentativeList.add(userNif);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            if((i % 50) == 0) logger.debug("Created " + i + " of " + 
                    simulationData.getNumVotesUsersWithoutRepresentative() + " mock DNIe certs");
        }
        countDownLatch.countDown();
    }
    
    public void launchRepresentativeRequests () throws Exception {
        logger.debug("launchRepresentativeRequests - NumRepresentatives: " + 
                simulationData.getNumRepresentatives());
        if(simulationData.getNumRepresentatives() == 0) {
            logger.debug("launchRepresentativeRequests - WITHOUT REPRESENTATIVE REQUESTS");
            return;
        } 
        while (simulationData.getNumRepresentativeRequests() <= 
                simulationData.getNumRepresentatives()){
            if((simulationData.getNumRepresentativeRequests() - 
                    simulationData.getNumRepresentativeRequestsColected()) < 
                    MAX_PENDING_RESPONSES) {
                requestCompletionService.submit(new RepresentativeRequestLauncher(
                        NifUtils.getNif(new Long(simulationData.
                        getAndIncrementUserIndex()).intValue())));
                simulationData.getAndIncrementNumRepresentativeRequests();
            } else Thread.sleep(500);
        } 
    }
    
    public void launchDelegationRequests () throws Exception {
        logger.debug("launchDelegationRequests - NumUsersWithRepresentative: " + 
                simulationData.getNumUsersWithRepresentative());
        if(simulationData.getNumUsersWithRepresentative() == 0) {
            logger.debug("launchRepresentativeRequests - WITHOUT DELEGATION REQUESTS");
            return;
        } 
        while (simulationData.getNumDelegationRequests() <= 
                simulationData.getNumUsersWithRepresentative()) {
            if((simulationData.getNumDelegationRequests() - 
                    simulationData.getNumDelegationRequestsColected()) < 
                    MAX_PENDING_RESPONSES) {
                String userNIF = NifUtils.getNif(new Long(
                        simulationData.getAndIncrementUserIndex()).intValue());
                String representativeNIF = getRandomRepresentative();
                if(representativeNIF == null) {
                    simulationData.setStatusCode(Respuesta.SC_ERROR);
                    simulationData.setMessage(ContextoPruebas.INSTANCE.getString(
                            "userBaseWithouRepresentativesErrorMsg"));
                    break;
                } 
                requestCompletionService.submit(new RepresentativeDelegationLauncher(
                        userNIF, representativeNIF));
                simulationData.getAndIncrementNumDelegationRequests();
            } else Thread.sleep(500);
        }
    }
    
    public void readResponses() throws InterruptedException, 
            ExecutionException, Exception {
        logger.debug("--------------------- readResponses ");
        for (int v = 0; v < simulationData.getNumRepresentatives(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Respuesta alta de representante '" + v + "' statusCode: " + 
                    respuesta.getCodigoEstado() + " - mensaje: " + respuesta.getMensaje());
            simulationData.setStatusCode(respuesta.getCodigoEstado());
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                 representativeNifList.add(respuesta.getMensaje());
                 simulationData.getAndIncrementNumRepresentativeRequestsOK();
            } else {
                simulationData.setMessage(respuesta.getMensaje());
                logger.error("ERROR - statusCode: " + respuesta.getCodigoEstado() 
                        + " - msg: " + respuesta.getMensaje());
                addErrorMsg(respuesta.getMensaje());
                simulationData.getAndIncrementNumRepresentativeRequestsERROR();
                finish();
            }
            simulationListener.updateSimulationData(simulationData);
        }
        

        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    launchDelegationRequests();                  
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        logger.debug("Begin to read delegation response results - pending: " + 
                 simulationData.getNumUsersWithRepresentative());
        for (int v = 0; v < simulationData.getNumUsersWithRepresentative(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Delegation response '" + v + "' - statusCode:" + 
                    respuesta.getCodigoEstado() + " - msg: " + respuesta.getMensaje());
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                userWithRepresentativeList.add(respuesta.getMensaje());
                simulationData.getAndIncrementNumDelegationsOK();
            } else {
                simulationData.setMessage(respuesta.getMensaje());
                simulationData.getAndIncrementNumDelegationsERROR();
            }
            
            //TODO defensive copy
            simulationListener.updateSimulationData(simulationData);
        }
        finish();
    }
    
    private String getRandomRepresentative() { 
        if(representativeNifList.isEmpty()) return null;
        int randomSelected = new Random().nextInt(representativeNifList.size()); // In real life, the Random object should be rather more shared than this
        return representativeNifList.get(randomSelected);
    }
    
    @Override public UserBaseSimulationData finish() throws Exception {
        logger.debug("finish");
        countDownLatch.await();
        simulationData.setFinish(System.currentTimeMillis());
        simulationData.setUsersWithRepresentativeList(userWithRepresentativeList);
        simulationData.setUsersWithoutRepresentativeList(userWithoutRepresentativeList);
        simulationData.setRepresentativeNifList(representativeNifList);
        if(requestExecutor != null) requestExecutor.shutdownNow();
        if(simulationListener != null){
            simulationListener.setSimulationResult(this);
        } else {
            logger.debug("--------------- SIMULATION RESULT----------------------");   
            logger.info("Duration: " + simulationData.getDurationStr());
            logger.info("numRepresentativeRequests: " + 
                    simulationData.getNumRepresentativeRequests());
            logger.info("numRepresentativeRequestsOK: " + 
                    simulationData.getNumRepresentativeRequestsOK());
            logger.info("numRepresentativeRequestsERROR: " + 
                    simulationData.getNumRepresentativeRequestsERROR());            
            logger.info("delegationRequests: " + 
                    simulationData.getNumDelegationRequests());
            logger.info("delegationRequestsOK: " + 
                    simulationData.getNumDelegationsOK());   
            logger.info("delegationRequestsERROR: " + 
                    simulationData.getNumDelegationsERROR());  
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

    @Override public UserBaseSimulationData getData() {
        return simulationData;
    }

}