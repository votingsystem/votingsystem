package org.sistemavotacion.test.simulacion;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.Timer;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.smime.CMSUtils;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.InfoVoto;
import org.sistemavotacion.test.panel.VotacionesPanel;
import org.sistemavotacion.test.util.NifUtils;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class Votacion implements ActionListener {
    
    private static Logger logger = LoggerFactory.getLogger(Votacion.class);
    
    public static final int MAX_PENDING_REESPONSES = 10;
    
    private static ExecutorService votacionExecutor;

    private final ExecutorService solicitudesExecutor;
    private static CompletionService<InfoVoto> solicitudesCompletionService;
    
    private static ExecutorService votosExecutor;
    private static CompletionService<InfoVoto> votosCompletionService;
    
    private boolean lanzadaValidacionRecibos = false;
    
    private static AtomicLong numeroUsuarios;
    
    private static AtomicLong solicitudesEnviadas;
    private static AtomicLong solicitudesValidas;
    private static AtomicLong solicitudesConError;
    private static AtomicLong votosEnviados;
    private static AtomicLong votosRecogidos;
    private static AtomicLong votosValidos;
    private static AtomicLong votosConError;

    private static long comienzo;
    private static long duracion;
    private Evento evento;
    
    HashMap<Long, OpcionEvento> mapaOpciones = new HashMap<Long, OpcionEvento>();
    
    private Timer timer;
    private int idUsuario;
    private int idUsuarioInicial;
    private static StringBuilder erroresEnSolicitudes;
    private static StringBuilder erroresEnVotos;
    
    public Votacion(Evento evento) {
        this.evento = evento;
        for (OpcionEvento opcion : evento.getOpciones()) {
            mapaOpciones.put(opcion.getId(), opcion);
        }
        idUsuario = ContextoPruebas.getIdUsuario();
        logger.debug("idUsuario: " + idUsuario);
        idUsuarioInicial = ContextoPruebas.getIdUsuario();
        numeroUsuarios = new AtomicLong(ContextoPruebas.getNumeroTotalDeVotosParaLanzar());
        votacionExecutor = Executors.newFixedThreadPool(5);
        solicitudesExecutor = Executors.newFixedThreadPool(100);
        solicitudesCompletionService =
            new ExecutorCompletionService<InfoVoto>(solicitudesExecutor);
        votosExecutor = Executors.newFixedThreadPool(100);
        votosCompletionService = new ExecutorCompletionService<InfoVoto>(votosExecutor);
        solicitudesEnviadas = new AtomicLong();
        solicitudesValidas = new AtomicLong();
        solicitudesConError = new AtomicLong();
        votosEnviados = new AtomicLong();
        votosRecogidos = new AtomicLong();
        votosValidos = new AtomicLong();
        votosConError = new AtomicLong();
    }
    
    public void lanzarVotacion() {
        logger.debug("lanzarVotacion");
        comienzo = System.currentTimeMillis();
        erroresEnSolicitudes = new StringBuilder("<html>");
        erroresEnVotos = new StringBuilder("<html>");
        votacionExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.debug("Lanzado hilo de solicitudes");
                    lanzarSolicitudesAcceso();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        votacionExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.debug("Lanzado hilo de votos");
                    lanzarVotos();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });       
    }
    
     public void lanzarSolicitudesAcceso () throws Exception {
        if(ContextoPruebas.isSimulacionConTiempos()) {
            Long milisegundosHoras = 1000 * 60 * 60 * new Long(ContextoPruebas.getHorasDuracionVotacion());
            Long milisegundosMinutos = 1000 * 60 * new Long(ContextoPruebas.getMinutosDuracionVotacion()); 
            Long totalMilisegundosSimulacion = milisegundosHoras + milisegundosMinutos;
            Long intervaloLanzamiento = totalMilisegundosSimulacion/ContextoPruebas.getNumeroTotalDeVotosParaLanzar();
            timer = new Timer(intervaloLanzamiento.intValue(), this);
            timer.setRepeats(true);
            timer.start();
        } else {
            do {
                if((solicitudesEnviadas.get() - votosRecogidos.get()) < 
                        MAX_PENDING_REESPONSES) {
                    lanzarSolicitudAcceso(idUsuario++);
                } else Thread.sleep(500);
            } while (idUsuario < numeroUsuarios.get() + idUsuarioInicial);
        }
    }
     
     public void lanzarSolicitudAcceso (int numUsu) throws Exception {
        Evento voto = prepararVoto(evento);
        InfoVoto infoVoto = new InfoVoto(voto,NifUtils.getNif(numUsu));
        solicitudesCompletionService.submit(new LanzadoraSolicitudAcceso(infoVoto));
        solicitudesEnviadas.getAndIncrement();
        VotacionesPanel.INSTANCIA.actualizarContadorSolicitudes(
            new Long(solicitudesEnviadas.get()).intValue());
     }
    
    public void lanzarVotos () throws Exception {
        for (int v = 0; v < numeroUsuarios.get(); v++) {
            logger.debug("Lanzando voto");
            Future<InfoVoto> f = solicitudesCompletionService.take();
            final InfoVoto infoVoto = f.get();
            if (200 == infoVoto.getCodigoEstado()) {
                solicitudesValidas.getAndIncrement();
                votosCompletionService.submit(new LanzadoraVoto(infoVoto));
                votosEnviados.getAndIncrement();
                VotacionesPanel.INSTANCIA.actualizarContadorVotosLanzados(
                  new Long(votosEnviados.get()).intValue());
                if(!lanzadaValidacionRecibos) {
                        votacionExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                logger.debug("Lanzado hilo validación de recibos");
                                validarRecibos();
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    });
                    lanzadaValidacionRecibos = true;
                }
            } else {
                solicitudesConError.getAndIncrement();
                 VotacionesPanel.INSTANCIA.actualizarContadorSolicitudesError(
                         new Long(solicitudesConError.get()).intValue());
                String mensaje = "SOLICITUD CON ERROR - Usuario: " + infoVoto.getFrom() 
                        + " - Mensaje de Servidor: " + infoVoto.getMensaje();
                logger.error(mensaje);
                infoVoto.setErrorControlAcceso(true);
                erroresEnSolicitudes.append(mensaje + "<br/>");
            }   
        }
        logger.debug("Lanzados todos los votos - solicitudes validas: " + solicitudesValidas.get() 
                + " - solicitudes con error: " + solicitudesConError.get());
        if(ContextoPruebas.getNumeroTotalDeVotosParaLanzar() == solicitudesConError.get())
            finalizarVotacion();
    }

    public void validarRecibos () throws Exception {
        int numVoto = 1;
        do {
            try {
                Future<InfoVoto> f = votosCompletionService.take();
                InfoVoto infoVoto = f.get();
                votosRecogidos.getAndIncrement();
                VotacionesPanel.INSTANCIA.actualizarContadorVotosValidados(numVoto++);    
                ReciboVoto reciboVoto = infoVoto.getReciboVoto();
                if (200 == reciboVoto.getCodigoEstado()) {
                    votosValidos.getAndIncrement();
                    File recibo = new File(ContextoPruebas.getUserDirPath(infoVoto.getFrom())
                        + ContextoPruebas.RECIBO_FILE + infoVoto.getVoto().getEventoId() + ".p7m");
                    FileUtils.copy(reciboVoto.getArchivoRecibo(), recibo);
                    logger.debug("OK - Recibo de voto en " + recibo.getAbsolutePath());
                } else {
                    String mensaje = "Voto CON ERROR - Usuario: " + infoVoto.getFrom() 
                            + " - Mensaje de Servidor: " + infoVoto.getMensaje()
                            + " - Hash certificado: " + infoVoto.getVoto().getHashCertificadoVotoHex();
                    erroresEnVotos.append(mensaje + "<br/>");
                    votosConError.getAndIncrement();
                    VotacionesPanel.INSTANCIA.actualizarContadorVotosError(
                            new Long(votosConError.get()).intValue());
                    logger.error(mensaje);
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                votosConError.getAndIncrement();
                continue;
            }
        } while (ContextoPruebas.getNumeroTotalDeVotosParaLanzar() >
                (votosRecogidos.get() + solicitudesConError.get()));
        finalizarVotacion();
    }
    
    public void finalizarVotacion() {
        duracion = System.currentTimeMillis() - comienzo;
        logger.debug("finalizarVotacion - Apagando ejecutores");
        if(timer != null) timer.stop();
        ContextoPruebas.setIdUsuario(idUsuario);
        votacionExecutor.shutdownNow();
        solicitudesExecutor.shutdownNow();
        votosExecutor.shutdownNow(); 
        mostrarEstadisticas();
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
    
    private static void mostrarEstadisticas () {
        StringBuilder result = new StringBuilder("<html>");
        result.append("<b>Duración: </b>" + 
                DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duracion));
        logger.info("Solicitudes enviadas: " + solicitudesEnviadas.get());
        result.append("<br/><b>Solicitudes enviadas: </b>" + solicitudesEnviadas.get());
        logger.info("Solicitudes validas: " + solicitudesValidas.get());
        result.append("<br/><b>Solicitudes validas: </b>" + solicitudesValidas.get());
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
        VotacionesPanel.INSTANCIA.mostrarResultadosSimulacion(
                result.toString(), mensajeErroresEnSolicitudes, mensajeErroresEnVotos);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(timer)) {
            try {
                if(numeroUsuarios.get() >= idUsuario - idUsuarioInicial)
                    lanzarSolicitudAcceso(idUsuario++);
                else timer.stop();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    
}
