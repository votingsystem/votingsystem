package org.sistemavotacion.test.simulation;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.Certificate;
import org.sistemavotacion.test.simulation.callable.RepresentativeRequestor;
import org.sistemavotacion.test.simulation.callable.RepresentativeDelegator;
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
import org.sistemavotacion.test.modelo.VotingSimulationData;
import org.sistemavotacion.test.simulation.callable.AccessControlInitializer;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import java.security.cert.X509Certificate;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.CertUtil;
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
    
    private final CountDownLatch usersWithoutRepresentativeLatch = new CountDownLatch(1);
    private final CountDownLatch repLatch = new CountDownLatch(1);
    
    List<String> representativeNifList = new ArrayList<String>();
    List<String> userWithRepresentativeList = new ArrayList<String>();
    List<String> userWithoutRepresentativeList = new ArrayList<String>();
    

    public UserBaseDataSimulator(UserBaseSimulationData simulationData, 
            SimulatorListener<UserBaseSimulationData> simulationListener) {
        super(simulationData);
        this.simulationListener = simulationListener;
        requestExecutor = Executors.newFixedThreadPool(100);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(requestExecutor);
    }
    
    public UserBaseDataSimulator(UserBaseSimulationData simulationData) {
        super(simulationData);
        this.simulationData = simulationData;
        requestExecutor = Executors.newFixedThreadPool(100);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(requestExecutor);
    }


    private void createUsersWithoutRepresentative() {
        logger.debug("createUsersWithoutRepresentative - users without representative:" +  
                simulationData.getNumVotesUsersWithoutRepresentative());
        for(int i = 1; i <= simulationData.
                getNumVotesUsersWithoutRepresentative(); i++ ) {
            int userIndex = new Long(simulationData.getAndIncrementUserIndex()).intValue();
            try {
                String userNif = NifUtils.getNif(userIndex);
                KeyStore keyStore = ContextoPruebas.INSTANCE.crearMockDNIe(userNif);
                userWithoutRepresentativeList.add(userNif);
                
                Certificate[] chain = keyStore.getCertificateChain(
                        ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS);
                X509Certificate usertCert = (X509Certificate) chain[0]; 
                byte[] usertCertPEMBytes = CertUtil.fromX509CertToPEM(usertCert);
                
                String certServiceURL = ContextoPruebas.INSTANCE.
                    getUserCertServiceURL();
                DocumentSenderWorker worker = new DocumentSenderWorker(null, 
                    usertCertPEMBytes, Contexto.X509_CONTENT_TYPE, certServiceURL, null);
                worker.execute();
                worker.get();
                if(Respuesta.SC_OK != worker.getStatusCode()) {
                    errorList.add(worker.getMessage());
                    return;
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                addErrorMsg(ex.getMessage());
            }
            if((i % 50) == 0) logger.debug("Created " + i + " of " + 
                    simulationData.getNumVotesUsersWithoutRepresentative() + " mock DNIe certs");
        }
        usersWithoutRepresentativeLatch.countDown();
    }
    
    public void launchRepresentativeRequests () throws Exception {
        logger.debug("launchRepresentativeRequests - NumRepresentatives: " + 
                simulationData.getNumRepresentatives());
        if(simulationData.getNumRepresentatives() == 0) {
            logger.debug("launchRepresentativeRequests - WITHOUT REPRESENTATIVE REQUESTS");
            return;
        } 
        while (simulationData.getNumRepresentativeRequests() < 
                simulationData.getNumRepresentatives()){
            if((simulationData.getNumRepresentativeRequests() - 
                    simulationData.getNumRepresentativeRequestsColected()) < 
                    MAX_PENDING_RESPONSES) {
                requestCompletionService.submit(new RepresentativeRequestor(
                        NifUtils.getNif(new Long(simulationData.
                        getAndIncrementUserIndex()).intValue())));
                simulationData.getAndIncrementNumRepresentativeRequests();
            } else Thread.sleep(500);
        } 
        logger.debug("launchRepresentativeRequests - NumRepresentatives requests: " 
                +  simulationData.getNumRepresentativeRequests());
    }
    
    public void launchDelegationRequests () throws Exception {
        logger.debug("launchDelegationRequests - NumUsersWithRepresentative: " + 
                simulationData.getNumUsersWithRepresentative()); 
        if(simulationData.getNumUsersWithRepresentative() == 0) {
            logger.debug("launchRepresentativeRequests - WITHOUT DELEGATION REQUESTS");
            return;
        } 
        while (simulationData.getNumDelegationRequests() < 
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
                requestCompletionService.submit(new RepresentativeDelegator(
                        userNIF, representativeNIF));
                simulationData.getAndIncrementNumDelegationRequests();
            } else Thread.sleep(200);
        }
    }
    
    public void readResponses() throws InterruptedException, 
            ExecutionException, Exception {
        logger.debug("- readResponses for num NumRepresentatives: " + 
                simulationData.getNumRepresentatives());
        for (int v = 0; v < simulationData.getNumRepresentatives(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Respuesta alta de representante '" + v + "' statusCode: " + 
                    respuesta.getCodigoEstado() + " - mensaje: " + respuesta.getMensaje());
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                 representativeNifList.add(respuesta.getMensaje());
                 simulationData.getAndIncrementNumRepresentativeRequestsOK();
            } else {
                logger.error("ERROR - statusCode: " + respuesta.getCodigoEstado() 
                        + " - msg: " + respuesta.getMensaje());
                addErrorMsg(respuesta.getMensaje());
                simulationData.getAndIncrementNumRepresentativeRequestsERROR();
            }
            if(simulationListener != null)
                simulationListener.updateSimulationData(simulationData);
        }

        if(!errorList.isEmpty()) {
            repLatch.countDown();
            return;
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
        if(simulationData.getNumRepresentatives() == 0) {
            logger.debug("launchRepresentativeRequests - There can't be delegation" +
                    " without representative requests - exit");
            
        } else {
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
                if(simulationListener != null) simulationListener.
                        updateSimulationData(simulationData);
            }        
        }
        repLatch.countDown();
    }
    
    private String getRandomRepresentative() { 
        if(representativeNifList.isEmpty()) return null;
        int randomSelected = new Random().nextInt(representativeNifList.size()); // In real life, the Random object should be rather more shared than this
        return representativeNifList.get(randomSelected);
    }

    @Override public UserBaseSimulationData call() throws Exception {
        logger.debug("call");
        simulationData.setStatusCode(Respuesta.SC_OK);
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
                    createUsersWithoutRepresentative();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        
        usersWithoutRepresentativeLatch.await();
        repLatch.await();
        
        if(errorList.size() > 0) simulationData.setStatusCode(Respuesta.SC_ERROR);

        simulationData.setFinish(System.currentTimeMillis());
        simulationData.setUsersWithRepresentativeList(userWithRepresentativeList);
        simulationData.setUsersWithoutRepresentativeList(userWithoutRepresentativeList);
        simulationData.setRepresentativeNifList(representativeNifList);
        if(requestExecutor != null) requestExecutor.shutdownNow();
        logger.debug("--------------- UserBaseDataSimulator ----------------------");   
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
        logger.info("userWithoutRepresentative: " + 
                userWithoutRepresentativeList.size()); 

        
        String errorsMsg = getFormattedErrorList();
        if(errorsMsg != null) {
            logger.info(" ************* " + geterrorList().size() + " ERRORS: \n" + 
                        errorsMsg);
        }
        logger.debug("-------------------------------------------------------");
        if(simulationListener != null)
            simulationListener.setSimulationResult(simulationData);
        
        return simulationData;
    }


    public static void main(String[] args) throws Exception {
        VotingSimulationData simulationData = null;
        if(args != null && args.length > 0) {
            logger.debug("args[0]");
            simulationData = VotingSimulationData.parse(args[0]);
        } else {
            File jsonFile = File.createTempFile("UserBaseDataSimulator", ".json");
            jsonFile.deleteOnExit();
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("simulatorFiles/votingSimulationData.json"), jsonFile); 
            simulationData = VotingSimulationData.parse(FileUtils.getStringFromFile(jsonFile));
            logger.debug("Simulation for Access Control: " + simulationData.getAccessControlURL());
        }
        AccessControlInitializer accessControlInitializer = 
                new AccessControlInitializer(simulationData.getAccessControlURL());
        Respuesta respuesta = accessControlInitializer.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            UserBaseDataSimulator simuHelper = new 
            UserBaseDataSimulator(simulationData.getUserBaseData(), null);
            simuHelper.call();
        } else logger.error(respuesta.getMensaje());
        
        

    }
}