package org.sistemavotacion.test.simulacion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.json.DeJSONAObjeto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.util.NifUtils;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PruebasSellosDeTiempo implements VotingSystemWorkerListener {
        
    private static Logger logger = LoggerFactory.getLogger(PruebasSellosDeTiempo.class);
    
    public static final int MAX_PENDING_RESPONSES = 10;
        
    
    private final ExecutorService requestExecutor;
    private static CompletionService<Respuesta> requestCompletionService;
    
    private static AtomicLong numeroSolicitudes;
    private static AtomicLong solicitudesEnviadas;
    private static AtomicLong solicitudesRecogidas;
    private int solicitudesOK = 0;
    private int solicitudesERROR = 0;
    
    private static long comienzo;
    
    List<String> representativeNifList = new ArrayList<String>();
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    

    public PruebasSellosDeTiempo(Long numSolicitudes, String urlInfoServidor) {
        logger.debug("numeroSolicitudes '" + numSolicitudes + "'");
        new InfoGetterWorker(null, urlInfoServidor, this).execute();
        
        solicitudesEnviadas = new AtomicLong(0);
        solicitudesRecogidas = new AtomicLong(0);
        numeroSolicitudes =  new AtomicLong(numSolicitudes);
        requestExecutor = Executors.newFixedThreadPool(1000);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(requestExecutor);
        
    }
    
    public void lanzar() throws InterruptedException {
        logger.debug("lanzar"); 
        countDownLatch.await();
        comienzo = System.currentTimeMillis();
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    lanzarSolicitudes();                    
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
    }
    
    public void lanzarSolicitudes () throws Exception {
        logger.debug("****************lanzarSolicitudes");
        if(numeroSolicitudes.get() == 0) {
            logger.debug("lanzarSolicitudes - SIN PETICIONES PENDIENTES");
            return;
        } 
        do {
            if((solicitudesEnviadas.get() - solicitudesRecogidas.get()) < 
                    MAX_PENDING_RESPONSES) {
                requestCompletionService.submit(new LanzadoraSelloTiempo(
                        NifUtils.getNif(new Long(
                        solicitudesEnviadas.getAndIncrement()).intValue())));
                logger.debug("lanzarSolicitudes - lanzada -> " + solicitudesEnviadas.get());
            } else Thread.sleep(500);
        } while (solicitudesEnviadas.get() < numeroSolicitudes.get());
    }
    
    public void recogerRespuestas() throws InterruptedException, 
            ExecutionException, Exception {
        logger.debug("--------------------- recogerRespuestas ");
        for (int v = 0; v < numeroSolicitudes.get(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Respuesta solicitud sello tiempo '" + v + "' statusCode: " + 
                    respuesta.getCodigoEstado() + " - mensaje: " + respuesta.getMensaje());
            if(respuesta.getCodigoEstado() == Respuesta.SC_OK) {
                solicitudesOK++;
            } else {
                solicitudesERROR++;
                finalizar();
            }
            solicitudesRecogidas.getAndIncrement();
        }
        finalizar();
    }
    
    public void finalizar() {
        logger.debug("--------------------- finalizar -----------------------");
        long duracion = System.currentTimeMillis() - comienzo;
        String duracionStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duracion);
        logger.debug("duracionStr: " + duracionStr);
        logger.debug("solicitudesOK: " + solicitudesOK);
        logger.debug("solicitudesERROR: " + solicitudesERROR);
        requestExecutor.shutdownNow();
    }
    
    public static void main(String[] args) throws InterruptedException {
        try {
            ContextoPruebas.inicializar();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        String urlServidor = "http://localhost:8080/SistemaVotacionControlAcceso/infoServidor";
        PruebasSellosDeTiempo pruebasSellosDeTiempo = new PruebasSellosDeTiempo(
                new Long(100), urlServidor);
        pruebasSellosDeTiempo.lanzar();
        
    }

    @Override
    public void process(List<String> messages) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showResult(VotingSystemWorker worker) {         
        if(Respuesta.SC_OK == worker.getStatusCode()) {
            try {
                ActorConIP actorConIP = DeJSONAObjeto.obtenerActorConIP(worker.getMessage());
                ContextoPruebas.setControlAcceso(actorConIP);
                countDownLatch.countDown();
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else {
            logger.error("Error - obteniendo datos del servidors");
        }
    }
    
    
}
