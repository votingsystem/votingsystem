package org.sistemavotacion.test.simulacion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.json.DeJSONAObjeto;
import org.sistemavotacion.test.util.NifUtils;
import org.sistemavotacion.test.worker.EncryptorWorker;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class EncryptionSimulation implements VotingSystemWorkerListener {
        
    private static Logger logger = LoggerFactory.getLogger(EncryptionSimulation.class);
    
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
    private String requestURL = null;

    public EncryptionSimulation(Long numSolicitudes, 
            String urlInfoServidor, String requestURL) {
        logger.debug("numeroSolicitudes '" + numSolicitudes + "'");
        this.requestURL = requestURL;
        new InfoGetterWorker(null, urlInfoServidor, this).execute();
        solicitudesEnviadas = new AtomicLong(0);
        solicitudesRecogidas = new AtomicLong(0);
        numeroSolicitudes =  new AtomicLong(numSolicitudes);
        requestExecutor = Executors.newFixedThreadPool(1000);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(requestExecutor);
    }
    
    public void init() throws InterruptedException {
        logger.debug("init - await to get server info"); 
        countDownLatch.await(); // 
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
        logger.debug("**************** lanzarSolicitudes");
        if(numeroSolicitudes.get() == 0) {
            logger.debug("lanzarSolicitudes - SIN PETICIONES PENDIENTES");
            return;
        } 
        do {
            if((solicitudesEnviadas.get() - solicitudesRecogidas.get()) < 
                    MAX_PENDING_RESPONSES) {
                requestCompletionService.submit(new EncryptorSender(
                        NifUtils.getNif(new Long(solicitudesEnviadas.
                        getAndIncrement()).intValue()), requestURL));
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
            logger.debug("Respuesta '" + v + "' statusCode: " + 
                    respuesta.getCodigoEstado());
            if(respuesta.getCodigoEstado() == Respuesta.SC_OK) {
                solicitudesOK++;
            } else {
                logger.debug(" - ERROR msg: " + respuesta.getMensaje());
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
    
    public class EncryptorSender implements Callable<Respuesta>, 
        VotingSystemWorkerListener {
    
        private static final int ENCRYPTOR_WORKER = 1;


        private String requestNIF;
        private Respuesta respuesta;

        private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
        private String serverURL = null;


        public EncryptorSender (String requestNIF, String serverURL) 
                throws Exception {
            this.requestNIF = requestNIF;
            this.serverURL = serverURL;
        }

        @Override
        public Respuesta call() throws Exception {
            new EncryptorWorker(ENCRYPTOR_WORKER, requestNIF, serverURL, this,
                    ContextoPruebas.getControlAcceso().getCertificate()).execute();
            countDownLatch.await();
            return getResult();
        }

        @Override
        public void process(List<String> messages) {
            for(String message : messages)  {
                logger.debug("process -> " + message);
            }
        }

        @Override
        public void showResult(VotingSystemWorker worker) {
            logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
             " - worker: " + worker.getClass().getSimpleName() + 
             " - workerId:" + worker.getId());
            respuesta = new Respuesta();
            respuesta.setCodigoEstado(worker.getStatusCode());
            respuesta.setMensaje(worker.getMessage());
            countDownLatch.countDown();
        }

        private Respuesta getResult() {
            return respuesta;
        }
    }

    
        public static void main(String[] args) throws InterruptedException {
        try {
            ContextoPruebas.inicializar();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        String urlServidor = "http://localhost:8080/SistemaVotacionControlAcceso/infoServidor";
        String requestURL = "http://localhost:8080/SistemaVotacionControlAcceso/encryptor";

        EncryptionSimulation encryptionSimulation = new EncryptionSimulation(
                new Long(50), urlServidor, requestURL);
        encryptionSimulation.init();
        
    }
}
