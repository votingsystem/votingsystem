package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.modelo.SimulationData;
import com.itextpdf.text.pdf.PdfReader;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.management.ManagementFactory;
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
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.simulation.callable.ServerInitializer;
import org.sistemavotacion.test.simulation.callable.ManifestSigner;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.callable.InfoSender;
import org.sistemavotacion.callable.InfoGetter;
import org.sistemavotacion.callable.PDFSignedSender;
import org.sistemavotacion.callable.SMIMESignedSender;
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
        SMIMESignedSender worker = new SMIMESignedSender(
                null, smimeDocument, ContextoPruebas.INSTANCE.getCancelEventURL(), 
                null, null);
        Respuesta respuesta = worker.call();
        if(Respuesta.SC_OK != respuesta.getCodigoEstado()) 
            logger.error(respuesta.getMensaje());
    }

    private void requestBackup() throws Exception {
        logger.debug("requestBackup");
        byte[] requestBackupPDFBytes = PdfFormHelper.getBackupRequest(
            event.getEventoId().toString(), event.getAsunto(), 
                            simulationData.getBackupRequestEmail());
        PrivateKey signerPrivateKey = ContextoPruebas.INSTANCE.getUserTestPrivateKey();
        Certificate[] signerCertChain = ContextoPruebas.INSTANCE.getUserTestCertificateChain();
        PdfReader requestBackupPDF = new PdfReader(requestBackupPDFBytes);
        PDFSignedSender worker = new PDFSignedSender(null, 
                ContextoPruebas.INSTANCE.getUrlBackupEvents(), 
                null, null, null, requestBackupPDF, signerPrivateKey, 
                signerCertChain, null);
        Respuesta respuesta = worker.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            String downloadServiceURL = ContextoPruebas.INSTANCE.
                    getUrlDownloadBackup(respuesta.getMensaje());
            InfoGetter infoGetter = new InfoGetter(null, downloadServiceURL, null);
            respuesta = infoGetter.call();
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) { 
                logger.debug("TODO validate backup");
                /*FutureTask<Respuesta> future = new FutureTask<Respuesta>(
                    new ZipBackupValidator(respuesta.getMessageBytes()));
                simulatorExecutor.execute(future);
                respuesta = future.get();
                logger.debug("BackupRequestWorker - status: " + respuesta.getCodigoEstado());*/
            } else logger.error(respuesta.getMensaje());
        } else logger.error(respuesta.getMensaje());
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
        
        simulatorExecutor = Executors.newFixedThreadPool(100);
        signManifestCompletionService = 
                new ExecutorCompletionService<Respuesta>(simulatorExecutor);
        
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
        InfoSender infoSender = new InfoSender(null, 
                eventStr.getBytes(), Contexto.JSON_CONTENT_TYPE, 
                urlPublishManifest);
        Respuesta respuesta = infoSender.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) { 
            event.setEventoId(Long.valueOf(respuesta.getMensaje()));
            urlSignManifest = ContextoPruebas.getSignManifestURL(
                Contexto.INSTANCE.getAccessControl().getServerURL()) + 
                File.separator + respuesta.getMensaje();
            //manifest PDF has been validated, now we have to download it
            String pdfURL = ContextoPruebas.getManifestServiceURL(
                    Contexto.INSTANCE.getAccessControl().getServerURL()) + 
                    File.separator + respuesta.getMensaje();
            InfoGetter pdfGetterWorker = new InfoGetter(null,
                    pdfURL, Contexto.PDF_CONTENT_TYPE);
            respuesta = pdfGetterWorker.call();
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) { 
                //if all is OK server responds with the id of the new manifest
                pdfBytes =respuesta.getMessageBytes();
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
                PDFSignedSender pdfSenderWorker = 
                        new PDFSignedSender(null,
                        urlToSendDocument, reason, location, null,
                        manifestToSign, privateKey, signerCertChain, 
                        destinationCert);
                respuesta = pdfSenderWorker.call();
                if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    initExecutors();//Manifest published, we now initialize user base data.
                } else logger.error(respuesta.getMensaje());
            } else logger.error(respuesta.getMensaje());
        } logger.error(respuesta.getMensaje());
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
        System.exit(0);
    }


    @Override public Respuesta call() throws Exception {
        logger.debug("call - NumberOfRequestsProjected: " +  
                simulationData.getNumRequestsProjected() + " - process:" + 
                ManagementFactory.getRuntimeMXBean().getName());
        simulationData.setBegin(System.currentTimeMillis());
        ServerInitializer accessControlInitializer = 
                new ServerInitializer(simulationData.getAccessControlURL(),
                ActorConIP.Tipo.CONTROL_ACCESO);
        Respuesta respuesta = accessControlInitializer.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
             publishManifest();
        } else logger.error(respuesta.getMensaje());
        
        countDownLatch.await();
        logger.debug("- call - shutdown executors");   
        
        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        if(simulatorExecutor != null) simulatorExecutor.shutdownNow();
        
        logger.debug("------- SIMULATION RESULT - Event: " + event.getId());  
        simulationData.setFinish(System.currentTimeMillis());
        simulationData.setFinish(System.currentTimeMillis());
                logger.info("Begin: " + DateUtils.getStringFromDate(
                simulationData.getBeginDate())  + " - Duration: " + 
                simulationData.getDurationStr());
        logger.info("Number of projected requests: " + 
                simulationData.getNumRequestsProjected());
        logger.info("Number of completed requests: " + simulationData.getNumRequests());
        logger.info("Number of signatures OK: " + simulationData.getNumRequestsOK());
        logger.info("Number of signatures ERROR: " + simulationData.getNumRequestsERROR());
        String errorsMsg = getFormattedErrorList();
        if(errorsMsg != null) {
            logger.info(" ************* " + getErrorList().size() + " ERRORS: \n" + 
                        errorsMsg);
        }
        respuesta = new Respuesta(Respuesta.SC_FINALIZADO,simulationData);
        if(simulationListener != null)            
            simulationListener.processResponse(respuesta);
        return respuesta;
    }

}
