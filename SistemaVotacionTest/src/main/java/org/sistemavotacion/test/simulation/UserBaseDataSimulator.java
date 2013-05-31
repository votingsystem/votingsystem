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
import java.util.concurrent.atomic.AtomicLong;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.KeyStoreHelper;
import org.sistemavotacion.test.modelo.UserBaseData;
import org.sistemavotacion.test.util.NifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class UserBaseDataSimulator implements Simulator {
    
    private static Logger logger = LoggerFactory.getLogger(UserBaseDataSimulator.class);
    
    public static final int MAX_PENDING_RESPONSES = 10;
        
    
    private final ExecutorService requestExecutor;
    private static CompletionService<Respuesta> requestCompletionService;
    
    private static AtomicLong numeroUsuariosRepresentados;
    private static AtomicLong numeroRepresentantes;
    private static AtomicLong altasEnviadas;
    private static AtomicLong altasRecogidas;
    private static AtomicLong delegacionesEnviadas;
    private static AtomicLong delegacionesRecogidas;
    
    private static long comienzo;
    private static long duracion;
    
    SimulatorListener<UserBaseData> simulationListener;
    
    private UserBaseData userBaseData;
    
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
        altasEnviadas = new AtomicLong(0);
        altasRecogidas = new AtomicLong(0);
        delegacionesEnviadas = new AtomicLong(0);
        delegacionesRecogidas = new AtomicLong(0);
        numeroUsuariosRepresentados = new AtomicLong(userBaseData.getNumUsersWithRepresentative());
        numeroRepresentantes =  new AtomicLong(userBaseData.getNumRepresentatives());
        requestExecutor = Executors.newFixedThreadPool(1000);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(requestExecutor);
    }
    
    public void lanzar() {
        logger.debug("lanzar");
        comienzo = System.currentTimeMillis();
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
        if(numeroRepresentantes.get() == 0) {
            logger.debug("lanzarAltasRepresentantes - SIN ALTAS PENDIENTES");
            return;
        } 
        do {
            if((altasEnviadas.get() - altasRecogidas.get()) < 
                    MAX_PENDING_RESPONSES) {
                requestCompletionService.submit(new RepresentingRequestLauncher(
                        NifUtils.getNif(new Long(userBaseData.
                        getAndIncrementUserIndex()).intValue())));
                altasEnviadas.getAndIncrement();
            } else Thread.sleep(500);
        } while (altasEnviadas.get() < numeroRepresentantes.get());
    }
    
    public void lanzarAltasDelegaciones () throws Exception {
        logger.debug("lanzarAltasDelegaciones - pendientes: " + 
                numeroUsuariosRepresentados.get());
        if(numeroUsuariosRepresentados.get() == 0) {
            logger.debug("lanzarAltasRepresentantes - pending 0 requests");
            return;
        } 
        do {
            if((delegacionesEnviadas.get() - delegacionesRecogidas.get()) < 
                    MAX_PENDING_RESPONSES) {
                String userNIF = NifUtils.getNif(new Long(
                        userBaseData.getAndIncrementUserIndex()).intValue());
                String representativeNIF = getRandomRepresentative();
                if(representativeNIF == null) {
                    userBaseData.setCodigoEstado(Respuesta.SC_ERROR);
                    userBaseData.setMessage(ContextoPruebas.getString(
                            "userBaseWithouRepresentativesErrorMsg"));
                    break;
                } 
                requestCompletionService.submit(new RepresentativeDelegationLauncher(
                        userNIF, representativeNIF));
                delegacionesEnviadas.getAndIncrement();
            } else Thread.sleep(500);
        } while (delegacionesEnviadas.get() < numeroUsuariosRepresentados.get());
    }
    
    public void recogerRespuestas() throws InterruptedException, 
            ExecutionException, Exception {
        logger.debug("--------------------- recogerRespuestas ");
        for (int v = 0; v < numeroRepresentantes.get(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Respuesta alta de representante '" + v + "' statusCode: " + 
                    respuesta.getCodigoEstado() + " - mensaje: " + respuesta.getMensaje());
            altasRecogidas.getAndIncrement();
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                 representativeNifList.add(respuesta.getMensaje());
                 userBaseData.getAndIncrementNumAltas();
                 simulationListener.setSimulationMessage(getProgressMessage());
            } else {
                simulationListener.setSimulationErrorMessage(respuesta.getMensaje());
                logger.error("ERROR - statusCode: " + respuesta.getCodigoEstado() 
                        + " - msg: " + respuesta.getMensaje());
                userBaseData.getAndIncrementNumAltasERROR();
                simulationListener.setSimulationMessage(getProgressMessage());
                finalizar();
            } 
            
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
                 numeroUsuariosRepresentados.get());
        for (int v = 0; v < numeroUsuariosRepresentados.get(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Delegation response '" + v + "' - statusCode:" + 
                    respuesta.getCodigoEstado() + " - msg: " + respuesta.getMensaje());
            delegacionesRecogidas.getAndIncrement();
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                userWithRepresentativeList.add(respuesta.getMensaje());
                userBaseData.getAndIncrementNumDelegacionesOK();
                simulationListener.setSimulationMessage(getProgressMessage());
            } else {
                userBaseData.getAndIncrementNumDelegacionesERROR();
                simulationListener.setSimulationMessage(getProgressMessage());
                simulationListener.setSimulationErrorMessage(respuesta.getMensaje());
                finalizar();
            }  
        }
        finalizar();
    }
    
    private String getRandomRepresentative() { 
        if(representativeNifList.isEmpty()) return null;
        int randomSelected = new Random().nextInt(representativeNifList.size()); // In real life, the Random object should be rather more shared than this
        return representativeNifList.get(randomSelected);
    }
    
    public UserBaseData getUserBaseData() {
        return userBaseData;
    }
    
    public void finalizar() {
        try {
            countDownLatch.await();
            logger.debug("finalizar");
            duracion = System.currentTimeMillis() - comienzo;
            userBaseData.setComienzo(comienzo);
            userBaseData.setDuracion(duracion);
            userBaseData.setUsersWithRepresentativeList(userWithRepresentativeList);
            userBaseData.setUsersWithoutRepresentativeList(userWithoutRepresentativeList);
            userBaseData.setRepresentativeNifList(representativeNifList);
            requestExecutor.shutdownNow();
            simulationListener.setSimulationResult(this, userBaseData);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private String getProgressMessage() {
        return "<html>" + altasRecogidas.get() + " de " + 
                 numeroRepresentantes.get() + " representantes<br/>"+  
                 delegacionesRecogidas.get()+ " de " + numeroUsuariosRepresentados.get() + 
                " usuarios<html>";
    }

}