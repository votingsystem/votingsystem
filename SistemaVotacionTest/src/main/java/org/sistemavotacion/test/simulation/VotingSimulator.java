package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.simulation.callable.Voter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.VotingSimulationData;
import org.sistemavotacion.test.modelo.UserBaseSimulationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingSimulator extends  Simulator<VotingSimulationData>
        implements ActionListener {
    
    private static Logger logger = LoggerFactory.getLogger(VotingSimulator.class);
    
    private static ExecutorService votacionExecutor;
    private static ExecutorService votosExecutor;
    private static CompletionService<Respuesta> votosCompletionService;

    private static List<String> accessRequesterrorList;
    private static List<String> voteerrorList;    
    private UserBaseSimulationData userBaseData = null;
    private VotingSimulationData simulationData = null;
    private Evento evento;
    
    private List<String> electorList = null;
    
    private SimulatorListener simulationListener;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    public VotingSimulator(VotingSimulationData simulationData, 
            SimulatorListener simulationListener) {
        super(simulationData);
        this.evento = simulationData.getEvento();
        this.simulationData = simulationData;
        this.userBaseData = simulationData.getUserBaseData();
        this.simulationListener = simulationListener;
    }
    
    public VotingSimulator(VotingSimulationData simulationData) {
        super(simulationData);
        this.evento = simulationData.getEvento();
        this.simulationData = simulationData;
        this.userBaseData = simulationData.getUserBaseData();
    }
    
    private List<String> getElectorList(UserBaseSimulationData userBaseData) {
        logger.debug("getElectorList");
        int totalNumberElectors = userBaseData.getNumberElectors();
        List<String> result = new ArrayList<String>(totalNumberElectors);
        
        List<String> representativesList = new ArrayList<String>(userBaseData.getRepresentativeNifList());
        int numberVotesRep = userBaseData.getNumVotesRepresentatives();
        if(numberVotesRep > 0 && !representativesList.isEmpty()) {
            for(int i = 0; i < numberVotesRep; i++) {
                int randomRep = new Random().nextInt(representativesList.size());
                result.add(representativesList.remove(randomRep));
            }
            logger.debug("Added '" + numberVotesRep + "' representatives to elector list");
        }
        
        List<String> userWithRepresentativesList = new ArrayList<String>(
                userBaseData.getUsersWithRepresentativeList());
        int numberVotesUserWithRepresentative = userBaseData.getNumVotesUsersWithRepresentative();
        if(numberVotesUserWithRepresentative > 0 && !userWithRepresentativesList.isEmpty()) {
            for(int i = 0; i < numberVotesUserWithRepresentative; i++) {
                int randomUser = new Random().nextInt(userWithRepresentativesList.size());
                result.add(userWithRepresentativesList.remove(randomUser));
            }
            logger.debug("Added '" + numberVotesUserWithRepresentative + "' " + 
                    "users WITH representatives to elector list");
        }
            
        List<String> userWithoutRepresentativesList = new ArrayList<String>(
                userBaseData.getUsersWithoutRepresentativeList());
        int numberVotesUserWithoutRepresentative = userBaseData.getNumVotesUsersWithoutRepresentative();
        if(numberVotesUserWithoutRepresentative > 0 && !userWithoutRepresentativesList.isEmpty()) {
            for(int i = 0; i < numberVotesUserWithoutRepresentative; i++) {
                int randomUser = new Random().nextInt(userWithoutRepresentativesList.size());
                result.add(userWithoutRepresentativesList.remove(randomUser));
            }
            logger.debug("Added '" + numberVotesUserWithoutRepresentative + "' " + 
                    "users WITHOUT representatives to elector list");
        }
        return result;
    }
    
    public void launchRequests () throws Exception {
        logger.debug(" - launchRequests");
        if(userBaseData.isTimerBased()) startTimer(this);
        else {
            while(!electorList.isEmpty()) {
                if((simulationData.getNumVotingRequests() - 
                        simulationData.getNumVotingRequestsColected()) < 
                        simulationData.getMaxPendingResponses()) {
                    int randomElector = new Random().nextInt(electorList.size());
                    launchAccessRequest(electorList.remove(randomElector));
                } else Thread.sleep(500);
            }
        }
    }   
     
    public void launchAccessRequest (String nif) throws Exception {
        Evento voto = evento.genRandomVote(ContextoPruebas.DIGEST_ALG);
        Usuario usuario = new Usuario(nif);
        voto.setUsuario(usuario);
        votosCompletionService.submit(new Voter(voto));
    }

    private void addAccessRequestErrorMsg(String msg) {
        if(accessRequesterrorList == null) 
            accessRequesterrorList = new ArrayList<String>();
        accessRequesterrorList.add(msg);
    }
    
    private void addVotingErrorMsg(String msg) {
        if(voteerrorList == null) 
            voteerrorList = new ArrayList<String>();
        voteerrorList.add(msg);
    }
        
    public void readResponses () throws Exception {
        logger.debug(" - readResponses - NumOfElectors: " + 
                simulationData.getNumOfElectors());
        while (simulationData.getNumOfElectors() > 
                simulationData.getNumVotingRequestsColected()) {
            String nifFrom = null;
            try {
                Future<Respuesta> f = votosCompletionService.take();
                Respuesta respuesta = f.get();
                nifFrom = respuesta.getEvento().getUsuario().getNif();
                if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    ReciboVoto reciboVoto = respuesta.getReciboVoto();
                    logger.debug("OK - Recibo de voto en ");
                    simulationData.getAndIncrementNumVotingRequestsOK();
                } else {
                    String mensaje = "ERROR - Usuario: " + nifFrom + 
                            " - Msg: " + respuesta.getMensaje();
                    logger.error(mensaje);
                    if(respuesta.getData() != null) { 
                        addAccessRequestErrorMsg(mensaje);
                        simulationData.getAndIncrementNumAccessRequestsERROR();
                    } else {
                        addVotingErrorMsg(mensaje);
                        simulationData.getAndIncrementNumVotingRequestsERROR();
                    }
                    //=======
                    break;
                }
            } catch (Exception ex) {
                logger.error("ERROR from nif: " + nifFrom + ex.getMessage(), ex);
                simulationData.getAndIncrementNumVotingRequestsERROR();
            }
        }
        countDownLatch.countDown();
    }
        
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(timer)) {
            if(!electorList.isEmpty()) {
                try {
                    int randomElector = new Random().nextInt(electorList.size());
                    launchAccessRequest(electorList.remove(randomElector));
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } else timer.stop();
        }
    }


    @Override public List<String> geterrorList() {
        List<String> errosList = new ArrayList<String>();
        if(voteerrorList != null) {
            errosList.add("------------------ VOTE ERROR LIST -------------\n");
            errosList.addAll(voteerrorList);
        } 
        if(accessRequesterrorList != null) {
            errosList.add("--------- ACCESS REQUEST ERROR LIST ------------\n");
            errosList.addAll(accessRequesterrorList);
        }
        return errosList;
    }

    @Override
    public VotingSimulationData call() throws Exception {
        electorList = getElectorList(userBaseData);
        simulationData.setNumOfElectors(electorList.size());
        
        logger.debug("lanzarVotacion - total number of electors: " +  
                simulationData.getNumOfElectors());
        votacionExecutor = Executors.newFixedThreadPool(5);
        votosExecutor = Executors.newFixedThreadPool(100);
        votosCompletionService = new ExecutorCompletionService<Respuesta>(votosExecutor);
        simulationData.setBegin(System.currentTimeMillis());
        votacionExecutor.execute(new Runnable() {
            @Override public void run() {
                try {
                    launchRequests();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        votacionExecutor.execute(new Runnable() {
            @Override public void run() {
                try {
                    readResponses();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        
        countDownLatch.await();
        logger.debug("- shutdown executors");
        
        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        if(votacionExecutor != null) votacionExecutor.shutdownNow();
        if(votosExecutor != null) votosExecutor.shutdownNow(); 
        logger.debug("--------------- SIMULATION RESULT------------------");
        logger.info("Duration: " + simulationData.getDurationStr());
        logger.info("Number of requests projected: " + simulationData.getNumOfElectors());
        logger.info("Solicitudes con error: " + simulationData.getNumAccessRequestsERROR());
        logger.info("Votos enviados: " + simulationData.getNumVotingRequests());
        logger.info("Votos validos: " +  simulationData.getNumVotingRequestsOK());
        logger.info("Votos con error: " + simulationData.getNumVotingRequestsERROR());
        if(accessRequesterrorList != null && 
                !accessRequesterrorList.isEmpty()) {
            logger.info("Access request errors: \n" + 
                getFormattedErrorList(accessRequesterrorList));
        } 
         if(voteerrorList != null && !voteerrorList.isEmpty()) {
            logger.info("Vote errors: \n" + 
                    getFormattedErrorList(voteerrorList));
         }            
        String errorsMsg = getFormattedErrorList();
        if(errorsMsg != null) {
            logger.info(" ************* " + geterrorList().size() + " ERRORS: \n" + 
                        errorsMsg);
        }
        logger.debug("------------------- FINISHED --------------------------");
        if(simulationListener != null) simulationListener.
                setSimulationResult(simulationData);
        
        simulationData.setStatusCode(Respuesta.SC_OK);
        return simulationData;
    }

    
}