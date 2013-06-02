package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.simulation.launcher.RepresentingRequestLauncher;
import org.sistemavotacion.test.simulation.launcher.RepresentativeDelegationLauncher;
import java.io.File;
import java.security.KeyStore;
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
import org.sistemavotacion.test.KeyStoreHelper;
import org.sistemavotacion.test.modelo.UserBaseData;
import org.sistemavotacion.util.NifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class UserBaseDataSimulator implements Simulator<UserBaseData> {
    
    private static Logger logger = LoggerFactory.getLogger(UserBaseDataSimulator.class);
    
    public static final int MAX_PENDING_RESPONSES = 10;
    
    private final ExecutorService requestExecutor;
    private static CompletionService<Respuesta> requestCompletionService;
    
    SimulatorListener<UserBaseData> simulationListener;
    
    private UserBaseData userBaseData;
    private static List<String> errorsList;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    
    List<String> representativeNifList = new ArrayList<String>();
    List<String> userWithRepresentativeList = new ArrayList<String>();
    List<String> userWithoutRepresentativeList = new ArrayList<String>();
    

    public UserBaseDataSimulator(UserBaseData userBaseData, 
            SimulatorListener<UserBaseData> simulationListener) {
        logger.debug("numRepresentatives '" + userBaseData.getNumRepresentatives() + 
            "' - Users with representative '" + userBaseData.getNumUsersWithRepresentative() + 
            "' - Users without representative '" + userBaseData.getNumUsersWithoutRepresentative());
        this.simulationListener = simulationListener;
        this.userBaseData = userBaseData;
        requestExecutor = Executors.newFixedThreadPool(1000);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(requestExecutor);
    }
    
    public void lanzar() {
        logger.debug("lanzar");
        userBaseData.setBegin(System.currentTimeMillis());
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    lanzarAltasRepresentantes();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    recogerRespuestas();                    
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
        
    private void addErrorMsg(String msg) {
        if(errorsList == null) errorsList = new ArrayList<String>();
        errorsList.add(msg);
    }

    private void crearMockDNIeUsersWithoutRepresentative() {
        logger.debug("crearMockDNIeUsersWithoutRepresentative - users without representative:" +  
                userBaseData.getNumUsersWithoutRepresentative());
        for(int i = 0; i < userBaseData.getNumUsersWithoutRepresentative(); i++ ) {
            int userIndex = new Long(userBaseData.getAndIncrementUserIndex()).intValue();
            try {
                String userNif = NifUtils.getNif(userIndex);
                File file = new File(ContextoPruebas.getUserKeyStorePath(userNif));
                KeyStore mockDnie = KeyStoreHelper.crearMockDNIe(userNif, file,
                    ContextoPruebas.getPrivateCredentialRaizAutoridad());
                userWithoutRepresentativeList.add(userNif);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            if((i % 50) == 0) logger.debug("Created " + i + " of " + 
                    userBaseData.getNumUsersWithoutRepresentative() + " mock DNIe certs");
        }
        countDownLatch.countDown();
    }
    
    public void lanzarAltasRepresentantes () throws Exception {
        logger.debug("****************lanzarAltasRepresentantes");
        if(userBaseData.getNumRepresentatives() == 0) {
            logger.debug("lanzarAltasRepresentantes - SIN ALTAS PENDIENTES");
            return;
        } 
        while (userBaseData.getNumRepresentativeRequests() <= 
                userBaseData.getNumRepresentatives()){
            if((userBaseData.getNumRepresentativeRequests() - 
                    userBaseData.getNumRepresentativeRequestsColected()) < 
                    MAX_PENDING_RESPONSES) {
                requestCompletionService.submit(new RepresentingRequestLauncher(
                        NifUtils.getNif(new Long(userBaseData.
                        getAndIncrementUserIndex()).intValue())));
                userBaseData.getAndIncrementNumRepresentativeRequests();
            } else Thread.sleep(500);
        } 
    }
    
    public void lanzarAltasDelegaciones () throws Exception {
        logger.debug("lanzarAltasDelegaciones - pendientes: " + 
                userBaseData.getNumUsersWithRepresentative());
        if(userBaseData.getNumUsersWithRepresentative() == 0) {
            logger.debug("lanzarAltasRepresentantes - pending 0 requests");
            return;
        } 
        while (userBaseData.getNumDelegationRequests() <= 
                userBaseData.getNumUsersWithRepresentative()) {
            if((userBaseData.getNumDelegationRequests() - 
                    userBaseData.getNumDelegationRequestsColected()) < 
                    MAX_PENDING_RESPONSES) {
                String userNIF = NifUtils.getNif(new Long(
                        userBaseData.getAndIncrementUserIndex()).intValue());
                String representativeNIF = getRandomRepresentative();
                if(representativeNIF == null) {
                    userBaseData.setStatusCode(Respuesta.SC_ERROR);
                    userBaseData.setMessage(ContextoPruebas.getString(
                            "userBaseWithouRepresentativesErrorMsg"));
                    break;
                } 
                requestCompletionService.submit(new RepresentativeDelegationLauncher(
                        userNIF, representativeNIF));
                userBaseData.getAndIncrementNumDelegationRequests();
            } else Thread.sleep(500);
        }
    }
    
    public void recogerRespuestas() throws InterruptedException, 
            ExecutionException, Exception {
        logger.debug("--------------------- recogerRespuestas ");
        for (int v = 0; v < userBaseData.getNumRepresentatives(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Respuesta alta de representante '" + v + "' statusCode: " + 
                    respuesta.getCodigoEstado() + " - mensaje: " + respuesta.getMensaje());
            userBaseData.setStatusCode(respuesta.getCodigoEstado());
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                 representativeNifList.add(respuesta.getMensaje());
                 userBaseData.getAndIncrementNumRepresentativeRequestsOK();
            } else {
                userBaseData.setMessage(respuesta.getMensaje());
                logger.error("ERROR - statusCode: " + respuesta.getCodigoEstado() 
                        + " - msg: " + respuesta.getMensaje());
                addErrorMsg(respuesta.getMensaje());
                userBaseData.getAndIncrementNumRepresentativeRequestsERROR();
                finish();
            }
            simulationListener.updateSimulationData(userBaseData);
        }
        

        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    lanzarAltasDelegaciones();                  
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        logger.debug("Begin to read delegation response results - pending: " + 
                 userBaseData.getNumUsersWithRepresentative());
        for (int v = 0; v < userBaseData.getNumUsersWithRepresentative(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Delegation response '" + v + "' - statusCode:" + 
                    respuesta.getCodigoEstado() + " - msg: " + respuesta.getMensaje());
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                userWithRepresentativeList.add(respuesta.getMensaje());
                userBaseData.getAndIncrementNumDelegationsOK();
            } else {
                userBaseData.setMessage(respuesta.getMensaje());
                userBaseData.getAndIncrementNumDelegationsERROR();
            }
            
            //TODO defensive copy
            simulationListener.updateSimulationData(userBaseData);
        }
        finish();
    }
    
    private String getRandomRepresentative() { 
        if(representativeNifList.isEmpty()) return null;
        int randomSelected = new Random().nextInt(representativeNifList.size()); // In real life, the Random object should be rather more shared than this
        return representativeNifList.get(randomSelected);
    }
    
    @Override public UserBaseData finish() {
        try {
            countDownLatch.await();
            logger.debug("finish");
            userBaseData.setFinish(System.currentTimeMillis());
            userBaseData.setUsersWithRepresentativeList(userWithRepresentativeList);
            userBaseData.setUsersWithoutRepresentativeList(userWithoutRepresentativeList);
            userBaseData.setRepresentativeNifList(representativeNifList);
            requestExecutor.shutdownNow();
            simulationListener.setSimulationResult(this);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            return userBaseData;
        }
    }

    @Override public UserBaseData getData() {
        return userBaseData;
    }

    @Override public List<String> getErrorsList() {
        return errorsList;
    }

}