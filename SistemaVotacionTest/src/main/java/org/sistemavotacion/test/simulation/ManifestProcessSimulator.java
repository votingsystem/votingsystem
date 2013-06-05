package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.modelo.SimulationData;
import com.itextpdf.text.pdf.PdfReader;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.MetaInf;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.pdf.PdfFormHelper;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.simulation.launcher.SignatureManifestLauncher;
import org.sistemavotacion.test.util.SimulationUtils;
import org.sistemavotacion.test.worker.BackupRequestWorker;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.PDFSignedSenderWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ManifestProcessSimulator extends Simulator<SimulationData> 
        implements VotingSystemWorkerListener, ActionListener {
    
    private static Logger logger = LoggerFactory.getLogger(ManifestProcessSimulator.class);
    
    private static final int ACCESS_CONTROL_GETTER_WORKER          = 0;
    private static final int CA_CERT_INITIALIZER                   = 1;    
    private static final int SEND_DOCUMENT_JSON_WORKER             = 2;
    private static final int MANIFEST_GETTER_WORKER                = 3;
    private static final int PDF_SIGNED_SENDER_WORKER              = 4;
    private static final int BACKUP_REQUEST_WORKER                 = 5;

    private static ExecutorService simulatorExecutor;
    private static ExecutorService signManifestExecutor;
    private static CompletionService<Respuesta> signManifestCompletionService;
    
    private List<String> signerList = null;
    
    private Evento event = null;
    private SimulationData simulationData = null;

    private String urlSignManifest = null;
    private String urlPublishManifest = null;
    private byte[] pdfBytes = null;
    
    //private AtomicBoolean done = new AtomicBoolean(true);
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    private SimulatorListener simulationListener;
    
    public ManifestProcessSimulator(SimulationData simulationData,
             SimulatorListener simulationListener) {  
        super(simulationData);
        this.simulationData = simulationData;
        this.simulationListener = simulationListener;
    }
    

    public void init() {
        logger.debug("inits - NumberOfRequestsProjected: " +  
                simulationData.getNumRequestsProjected());
        simulationData.setBegin(System.currentTimeMillis());
        try {
            String urlServidor = StringUtils.prepararURL(simulationData.getAccessControlURL());
            String urlInfoServidor = ContextoPruebas.getURLInfoServidor(urlServidor);
            new InfoGetterWorker(ACCESS_CONTROL_GETTER_WORKER, urlInfoServidor, 
                null, this).execute();            
            countDownLatch.await();
            finish();
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public void launchRequests () throws Exception {
        logger.debug(" ----------- launchRequests - NumRequestsProjected: " + 
                simulationData.getNumRequestsProjected());
        if(simulationData.isTimerBased()) startTimer(this);
        else {
            while(!signerList.isEmpty()) {
                if((simulationData.getNumRequests() - 
                        simulationData.getNumRequestsColected()) < 
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
            urlSignManifest, manifestToSign, reason, location));
        simulationData.getAndIncrementNumRequests();
    }
    
    private void readResponses() throws Exception {
        logger.debug(" -------------- readResponses - NumRequestsProjected: " + 
                simulationData.getNumRequestsProjected());
        while (simulationData.getNumRequestsProjected() > 
                simulationData.getNumRequestsColected()) {
            try {
                Future<Respuesta> f = signManifestCompletionService.take();
                Respuesta respuesta = f.get();   
                if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    simulationData.getAndIncrementNumRequestsOK();
                } else {
                    simulationData.getAndIncrementNumRequestsERROR();
                    String msg = "Signature ERROR - msg: " + respuesta.getMensaje();
                    //logger.error(msg);
                    addErrorMsg(msg);
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                String msg = "Signature ERROR - msg: " + ex.getMessage();
                addErrorMsg(msg);
                simulationData.getAndIncrementNumRequestsERROR();
            }
            //done.set(true);
        }
        if(simulationData.getBackupRequestEmail() != null) requestBackup();
        else countDownLatch.countDown();       
    }
    
    private void requestBackup() throws Exception {
        logger.debug("requestBackup");
        byte[] requestBackupPDFBytes = PdfFormHelper.getBackupRequest(
            event.getEventoId().toString(), event.getAsunto(), 
                            simulationData.getBackupRequestEmail());
        new BackupRequestWorker(BACKUP_REQUEST_WORKER, 
                ContextoPruebas.INSTANCE.getUrlBackupEvents(), 
                requestBackupPDFBytes, this).execute(); 
    }
    
    
    @Override public void processVotingSystemWorkerMsg(List<String> messages) {}
    
    
    private void initExecutors(){
        if(!(simulationData.getNumRequestsProjected() > 0)) {
            logger.debug("WITHOUT NumberOfRequestsProjected");
            countDownLatch.countDown();
            return;
        }
        signerList = new ArrayList<String>();
        for(int i = 0; i < simulationData.getNumRequestsProjected(); i++) {
            signerList.add(NifUtils.getNif(i));
        }
        
        simulatorExecutor = Executors.newFixedThreadPool(5);
        signManifestExecutor = Executors.newFixedThreadPool(100);
        signManifestCompletionService = 
                new ExecutorCompletionService<Respuesta>(signManifestExecutor);
        
        simulatorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    launchRequests();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        simulatorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    readResponses();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }
    
    private void publishEvent() {
        logger.debug("publishEvent");
        try {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            String dateStr = formatter.format(date);
            event = simulationData.getEvento();
            String subject = event.getAsunto()+ " -> " + dateStr;
            event.setAsunto(subject);
            String eventStr = event.toJSON().toString();
            urlPublishManifest = ContextoPruebas.getManifestServiceURL(
                    Contexto.INSTANCE.getAccessControl().getServerURL());
            new DocumentSenderWorker(SEND_DOCUMENT_JSON_WORKER, eventStr.getBytes(), 
                    Contexto.JSON_CONTENT_TYPE, urlPublishManifest, this).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void initilizeAuthorityCert() {
        try {
            byte[] caPemCertificateBytes = CertUtil.fromX509CertToPEM (
                ContextoPruebas.INSTANCE.getRootCACert());
            String urlAnyadirCertificadoCA = ContextoPruebas.INSTANCE.
                    getRootCAServiceURL();
            new DocumentSenderWorker(CA_CERT_INITIALIZER, 
                caPemCertificateBytes, null, urlAnyadirCertificadoCA, this).execute();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
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
    
    @Override public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
                " - worker: " + worker.getClass().getSimpleName() + 
                " - workerId:" + worker.getId());
        String msg = null;
        switch(worker.getId()) {
            case ACCESS_CONTROL_GETTER_WORKER:  
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP controlAcceso = ActorConIP.parse(worker.getMessage());
                        msg = SimulationUtils.checkActor(controlAcceso, 
                                ActorConIP.Tipo.CONTROL_ACCESO);
                        if(msg == null) {
                            ContextoPruebas.INSTANCE.setControlAcceso(controlAcceso);
                            initilizeAuthorityCert();
                            return;
                        }
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                }else msg = worker.getMessage();
                msg = "### ERROR - ACCESS_CONTROL_GETTER_WORKER -" + msg;
                break;
            case CA_CERT_INITIALIZER:
                publishEvent();
                return;        
            case SEND_DOCUMENT_JSON_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) { 
                    event.setEventoId(Long.valueOf(worker.getMessage()));
                    urlSignManifest = ContextoPruebas.getSignManifestURL(
                        Contexto.INSTANCE.getAccessControl().getServerURL()) + 
                        File.separator + worker.getMessage();
                    //manifest PDF has been validated, now we have to download it
                    String pdfURL = ContextoPruebas.getManifestServiceURL(
                            Contexto.INSTANCE.getAccessControl().getServerURL()) + 
                            File.separator + worker.getMessage();
                    new InfoGetterWorker(MANIFEST_GETTER_WORKER,
                            pdfURL, Contexto.PDF_CONTENT_TYPE, this).execute();
                    return;
                } else {
                    msg = "###ERROR SEND_DOCUMENT_JSON_WORKER: " + worker.getMessage();
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
                        PrivateKey privateKey = ContextoPruebas.INSTANCE.
                                getUserTestPrivateKey();
                        Certificate[] signerCertChain = ContextoPruebas.INSTANCE.
                                getUserTestCertificateChain();
                        String urlToSendDocument = urlPublishManifest + 
                            File.separator + event.getEventoId();
                        X509Certificate destinationCert = Contexto.INSTANCE.
                                getAccessControl().getCertificate();
                        
                        new PDFSignedSenderWorker(PDF_SIGNED_SENDER_WORKER,
                                urlToSendDocument, reason, location, null,
                                manifestToSign, privateKey, signerCertChain, 
                                destinationCert, this).execute();
                        return;
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                } else msg = worker.getMessage();
                msg = "### ERROR - MANIFEST_GETTER_WORKER -" + msg;
                break;  
            case PDF_SIGNED_SENDER_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) { 
                    initExecutors();//Manifest published, we now initialize user base data.
                    return;
                } else msg = "###ERROR PDF_PUBLISHER_WORKER: " + worker.getMessage();
                break;
            case BACKUP_REQUEST_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    MetaInf metaInf =  (MetaInf) worker.getRespuesta().getData();
                    logger.debug("BACKUP_REQUEST_WORKER OK - Event -> " + event.getEventoId());
                    if(metaInf.getErrorsList() != null && 
                            !metaInf.getErrorsList().isEmpty()) {
                        addErrorList(metaInf.getErrorsList());
                    }
                    logger.debug("------------ META-INF BACKUP ------------");
                    logger.debug(metaInf.getFormattedInfo());
                    countDownLatch.countDown();
                    return;
                } else msg = "### ERRROR BACKUP_REQUEST_WORKER: " + 
                        worker.getMessage();
                break;
        }
        addErrorMsg(msg);
        logger.error(msg);
        countDownLatch.countDown();
    }

    public static void main(String[] args) throws Exception {
        SimulationData simulationData = null;
        if(args != null && args.length > 0) {
            logger.debug("args[0]");
            simulationData = SimulationData.parse(args[0]);
        } else {
            File jsonFile = File.createTempFile("ManifestProcessSimulation", ".json");
            jsonFile.deleteOnExit();
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("simulatorFiles/manifestSimulationData.json"), jsonFile); 
            simulationData = SimulationData.parse(FileUtils.getStringFromFile(jsonFile));
        }
        ManifestProcessSimulator simuHelper = new ManifestProcessSimulator(
                simulationData, null);
        simuHelper.init();
    }

    @Override public SimulationData getData() {
        return simulationData;
    }

    @Override  public SimulationData finish() throws Exception {
        logger.debug("finish");
        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        if(simulatorExecutor != null) simulatorExecutor.shutdownNow();
        if(signManifestExecutor != null) signManifestExecutor.shutdownNow();         
        if(simulationListener != null) {           
            simulationListener.setSimulationResult(this);
        } else { 
            logger.debug("--------------- SIMULATION RESULT----------------------");   
            simulationData.setFinish(System.currentTimeMillis());
            logger.info("Duration: " + simulationData.getDurationStr());
            logger.info("Number of projected requests: " + 
                    simulationData.getNumRequestsProjected());
            logger.info("Number of completed requests: " + simulationData.getNumRequests());
            logger.info("Number of signatures OK: " + simulationData.getNumRequestsOK());
            logger.info("Number of signatures ERROR: " + simulationData.getNumRequestsERROR());
            String errorsMsg = getFormattedErrorList();
            if(errorsMsg != null) {
                logger.info(" ************* " + getErrorsList().size() + " ERRORS: \n" + 
                            errorsMsg);
            }
            logger.debug("------------------- FINISHED --------------------------");
            System.exit(0);
        }
        return simulationData;
    }

}
