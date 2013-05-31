package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.simulation.launcher.AccessRequestLauncher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.Timer;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.CMSUtils;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.InfoVoto;
import org.sistemavotacion.test.modelo.SimulationData;
import org.sistemavotacion.test.modelo.UserBaseData;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 
* Simulation to test only access requests.
*/
public class AccessRequestSimulator implements ActionListener, Simulator {
    
    private static Logger logger = LoggerFactory.getLogger(AccessRequestSimulator.class);
    
    private static ExecutorService simulatorExecutor;
    
    private static ExecutorService accessRequestExecutor;
    private static CompletionService<InfoVoto> accessRequestCompletionService;

    
    private static AtomicLong requests;
    private static AtomicLong requestsFinished;
    private static AtomicLong requestsERROR;
    private static AtomicLong requestsOK;

    private static long begin;
    
    HashMap<Long, OpcionEvento> optionsMap = new HashMap<Long, OpcionEvento>();
    
    private Timer timer;
    private static StringBuilder errorsLog;
    private UserBaseData userBaseData = null;
    private SimulationData simulationData = null;
    
    private int numberRequests = 0;
    private List<String> electorList = null;
    
    private SimulatorListener simulationListener;
    
    private AtomicBoolean done = new AtomicBoolean(true);
    
    public AccessRequestSimulator(SimulationData simulationData, 
            SimulatorListener simulationListener) {
        this.userBaseData = simulationData.getUserBaseData(); 
        this.simulationData = simulationData;
        this.simulationListener = simulationListener;
        for (OpcionEvento opcion : userBaseData.getEvento().getOpciones()) {
            optionsMap.put(opcion.getId(), opcion);
        }
        simulatorExecutor = Executors.newFixedThreadPool(10);
        accessRequestExecutor = Executors.newFixedThreadPool(100);
        accessRequestCompletionService = 
                new ExecutorCompletionService<InfoVoto>(accessRequestExecutor);
        requests = new AtomicLong();
        requestsERROR = new AtomicLong();
        requestsFinished = new AtomicLong();
        requestsOK = new AtomicLong();
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
    
    public void init() {
        logger.debug("lanzarVotacion - total number of electors: " +  numberRequests);
        begin = System.currentTimeMillis();
        errorsLog = new StringBuilder("");
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
        if(userBaseData.isSimulacionConTiempos()) {
            Long milisegundosHoras = 1000 * 60 * 60 * new Long(userBaseData.getHorasDuracionVotacion());
            Long milisegundosMinutos = 1000 * 60 * new Long(userBaseData.getMinutosDuracionVotacion()); 
            Long totalMilisegundosSimulacion = milisegundosHoras + milisegundosMinutos;
            Long intervaloLanzamiento = totalMilisegundosSimulacion/numberRequests;
            timer = new Timer(intervaloLanzamiento.intValue(), this);
            timer.setRepeats(true);
            timer.start();
        } else {
             
            while(!electorList.isEmpty()) {
                if((requests.get() - requestsFinished.get()) < 
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
        Evento voto = prepararVoto(userBaseData.getEvento());
        InfoVoto infoVoto = new InfoVoto(voto, nif);
        accessRequestCompletionService.submit(new AccessRequestLauncher(infoVoto));
        requests.getAndIncrement();
    }


    public void validateReceipts () throws Exception {
        logger.debug("******** validateReceipts");
        while (numberRequests > requestsFinished.get()) {
            try {
                Future<InfoVoto> f = accessRequestCompletionService.take();
                requestsFinished.getAndIncrement();
                InfoVoto infoVoto = f.get();   
                
                if (Respuesta.SC_OK == infoVoto.getCodigoEstado()) {
                    ReciboVoto reciboVoto = infoVoto.getReciboVoto();
                    requestsOK.getAndIncrement();
                    logger.debug("Request OK");
                } else {
                    requestsERROR.getAndIncrement();
                    String mensaje = "Request ERROR - Usuario: " + infoVoto.getFrom() 
                            + " - msg: " + infoVoto.getMensaje();
                    logger.error(mensaje);
                    errorsLog.append(mensaje + "\n");
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                requestsERROR.getAndIncrement();
            }
            done.set(true);
        }
        finalizarVotacion();
    }
    
    public void finalizarVotacion() {
        long duration = System.currentTimeMillis() - begin;
        logger.debug("FINISH - shutdown executors");
        if(timer != null) timer.stop();
        simulatorExecutor.shutdownNow();
        accessRequestExecutor.shutdownNow(); 
        StringBuilder result = new StringBuilder("<html>");
        result.append("<b>Duración: </b>" + 
                DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration));
        logger.info("Solicitudes con error: " + requestsERROR.get());
        result.append("<br/><b>Solicitudes con error: </b>" + requestsERROR.get());
        logger.info("Solicitudes enviadas: " + requests.get());
        result.append("<br/><b>Solicitudes enviadas: </b>" + requestsERROR.get());
        logger.info("Solicitudes OK: " + requestsOK.get());
        result.append("<br/><b>Solicitudes OK: </b>" + requestsOK.get());
        String errorsMessage = null;
        if(requestsERROR.get() > 0) {
            errorsMessage = errorsLog.toString();
            logger.error("ERRORS: " + errorsMessage);
        }
        if(simulationListener != null) 
            simulationListener.setSimulationResult(this, errorsMessage);
        System.exit(0);
    }
        
    public Evento prepararVoto (Evento evento) throws NoSuchAlgorithmException {
        Evento voto = new Evento();
        voto.setAsunto(evento.getAsunto());
        voto.setCentroControl(evento.getCentroControl());
        voto.setContenido(evento.getContenido());
        voto.setControlAcceso(evento.getControlAcceso());
        voto.setEventoId(evento.getEventoId());
        voto.setOpciones(evento.getOpciones());
        voto.setOrigenHashSolicitudAcceso(UUID.randomUUID().toString());
        voto.setHashSolicitudAccesoBase64(CMSUtils.getHashBase64(
            voto.getOrigenHashSolicitudAcceso(), ContextoPruebas.DIGEST_ALG));
        voto.setOrigenHashCertificadoVoto(UUID.randomUUID().toString());
        voto.setHashCertificadoVotoBase64(CMSUtils.getHashBase64(
            voto.getOrigenHashCertificadoVoto(), ContextoPruebas.DIGEST_ALG));  
        voto.setOpcionSeleccionada(optionsMap.get(
        		getRandomOpcionSeleccionadaId(voto)));
        return voto;
    }
    
    private Long getRandomOpcionSeleccionadaId (Evento evento) {
        int size = evento.getOpciones().size();
        int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
        OpcionEvento opcionDeEvento = (OpcionEvento) evento.getOpciones().toArray()[item];
        return opcionDeEvento.getId();
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
    
    public static void main(String[] args) throws InterruptedException {
        try {
            ContextoPruebas.inicializar();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        
    }
    
}
