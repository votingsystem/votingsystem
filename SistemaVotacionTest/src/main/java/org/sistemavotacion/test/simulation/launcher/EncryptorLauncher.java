package org.sistemavotacion.test.simulation.launcher;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.simulation.EncryptionSimulator;
import org.sistemavotacion.test.worker.EncryptorWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class EncryptorLauncher implements Callable<Respuesta>, 
        VotingSystemWorkerListener {

    private static Logger logger = LoggerFactory.getLogger(EncryptionSimulator.class);

    private static final int ENCRYPTOR_WORKER = 1;


    private String requestNIF;
    private Respuesta respuesta;

    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    private String serverURL = null;


    public EncryptorLauncher (String requestNIF, String serverURL) 
            throws Exception {
        this.requestNIF = requestNIF;
        this.serverURL = serverURL;
    }

    @Override
    public Respuesta call() throws Exception {
        new EncryptorWorker(ENCRYPTOR_WORKER, requestNIF, serverURL, this,
                ContextoPruebas.INSTANCE.getControlAcceso().getCertificate()).execute();
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
