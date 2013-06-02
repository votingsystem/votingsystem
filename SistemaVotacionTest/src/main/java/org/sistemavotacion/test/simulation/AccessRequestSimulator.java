package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.simulation.launcher.AccessRequestLauncher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 
* Simulation to test only access requests.
*/
public class AccessRequestSimulator implements ActionListener, 
        Simulator<VotingSimulationData> {
    
    private static Logger logger = LoggerFactory.getLogger(AccessRequestSimulator.class);
    
    private static ExecutorService simulatorExecutor;
    
    private static ExecutorService accessRequestExecutor;
    private static CompletionService<Respuesta> accessRequestCompletionService;
    
    private Timer timer;
    private static List<String> errorsList;
    private UserBaseData userBaseData = null;
    private VotingSimulationData simulationData = null;
    private Evento evento = null;
    
    private int numberRequests = 0;
    private List<String> electorList = null;
    
    private SimulatorListener simulationListener;
    
    //private AtomicBoolean done = new AtomicBoolean(true);
    
    public AccessRequestSimulator(VotingSimulationData simulationData, 
            SimulatorListener simulationListener) {
        this.userBaseData = simulationData.getUserBaseData(); 
        this.simulationData = simulationData;
        this.simulationListener = simulationListener;
        this.evento = simulationData.getEvento();
        simulatorExecutor = Executors.newFixedThreadPool(10);
        accessRequestExecutor = Executors.newFixedThreadPool(100);
        accessRequestCompletionService = 
                new ExecutorCompletionService<Respuesta>(accessRequestExecutor);
        electorList = getElectorList(userBaseData);
        numberRequests = electorList.size();
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
    
    private void addErrorMsg(String msg) {
        if(errorsList == null) errorsList = new ArrayList<String>();
        errorsList.add(msg);
    }
    
    public void init() {
        logger.debug("lanzarVotacion - total number of electors: " +  numberRequests);
        simulationData.setBegin(System.currentTimeMillis());
        simulatorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    beginRequests();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        simulatorExecutor.execute(new Runnable() {
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
    
    public void beginRequests () throws Exception {
        logger.debug(" ******* beginRequests");
        if(userBaseData.isTimerBased()) {
            Long milisegundosHoras = 1000 * 60 * 60 * new Long(userBaseData.getHorasDuracionVotacion());
            Long milisegundosMinutos = 1000 * 60 * new Long(userBaseData.getMinutosDuracionVotacion()); 
            Long totalMilisegundosSimulacion = milisegundosHoras + milisegundosMinutos;
            Long intervaloLanzamiento = totalMilisegundosSimulacion/numberRequests;
            timer = new Timer(intervaloLanzamiento.intValue(), this);
            timer.setRepeats(true);
            timer.start();
        } else {
            while(!electorList.isEmpty()) {
                if((simulationData.getNumAccessRequests() - 
                        simulationData.getNumAccessRequestsColected()) < 
                        simulationData.getMaxPendingResponses()) {
                    int randomElector = new Random().nextInt(electorList.size());
                    lanzarSolicitudAcceso(electorList.remove(randomElector));
                } else Thread.sleep(1000);
                /*if(done.get()) {
                    int randomElector = new Random().nextInt(electorList.size());
                    lanzarSolicitudAcceso(electorList.remove(randomElector));
                    done.set(false);
                } else Thread.sleep(1000);*/
            }
        }
    }   
     
    public void lanzarSolicitudAcceso (String nif) throws Exception {
        Evento voto = evento.genRandomVote(ContextoPruebas.DIGEST_ALG);
        Usuario usuario = new Usuario(nif);
        voto.setUsuario(usuario);
        accessRequestCompletionService.submit(new AccessRequestLauncher(voto));
    }

    public void validateReceipts () throws Exception {
        logger.debug("******** validateReceipts");
        while (numberRequests > simulationData.getNumAccessRequestsColected()) {
            try {
                Future<Respuesta> f = accessRequestCompletionService.take();
                Respuesta respuesta = f.get();   
                String fromNif = respuesta.getEvento().getUsuario().getNif();
                if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    ReciboVoto reciboVoto = respuesta.getReciboVoto();
                    logger.debug("Request OK from nif: " + fromNif);
                    simulationData.getAndIncrementNumAccessRequestsOK();
                } else {
                    String msg = "Request ERROR - Usuario: " + fromNif + 
                            " - msg: " + respuesta.getMensaje();
                    logger.error(msg);
                    addErrorMsg(msg);
                    simulationData.getAndIncrementNumAccessRequestsERROR();
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                addErrorMsg(ex.getMessage());
                simulationData.getAndIncrementNumAccessRequestsERROR();
            }
            //done.set(true);
        }
        finish();
    }
    
    @Override public VotingSimulationData finish() throws Exception {
        logger.debug("finish");
        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        simulatorExecutor.shutdownNow();
        accessRequestExecutor.shutdownNow(); 
        if(simulationListener != null) {
            simulationListener.setSimulationResult(this);
        } else {
            logger.debug("--------------- SIMULATION RESULT------------------");
            logger.info("Duration: " + simulationData.getDurationStr());
            logger.info("Solicitudes enviadas: " + simulationData.getNumAccessRequests());
            logger.info("Solicitudes OK: " + simulationData.getNumAccessRequestsOK());
            logger.info("Solicitudes con error: " + simulationData.getNumAccessRequestsERROR());
            if(electorList != null && !electorList.isEmpty()) {
                logger.info("Access Request errors: \n" + 
                        getFormattedErrorList(electorList));
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

    @Override
    public VotingSimulationData getData() {
        return simulationData;
    }

    @Override
    public List<String> getErrorsList() {
        return errorsList;
    }
    
}
