package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.simulation.launcher.VotingLauncher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
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
import org.sistemavotacion.test.panel.VotacionesPanel;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingSimulator implements ActionListener, Simulator {
    
    private static Logger logger = LoggerFactory.getLogger(VotingSimulator.class);
    
    private static ExecutorService votacionExecutor;
    
    private static ExecutorService votosExecutor;
    private static CompletionService<InfoVoto> votosCompletionService;
    
    private static AtomicLong votosEnviados;
    private static AtomicLong votosRecogidos;
    private static AtomicLong solicitudesConError;
    private static AtomicLong votosConError;
    private static AtomicLong votosValidos;
   
    private static long comienzo;
    
    HashMap<Long, OpcionEvento> mapaOpciones = new HashMap<Long, OpcionEvento>();
    
    private Timer timer;
    private static StringBuilder erroresEnSolicitudes;
    private static StringBuilder erroresEnVotos;    
    private UserBaseData userBaseData = null;
    private SimulationData simulationData = null;
    
    private int numberOfElectors = 0;
    private List<String> electorList = null;
    
    private SimulatorListener simulationListener;
    
    public VotingSimulator(UserBaseData userBaseData, SimulatorListener simulationListener) {

    }

    VotingSimulator(SimulationData simulationData, 
            VotingProcessSimulator simulationListener) {
        this.simulationData = simulationData;
        this.userBaseData = simulationData.getUserBaseData();
        this.simulationListener = simulationListener;
        for (OpcionEvento opcion : userBaseData.getEvento().getOpciones()) {
            mapaOpciones.put(opcion.getId(), opcion);
        }
        votacionExecutor = Executors.newFixedThreadPool(10);
        votosExecutor = Executors.newFixedThreadPool(100);
        votosCompletionService = new ExecutorCompletionService<InfoVoto>(votosExecutor);
        solicitudesConError = new AtomicLong();
        votosEnviados = new AtomicLong();
        votosRecogidos = new AtomicLong();
        votosValidos = new AtomicLong();
        votosConError = new AtomicLong();
        electorList = getElectorList(userBaseData);
        numberOfElectors = electorList.size();
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
        logger.debug("lanzarVotacion - total number of electors: " +  numberOfElectors);
        comienzo = System.currentTimeMillis();
        erroresEnSolicitudes = new StringBuilder("<html>");
        erroresEnVotos = new StringBuilder("<html>");
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
        if(userBaseData.isSimulacionConTiempos()) {
            Long milisegundosHoras = 1000 * 60 * 60 * new Long(userBaseData.getHorasDuracionVotacion());
            Long milisegundosMinutos = 1000 * 60 * new Long(userBaseData.getMinutosDuracionVotacion()); 
            Long totalMilisegundosSimulacion = milisegundosHoras + milisegundosMinutos;
            Long intervaloLanzamiento = totalMilisegundosSimulacion/numberOfElectors;
            timer = new Timer(intervaloLanzamiento.intValue(), this);
            timer.setRepeats(true);
            timer.start();
        } else {
            while(!electorList.isEmpty()) {
                if((votosEnviados.get() - votosRecogidos.get()) < 
                        simulationData.getMaxPendingResponses()) {
                    int randomElector = new Random().nextInt(electorList.size());
                    lanzarSolicitudAcceso(electorList.remove(randomElector));
                } else Thread.sleep(500);
            }
        }
    }   
     
    public void lanzarSolicitudAcceso (String nif) throws Exception {
        Evento voto = prepararVoto(userBaseData.getEvento());
        InfoVoto infoVoto = new InfoVoto(voto, nif);
        votosCompletionService.submit(new VotingLauncher(infoVoto));
        votosEnviados.getAndIncrement();
        if(VotacionesPanel.INSTANCIA != null)
                VotacionesPanel.INSTANCIA.actualizarContadorVotosLanzados(
                new Long(votosEnviados.get()).intValue());
    }


    public void validateReceipts () throws Exception {
        logger.debug("******** validateReceipts");
        while (numberOfElectors > votosRecogidos.get()) {
            try {
                Future<InfoVoto> f = votosCompletionService.take();
                votosRecogidos.getAndIncrement();
                InfoVoto infoVoto = f.get();
                if(VotacionesPanel.INSTANCIA != null)
                        VotacionesPanel.INSTANCIA.actualizarContadorVotosValidados(
                        new Long(votosRecogidos.get()).intValue());    
                
                if (Respuesta.SC_OK == infoVoto.getCodigoEstado()) {
                    ReciboVoto reciboVoto = infoVoto.getReciboVoto();
                    votosValidos.getAndIncrement();
                    File recibo = new File(ContextoPruebas.getUserDirPath(infoVoto.getFrom())
                        + ContextoPruebas.RECIBO_FILE + infoVoto.getVoto().getEventoId() + ".p7m");
                    FileUtils.copy(reciboVoto.getArchivoRecibo(), recibo);
                    logger.debug("OK - Recibo de voto en " + recibo.getAbsolutePath());
                } else {
                    String mensaje = "Voto CON ERROR - Usuario: " + infoVoto.getFrom() 
                            + " - Msg: " + infoVoto.getMensaje()
                            + " - Hash certificado: " + infoVoto.getVoto().getHashCertificadoVotoHex();
                    logger.error(mensaje);
                    switch(infoVoto.getError()) {
                        case ACCESS_REQUEST:
                            solicitudesConError.getAndIncrement();
                            if(VotacionesPanel.INSTANCIA != null)
                                VotacionesPanel.INSTANCIA.actualizarContadorSolicitudesError(
                                new Long(solicitudesConError.get()).intValue());
                            break;
                        case VOTE:
                            votosConError.getAndIncrement();
                            if(VotacionesPanel.INSTANCIA != null)
                                VotacionesPanel.INSTANCIA.actualizarContadorVotosError(
                                new Long(votosConError.get()).intValue());
                            erroresEnVotos.append(mensaje + "<br/>");
                            break;
                    }

                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                votosConError.getAndIncrement();
            }
        }
        finalizarVotacion();
    }
    
    public void finalizarVotacion() {
        long duracion = System.currentTimeMillis() - comienzo;
        logger.debug("finalizarVotacion - shutdown executors");
        if(timer != null) timer.stop();
        votacionExecutor.shutdownNow();
        //solicitudesExecutor.shutdownNow();
        votosExecutor.shutdownNow(); 
        StringBuilder result = new StringBuilder("<html>");
        String durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duracion);
        logger.info("Duration: " + durationStr);
        result.append("<b>Duración: </b>" + durationStr);
        logger.info("Solicitudes con error: " + solicitudesConError.get());
        result.append("<br/><b>Solicitudes con error: </b>" + solicitudesConError.get());
        logger.info("Votos enviados: " + votosEnviados.get());
        result.append("<br/><b>Votos enviados: </b>" + votosEnviados.get());
        logger.info("Votos validos: " + votosValidos.get());
        result.append("<br/><b>Votos validos: </b>" + votosValidos.get());
        logger.info("Votos con error: " + votosConError.get());
        result.append("<br/><b>Votos con error: </b>" + votosConError.get());
        String mensajeErroresEnSolicitudes = null;
        String mensajeErroresEnVotos = null;
        if(solicitudesConError.get() > 0) {
            mensajeErroresEnSolicitudes = erroresEnSolicitudes.toString();
        }
        if(votosConError.get() > 0) {
            mensajeErroresEnVotos = erroresEnVotos.toString();
        }
        if(VotacionesPanel.INSTANCIA != null)
                VotacionesPanel.INSTANCIA.mostrarResultadosSimulacion(
                result.toString(), mensajeErroresEnSolicitudes, mensajeErroresEnVotos);
        if(simulationListener != null) 
            simulationListener.setSimulationResult(this, result.toString());
    }
        
    public synchronized Evento prepararVoto (Evento evento) 
            throws NoSuchAlgorithmException {
        Evento voto = new Evento();
        voto.setAsunto(evento.getAsunto());
        voto.setCentroControl(evento.getCentroControl());
        voto.setContenido(evento.getContenido());
        voto.setControlAcceso(evento.getControlAcceso());
        voto.setEventoId(evento.getEventoId());
        voto.setOpciones(evento.getOpciones());
        String origenHashSolicitudAcceso = UUID.randomUUID().toString();
        voto.setOrigenHashSolicitudAcceso(origenHashSolicitudAcceso);
        voto.setHashSolicitudAccesoBase64(CMSUtils.getHashBase64(
            origenHashSolicitudAcceso, ContextoPruebas.DIGEST_ALG));
        String origenHashCertificadoVoto = UUID.randomUUID().toString();
        voto.setOrigenHashCertificadoVoto(origenHashCertificadoVoto);
        voto.setHashCertificadoVotoBase64(CMSUtils.getHashBase64(
            origenHashCertificadoVoto, ContextoPruebas.DIGEST_ALG));  
        voto.setOpcionSeleccionada(mapaOpciones.get(
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
    
    
}
