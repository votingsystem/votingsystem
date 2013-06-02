package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.simulation.launcher.VotingLauncher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import javax.swing.Timer;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.VotingSimulationData;
import org.sistemavotacion.test.modelo.UserBaseData;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingSimulator implements ActionListener, 
        Simulator<VotingSimulationData> {
    
    private static Logger logger = LoggerFactory.getLogger(VotingSimulator.class);
    
    private static ExecutorService votacionExecutor;
    
    private static ExecutorService votosExecutor;
    private static CompletionService<Respuesta> votosCompletionService;

    private Timer timer;
    private static List<String> accessRequestErrorsList;
    private static List<String> voteErrorsList;    
    private UserBaseData userBaseData = null;
    private VotingSimulationData simulationData = null;
    private Evento evento;
    
    private List<String> electorList = null;
    
    private SimulatorListener simulationListener;
    
    public VotingSimulator(VotingSimulationData simulationData, 
            SimulatorListener simulationListener) {
        this.evento = simulationData.getEvento();
        this.userBaseData = simulationData.getUserBaseData();
        this.simulationListener = simulationListener;
        votacionExecutor = Executors.newFixedThreadPool(10);
        votosExecutor = Executors.newFixedThreadPool(100);
        votosCompletionService = new ExecutorCompletionService<Respuesta>(votosExecutor);
        electorList = getElectorList(userBaseData);
        simulationData.setNumberOfElectors(electorList.size());
        this.simulationData = simulationData;
    }
    
    private List<String> getElectorList(UserBaseData userBaseData) {
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
    
    public void init() {
        logger.debug("lanzarVotacion - total number of electors: " +  
                simulationData.getNumberOfElectors());
        simulationData.setBegin(System.currentTimeMillis());
        votacionExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    beginElection();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        votacionExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    validateReceipts();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }
    
    public void beginElection () throws Exception {
        logger.debug(" ******* beginElection");
        if(userBaseData.isTimerBased()) {
            Long milisegundosHoras = 1000 * 60 * 60 * new Long(userBaseData.getHorasDuracionVotacion());
            Long milisegundosMinutos = 1000 * 60 * new Long(userBaseData.getMinutosDuracionVotacion()); 
            Long totalMilisegundosSimulacion = milisegundosHoras + milisegundosMinutos;
            Long intervaloLanzamiento = totalMilisegundosSimulacion/
                    simulationData.getNumberOfElectors();
            timer = new Timer(intervaloLanzamiento.intValue(), this);
            timer.setRepeats(true);
            timer.start();
        } else {
            while(!electorList.isEmpty()) {
                if((simulationData.getNumVotingRequests() - 
                        simulationData.getNumVotingRequestsColected()) < 
                        simulationData.getMaxPendingResponses()) {
                    int randomElector = new Random().nextInt(electorList.size());
                    lanzarSolicitudAcceso(electorList.remove(randomElector));
                } else Thread.sleep(500);
            }
        }
    }   
     
    public void lanzarSolicitudAcceso (String nif) throws Exception {
        Evento voto = evento.genRandomVote(ContextoPruebas.DIGEST_ALG);
        Usuario usuario = new Usuario(nif);
        voto.setUsuario(usuario);
        votosCompletionService.submit(new VotingLauncher(voto));
    }

    private void addAccessRequestErrorMsg(String msg) {
        if(accessRequestErrorsList == null) 
            accessRequestErrorsList = new ArrayList<String>();
        accessRequestErrorsList.add(msg);
    }
    
    private void addVotingErrorMsg(String msg) {
        if(voteErrorsList == null) 
            voteErrorsList = new ArrayList<String>();
        voteErrorsList.add(msg);
    }
        
    public void validateReceipts () throws Exception {
        logger.debug("******** validateReceipts");
        while (simulationData.getNumberOfElectors() > 
                simulationData.getNumVotingRequestsColected()) {
            String nifFrom = null;
            try {
                Future<Respuesta> f = votosCompletionService.take();
                Respuesta respuesta = f.get();
                nifFrom = respuesta.getEvento().getUsuario().getNif();
                if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    ReciboVoto reciboVoto = respuesta.getReciboVoto();
                    File recibo = new File(ContextoPruebas.getUserDirPath(nifFrom)
                        + ContextoPruebas.RECIBO_FILE + respuesta.getEvento().getEventoId() + ".p7m");
                    FileUtils.copy(reciboVoto.getArchivoRecibo(), recibo);
                    logger.debug("OK - Recibo de voto en " + recibo.getAbsolutePath());
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
                }
            } catch (Exception ex) {
                logger.error("ERROR from nif: " + nifFrom + ex.getMessage(), ex);
                simulationData.getAndIncrementNumVotingRequestsERROR();
            }
        }
        finish();
    }
    
    @Override public VotingSimulationData finish() throws Exception {
        logger.debug("finish - shutdown executors");
        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        votacionExecutor.shutdownNow();
        votosExecutor.shutdownNow(); 
        if(simulationListener != null) {           
            simulationListener.setSimulationResult(this);
        } else {
            logger.debug("--------------- SIMULATION RESULT------------------");
            logger.info("Duration: " + simulationData.getDurationStr());
            logger.info("Number of requests projected: " + simulationData.getNumberOfElectors());
            logger.info("Solicitudes con error: " + simulationData.getNumAccessRequestsERROR());
            logger.info("Votos enviados: " + simulationData.getNumVotingRequests());
            logger.info("Votos validos: " +  simulationData.getNumVotingRequestsOK());
            logger.info("Votos con error: " + simulationData.getNumVotingRequestsERROR());
            if(accessRequestErrorsList != null && 
                    !accessRequestErrorsList.isEmpty()) {
                logger.info("Access request errors: \n" + 
                    getFormattedErrorList(accessRequestErrorsList));
            } 
             if(voteErrorsList != null && !voteErrorsList.isEmpty()) {
                logger.info("Vote errors: \n" + 
                        getFormattedErrorList(voteErrorsList));
             }            
            logger.debug("------------------- FINISHED ----------------------");
            System.exit(0);
        }
        return simulationData;
    }
    
    private String getFormattedErrorList(List<String> errorsList) {
        if(errorsList == null || errorsList.isEmpty()) return null;
        else {
            StringBuilder result = new StringBuilder("");
            for(String error:errorsList) {
                result.append(error + "\n");
            }
            return result.toString();
        }
    }
        
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(timer)) {
            if(!electorList.isEmpty()) {
                try {
                    int randomElector = new Random().nextInt(electorList.size());
                    lanzarSolicitudAcceso(electorList.remove(randomElector));
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } else timer.stop();
        }
    }


    @Override public VotingSimulationData getData() {
        return simulationData;
    }

    @Override public List<String> getErrorsList() {
        List<String> errosList = new ArrayList<String>();
        if(voteErrorsList != null) {
            errosList.add("------------------ VOTE ERROR LIST -------------\n");
            errosList.addAll(voteErrorsList);
        } 
        if(accessRequestErrorsList != null) {
            errosList.add("--------- ACCESS REQUEST ERROR LIST ------------\n");
            errosList.addAll(accessRequestErrorsList);
        }
        return errosList;
    }
    
}