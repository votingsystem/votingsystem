package org.sistemavotacion.test.simulation;

import com.itextpdf.text.pdf.PdfReader;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import javax.mail.internet.MimeBodyPart;
import javax.swing.Timer;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.VotingSimulationData;
import org.sistemavotacion.test.modelo.UserBaseData;
import org.sistemavotacion.test.simulation.launcher.SignatureManifestLauncher;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.PDFSignerWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ManifestProcessSimulator implements 
        VotingSystemWorkerListener, ActionListener {
    
    private static Logger logger = LoggerFactory.getLogger(ManifestProcessSimulator.class);
    
    private static final int ACCESS_CONTROL_GETTER_WORKER          = 0;
    private static final int CA_CERT_INITIALIZER                   = 1;    
    private static final int SEND_DOCUMENT_JSON_WORKER             = 2;
    private static final int MANIFEST_GETTER_WORKER                = 3;
    private static final int PDF_SIGNER_WORKER                     = 4;
    private static final int PDF_PUBLISHER_WORKER                  = 5;

    private Timer timer;
    private static ExecutorService simulatorExecutor;
    private static ExecutorService signManifestExecutor;
    private static CompletionService<Respuesta> signManifestCompletionService;
    private static StringBuilder errorsLog;
    
    private List<String> signerList = null;
    private static AtomicLong signatures = new AtomicLong(0);
    private static AtomicLong signaturesFinished = new AtomicLong(0);
    private static AtomicLong signaturesERROR = new AtomicLong(0);
    private static AtomicLong signaturesOK = new AtomicLong(0);
    
    private Evento event = null;
    private UserBaseData userBaseData = null;
    private VotingSimulationData simulationData = null;

    private String urlSignManifest = null;
    private String urlPublishManifest = null;
    private byte[] pdfBytes = null;
    
    //private AtomicBoolean done = new AtomicBoolean(true);
    
    private static long begin;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    
    public ManifestProcessSimulator(VotingSimulationData simulationData) {
        try {
            ContextoPruebas.inicializar();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }      
        this.simulationData = simulationData;
        this.userBaseData = simulationData.getUserBaseData();
        errorsLog = new StringBuilder("");
    }
    
    private List<String> getSignerList(int numberOfRequests) {
        List<String> result = new ArrayList<String>();
        for(int i = 0; i < numberOfRequests; i++) {
            result.add(NifUtils.getNif(i));
        }
        return result;
    }
    
    public void init() {
        logger.debug("inits: " +  simulationData.getNumberOfRequests());
        begin = System.currentTimeMillis();
        try {
            setupAccesControl(simulationData.getAccessControlURL());
            countDownLatch.await();
            finish();
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public void beginSignatures () throws Exception {
        logger.debug(" ******* beginSignatures");
        if(userBaseData.isTimerBased()) {
            Long milisegundosHoras = 1000 * 60 * 60 * new Long(userBaseData.getHorasDuracionVotacion());
            Long milisegundosMinutos = 1000 * 60 * new Long(userBaseData.getMinutosDuracionVotacion()); 
            Long totalMilisegundosSimulacion = milisegundosHoras + milisegundosMinutos;
            Long intervaloLanzamiento = totalMilisegundosSimulacion/simulationData.getNumberOfRequests();
            timer = new Timer(intervaloLanzamiento.intValue(), this);
            timer.setRepeats(true);
            timer.start();
        } else {
            while(!signerList.isEmpty()) {
                if((signatures.get() - signaturesFinished.get()) < 
                        simulationData.getMaxPendingResponses()) {
                    int randomSigner = new Random().nextInt(signerList.size());
                    launchSignature(signerList.remove(randomSigner));
                } else Thread.sleep(1000);
                /*if(done.get()) {
                    int randomElector = new Random().nextInt(electorList.size());
                    lanzarSolicitudAcceso(electorList.remove(randomElector));
                    done.set(false);
                } else Thread.sleep(1000);*/
            }
        }
    }   
    
    private void launchSignature(String nif) throws Exception {
        String reason = null;
        String location = null;
        PdfReader manifestToSign = new PdfReader(pdfBytes);
        signManifestCompletionService.submit(new SignatureManifestLauncher(nif, 
                ContextoPruebas.getUrlTimeStampServer(), urlSignManifest, 
                manifestToSign, reason, location));
        signatures.getAndIncrement();        
    }
    
    private void validateReceipts() {
        logger.debug("******** validateReceipts");
        while (simulationData.getNumberOfRequests() > signaturesFinished.get()) {
            try {
                Future<Respuesta> f = signManifestCompletionService.take();
                signaturesFinished.getAndIncrement();
                Respuesta respuesta = f.get();   
                if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    logger.debug("Signature OK");
                    signaturesOK.getAndIncrement();
                } else {
                    signaturesERROR.getAndIncrement();
                    String mensaje = "Siganture ERROR - msg: " + respuesta.getMensaje();
                    logger.error(mensaje);
                    errorsLog.append(mensaje + "\n");
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                signaturesERROR.getAndIncrement();
            }
            //done.set(true);
        }
        finish();        
    }
    
    private void finish() {
        logger.debug("--------------- SIMULATION RESULT----------------------");   
        long duration = System.currentTimeMillis() - begin;
        String durationStr = DateUtils.
                getElapsedTimeHoursMinutesFromMilliseconds(duration);
        String errorsStr = errorsLog.toString();
        logger.info("Duration: " + durationStr);
        logger.info("Number of projected requests: " + 
                simulationData.getNumberOfRequests());
        logger.info("Number of completed requests: " + signaturesFinished.get());
        logger.info("Number of signatures OK: " + signaturesOK.get());
        logger.info("Number of signatures ERROR: " + signaturesERROR.get());
        if(!"".equals(errorsStr)) logger.info("Errors: " + errorsStr);
        logger.debug("------------------- FINISHED --------------------------");
        System.exit(0);
    }


    @Override
    public void process(List<String> messages) { }
    
    
    private void launchExecutors(){
        if(!(simulationData.getNumberOfRequests() > 0)) {
            logger.debug("Simulation without requests");
            countDownLatch.countDown();
            return;
        }
        signerList = getSignerList(simulationData.getNumberOfRequests());
        simulatorExecutor = Executors.newFixedThreadPool(10);
        signManifestExecutor = Executors.newFixedThreadPool(100);
        signManifestCompletionService = 
                new ExecutorCompletionService<Respuesta>(signManifestExecutor);
        signatures = new AtomicLong();
        signaturesERROR = new AtomicLong();
        signaturesFinished = new AtomicLong();
        signaturesOK = new AtomicLong();
        
        simulatorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    beginSignatures();                    
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
    
    private void setupAccesControl(String controlAccessServerURL){
        logger.debug("setupAccesControl");
        String urlServidor = StringUtils.prepararURL(controlAccessServerURL);
        String urlInfoServidor = ContextoPruebas.getURLInfoServidor(urlServidor);
        new InfoGetterWorker(ACCESS_CONTROL_GETTER_WORKER, urlInfoServidor, 
                null, this).execute();
    }
    
    private void publishEvent() {
        logger.debug("publishEvent");
        try {
            SignedMailGenerator signedMailGenerator = null;
            
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            String dateStr = formatter.format(date);
            
            Calendar today = Calendar.getInstance();
            Calendar dateInit = Calendar.getInstance();
            dateInit.add(Calendar.DATE, - 1);
            Calendar dateFinish  = Calendar.getInstance();
            dateFinish.add(Calendar.DATE,  30);
            
            event = new Evento();
            event.setAsunto("Asunto Manifiesto -> " + dateStr);
            
            event.setFechaInicio(today.getTime());
            event.setFechaFin(dateFinish.getTime());
            event.setContenido("Contenido Manifiesto -> " 
                    + simulationData.getHtmlContent());
            String eventStr = event.toJSON().toString();
            File manifestToPublish = File.createTempFile("manifestToPublish", ".json");
            manifestToPublish.deleteOnExit();
            FileUtils.copyStringToFile(eventStr, manifestToPublish);
            urlPublishManifest = ContextoPruebas.getManifestServiceURL(
                    ContextoPruebas.getControlAcceso().getServerURL());
            new DocumentSenderWorker(
                    SEND_DOCUMENT_JSON_WORKER, manifestToPublish, 
                    Contexto.JSON_CONTENT_TYPE, urlPublishManifest, this).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void initilizeAuthorityCert() {
        try {
            byte[] caPemCertificateBytes = CertUtil.fromX509CertToPEM (
                ContextoPruebas.getCertificadoRaizAutoridad());
            String urlAnyadirCertificadoCA = ContextoPruebas.getURLAnyadirCertificadoCA(
                ContextoPruebas.getControlAcceso().getServerURL());
            new DocumentSenderWorker(CA_CERT_INITIALIZER, 
                caPemCertificateBytes, null, urlAnyadirCertificadoCA, this).execute();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    /*
     * Method invoked when pdf document is timestamped
     */
    private void processPDF(File document) {
        logger.error("processPDF");
        try {
            File documentToSend = File.createTempFile("pdfEncryptedFile", ".eml");
            document.deleteOnExit();

            MimeBodyPart mimeBodyPart = Encryptor.encryptFile(document, 
                    ContextoPruebas.getControlAcceso().getCertificate());
            mimeBodyPart.writeTo(new FileOutputStream(documentToSend));
            String contentType = Contexto.PDF_SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
            String urlToSendSignedManifestToPublish = urlPublishManifest + 
                    File.separator + event.getEventoId();
            new DocumentSenderWorker(PDF_PUBLISHER_WORKER, documentToSend, 
                    contentType, urlToSendSignedManifestToPublish, this).execute();
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            countDownLatch.countDown();
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(timer)) {
            if(!signerList.isEmpty()) {
                try {
                    int randomSigner = new Random().nextInt(signerList.size());
                    launchSignature(signerList.remove(randomSigner));
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } else timer.stop();
        }
    }
    
    
    @Override
    public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
                " - worker: " + worker.getClass().getSimpleName() + 
                " - workerId:" + worker.getId());
        switch(worker.getId()) {
            case ACCESS_CONTROL_GETTER_WORKER:           
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP controlAcceso = ActorConIP.parse(worker.getMessage());
                        if(!(ActorConIP.Tipo.CONTROL_ACCESO == controlAcceso.getTipo())) {
                            logger.error("SERVER NOT ACCESS CONTROL");
                            return;
                        }
                        ContextoPruebas.setControlAcceso(controlAcceso);
                        initilizeAuthorityCert();
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        countDownLatch.countDown();
                    }
                }else {
                    String mensaje = "Error - " + worker.getMessage();
                    logger.error("### ERROR - " + mensaje);
                    countDownLatch.countDown();
                }
                break;
            case CA_CERT_INITIALIZER:
                publishEvent();
                break;         
            case SEND_DOCUMENT_JSON_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) { 
                    event.setEventoId(Long.valueOf(worker.getMessage()));
                    urlSignManifest = ContextoPruebas.getSignManifestURL(
                        ContextoPruebas.getControlAcceso().getServerURL()) + 
                        File.separator + worker.getMessage();
                    //manifest PDF has been validated, now we have to download it
                    String pdfURL = ContextoPruebas.getManifestServiceURL(
                            ContextoPruebas.getControlAcceso().getServerURL()) + 
                            File.separator + worker.getMessage();
                    new InfoGetterWorker(MANIFEST_GETTER_WORKER,
                            pdfURL, Contexto.PDF_CONTENT_TYPE, this).execute();
                } else {
                    logger.error("ERROR - " + worker.getMessage());
                    countDownLatch.countDown();
                }
                break;
            case MANIFEST_GETTER_WORKER:
                //This is the pdf to publish, but first we have to sign and timestamp it
                if(Respuesta.SC_OK == worker.getStatusCode()) { 
                    try {
                        //if all is OK server responds with the id of the new manifest
                        pdfBytes =((InfoGetterWorker)worker).getRespuesta().
                            getBytesArchivo();
                        PdfReader manifestToSign = new PdfReader(pdfBytes);
                        String reason = null;
                        String location = null;
                        KeyStore keyStore = ContextoPruebas.getUsuarioPruebas().getKeyStore();
                        PrivateKey privateKey = (PrivateKey)keyStore.getKey(
                                ContextoPruebas.END_ENTITY_ALIAS,
                                ContextoPruebas.PASSWORD.toCharArray());
                        Certificate[] signerCertChain = keyStore.getCertificateChain(
                                ContextoPruebas.END_ENTITY_ALIAS);
                        new PDFSignerWorker(PDF_SIGNER_WORKER, 
                                ContextoPruebas.getUrlTimeStampServer(),
                                this, reason, location, null, manifestToSign, 
                                privateKey, signerCertChain).execute();
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        countDownLatch.countDown();
                    }
                } else {
                    logger.error("ERROR - " + worker.getMessage());
                    countDownLatch.countDown();
                }
                break;  
            case PDF_SIGNER_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) { 
                    processPDF(((PDFSignerWorker)worker).getSignedAndTimeStampedPDF());
                } else {
                    logger.error("ERROR - " + worker.getMessage());
                    countDownLatch.countDown();
                }
                break;
            case PDF_PUBLISHER_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) { 
                    launchExecutors();//Manifest published, we now initialize user base data.
                } else {
                    logger.error("ERROR - " + worker.getMessage());
                    countDownLatch.countDown();
                }
                break;
        }
    }
    
    
    
    public static void main(String[] args) throws Exception {
        VotingSimulationData simulationData = null;
        if(args != null && args.length > 0) {
            logger.debug("args[0]");
            simulationData = VotingSimulationData.parse(args[0]);
        } else {
            File jsonFile = File.createTempFile("ManifestProcessSimulation", ".json");
            jsonFile.deleteOnExit();
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("simulatorFiles/manifestSimulationData.json"), jsonFile); 
            simulationData = VotingSimulationData.parse(FileUtils.getStringFromFile(jsonFile));
        }
        ManifestProcessSimulator simuHelper = new ManifestProcessSimulator(simulationData);
        simuHelper.init();
    }
}
