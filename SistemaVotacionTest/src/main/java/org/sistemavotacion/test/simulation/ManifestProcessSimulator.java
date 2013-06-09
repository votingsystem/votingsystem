package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.modelo.SimulationData;
import com.itextpdf.text.pdf.PdfReader;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
import java.util.concurrent.FutureTask;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.pdf.PdfFormHelper;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.simulation.callable.BackupValidator;
import org.sistemavotacion.test.simulation.callable.ManifestSigner;
import org.sistemavotacion.test.util.SimulationUtils;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.PDFSignedSenderWorker;
import org.sistemavotacion.worker.SMIMESignedSenderWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ManifestProcessSimulator extends Simulator<SimulationData> 
        implements ActionListener {
    
    private static Logger logger = LoggerFactory.getLogger(
            ManifestProcessSimulator.class);

    private static ExecutorService simulatorExecutor;
    private static ExecutorService signManifestExecutor;
    private static CompletionService<Respuesta> signManifestCompletionService;
    
    private List<String> signerList = null;
    
    private Evento event = null;
    private Evento.Estado nextEventState = null;

    private String urlSignManifest = null;
    private String urlPublishManifest = null;
    private byte[] pdfBytes = null;
    
    private SimulatorListener simulationListener;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    public ManifestProcessSimulator(SimulationData simulationData,
             SimulatorListener simulationListener) {  
        super(simulationData);
        this.simulationListener = simulationListener;
    }

    public ManifestProcessSimulator(SimulationData simulationData) {  
        super(simulationData);
        this.simulationData = simulationData;
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
                } else Thread.sleep(200);
            }
        }
    }   
    
    private void launchSignature(String nif) throws Exception {
        String reason = null;
        String location = null;
        PdfReader manifestToSign = new PdfReader(pdfBytes);
        signManifestCompletionService.submit(new ManifestSigner(nif, 
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
        }
        if(nextEventState != null) setNextEventState();
        if(nextEventState != null && simulationData.
                getBackupRequestEmail() != null) requestBackup();
        countDownLatch.countDown();    
    }
    
    private void setNextEventState() throws Exception {
        logger.debug("setNextEventState");
        String cancelDataStr = event.getCancelEventJSON(
            Contexto.INSTANCE.getAccessControl().getServerURL(), 
            nextEventState).toString();
        String msgSubject = ContextoPruebas.INSTANCE.getString("cancelClaimMsgSubject");
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                ContextoPruebas.INSTANCE.getUserTest().getKeyStore(),
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.VOTE_SIGN_MECHANISM);
        SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(
                ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                Contexto.INSTANCE.getAccessControl().getNombreNormalizado(), 
                cancelDataStr, msgSubject,  null);
        SMIMESignedSenderWorker worker = new SMIMESignedSenderWorker(
                null, smimeDocument, ContextoPruebas.INSTANCE.getCancelEventURL(), 
                null, null, null);
        worker.execute();
        worker.get();//wait
        if(Respuesta.SC_OK != worker.getStatusCode()) 
            logger.error(worker.getErrorMessage());
    }

    private void requestBackup() throws Exception {
        logger.debug("requestBackup");
        byte[] requestBackupPDFBytes = PdfFormHelper.getBackupRequest(
            event.getEventoId().toString(), event.getAsunto(), 
                            simulationData.getBackupRequestEmail());
        PrivateKey signerPrivateKey = ContextoPruebas.INSTANCE.getUserTestPrivateKey();
        Certificate[] signerCertChain = ContextoPruebas.INSTANCE.getUserTestCertificateChain();
        PdfReader requestBackupPDF = new PdfReader(requestBackupPDFBytes);
        PDFSignedSenderWorker worker = new PDFSignedSenderWorker(null, 
                ContextoPruebas.INSTANCE.getUrlBackupEvents(), 
                null, null, null, requestBackupPDF, signerPrivateKey, 
                signerCertChain, null, null);
        worker.execute();
        Respuesta respuesta = worker.get();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            FutureTask<Respuesta> future = new FutureTask<Respuesta>(
                new BackupValidator(respuesta.getBytesArchivo()));
            simulatorExecutor.execute(future);
            respuesta = future.get();
            logger.debug("BackupRequestWorker - status: " + respuesta.getCodigoEstado());
        } else logger.error(worker.getErrorMessage());
    }
    
    private void initExecutors(){
        if(!(simulationData.getNumRequestsProjected() > 0)) {
            logger.debug("WITHOUT NumberOfRequestsProjected");
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
    
    private void publishManifest() throws Exception {
        logger.debug("publishManifest");
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String dateStr = formatter.format(date);
        event = simulationData.getEvento();
        nextEventState = event.getNextState();
        String subject = event.getAsunto()+ " -> " + dateStr;
        event.setAsunto(subject);
        String eventStr = event.toJSON().toString();
        urlPublishManifest = ContextoPruebas.getManifestServiceURL(
                Contexto.INSTANCE.getAccessControl().getServerURL());
        DocumentSenderWorker worker = new DocumentSenderWorker(null, 
                eventStr.getBytes(), Contexto.JSON_CONTENT_TYPE, 
                urlPublishManifest, null);
        worker.execute();
        worker.get();
        if(Respuesta.SC_OK == worker.getStatusCode()) { 
            event.setEventoId(Long.valueOf(worker.getMessage()));
            urlSignManifest = ContextoPruebas.getSignManifestURL(
                Contexto.INSTANCE.getAccessControl().getServerURL()) + 
                File.separator + worker.getMessage();
            //manifest PDF has been validated, now we have to download it
            String pdfURL = ContextoPruebas.getManifestServiceURL(
                    Contexto.INSTANCE.getAccessControl().getServerURL()) + 
                    File.separator + worker.getMessage();
            InfoGetterWorker pdfGetterWorker = new InfoGetterWorker(null,
                    pdfURL, Contexto.PDF_CONTENT_TYPE, null);
            pdfGetterWorker.execute();
            Respuesta respuesta = pdfGetterWorker.get();
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) { 
                //if all is OK server responds with the id of the new manifest
                pdfBytes =respuesta.getBytesArchivo();
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
                PDFSignedSenderWorker pdfSenderWorker = 
                        new PDFSignedSenderWorker(null,
                        urlToSendDocument, reason, location, null,
                        manifestToSign, privateKey, signerCertChain, 
                        destinationCert, null);
                pdfSenderWorker.execute();
                pdfSenderWorker.get();
                if(Respuesta.SC_OK == pdfSenderWorker.getStatusCode()) {
                    initExecutors();//Manifest published, we now initialize user base data.
                } else logger.error(pdfSenderWorker.getMessage());
            } else logger.error(pdfGetterWorker.getMessage());
        } logger.error(worker.getMessage());
    }

    private void initCA_AccessControl() throws Exception {
        logger.debug("initCA_AccessControl");
        //we need to add test Authority Cert to system in order
        //to validate signatures
        byte[] rootCACertPEMBytes = CertUtil.fromX509CertToPEM (
            ContextoPruebas.INSTANCE.getRootCACert());
        String rootCAServiceURL = ContextoPruebas.INSTANCE.
                getAccessControlRootCAServiceURL();
        DocumentSenderWorker worker = new DocumentSenderWorker(null, 
            rootCACertPEMBytes, null, rootCAServiceURL, null);
        worker.execute();
        worker.get();
        if(Respuesta.SC_OK == worker.getStatusCode()) {
            publishManifest();
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
        simuHelper.call();
    }


    @Override
    public SimulationData call() throws Exception {
        logger.debug("call - NumberOfRequestsProjected: " +  
                simulationData.getNumRequestsProjected());
        simulationData.setBegin(System.currentTimeMillis());
        String urlServidor = StringUtils.prepararURL(simulationData.getAccessControlURL());
        String urlInfoServidor = ContextoPruebas.getURLInfoServidor(urlServidor);
        InfoGetterWorker worker = new InfoGetterWorker(null, urlInfoServidor, 
            null, null);
        worker.execute();            
        worker.get();
        if(Respuesta.SC_OK == worker.getStatusCode()) {
            ActorConIP controlAcceso = ActorConIP.parse(worker.getMessage());
            String msg = SimulationUtils.checkActor(controlAcceso, 
                    ActorConIP.Tipo.CONTROL_ACCESO);
            if(msg == null) {
                ContextoPruebas.INSTANCE.setControlAcceso(controlAcceso);
                initCA_AccessControl();
            }
        } else logger.error(worker.getErrorMessage());
        
        
        countDownLatch.await();
        logger.debug("- call - shutdown executors");   
        
        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        if(simulatorExecutor != null) simulatorExecutor.shutdownNow();
        if(signManifestExecutor != null) signManifestExecutor.shutdownNow(); 
        
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
            logger.info(" ************* " + geterrorList().size() + " ERRORS: \n" + 
                        errorsMsg);
        }
        logger.debug("------------------- FINISHED --------------------------");
        
        if(simulationListener != null)            
            simulationListener.setSimulationResult(simulationData);
        
        
        simulationData.setStatusCode(Respuesta.SC_OK);
        return simulationData;
    }

}
