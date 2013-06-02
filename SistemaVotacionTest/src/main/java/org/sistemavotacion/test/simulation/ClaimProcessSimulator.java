package org.sistemavotacion.test.simulation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
import javax.swing.Timer;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.VotingSimulationData;
import org.sistemavotacion.test.modelo.UserBaseData;
import org.sistemavotacion.test.simulation.launcher.SignatureClaimLauncher;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.TimeStampWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 1)- Check Access Control and initialice CA cert
* 2)- Check if there's a Control Center associated and associated if not.
* 3)- Publish Event
* 4)- Initialice user base data
*/
public class ClaimProcessSimulator implements 
        VotingSystemWorkerListener, ActionListener {
    
    private static Logger logger = LoggerFactory.getLogger(ClaimProcessSimulator.class);
    
    private static final int ACCESS_CONTROL_GETTER_WORKER = 0;
    private static final int CA_CERT_INITIALIZER          = 1;    
    private static final int TIME_STAMP_WORKER            = 3;
    private static final int PUBLISH_CLAIM_WORKER         = 4;

    private Timer timer;
    private static ExecutorService simulatorExecutor;
    private static ExecutorService signClaimExecutor;
    private static CompletionService<Respuesta> signClaimCompletionService;
    private static StringBuilder errorsLog;
    
    private List<String> signerList = null;
    private static AtomicLong signatures = new AtomicLong(0);
    private static AtomicLong signaturesFinished = new AtomicLong(0);
    private static AtomicLong signaturesERROR = new AtomicLong(0);
    private static AtomicLong signaturesOK = new AtomicLong(0);
    
    private Evento event = null;
    private UserBaseData userBaseData = null;
    private VotingSimulationData simulationData = null;
    
    //private AtomicBoolean done = new AtomicBoolean(true);
    
    private static long begin;
    
    private SMIMEMessageWrapper signedPublishrequestSMIME;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    
    public ClaimProcessSimulator(VotingSimulationData simulationData) {
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
        signClaimCompletionService.submit(new SignatureClaimLauncher(
                nif,event.getEventoId()));
        signatures.getAndIncrement();        
    }
    
    private void validateReceipts() {
        logger.debug("******** validateReceipts");
        while (simulationData.getNumberOfRequests() > signaturesFinished.get()) {
            try {
                Future<Respuesta> f = signClaimCompletionService.take();
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
        signClaimExecutor = Executors.newFixedThreadPool(100);
        signClaimCompletionService = 
                new ExecutorCompletionService<Respuesta>(signClaimExecutor);
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
            String subject = ContextoPruebas.getString("publishClaimMsgSubject");
            
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                ContextoPruebas.getUsuarioPruebas().getKeyStore(),
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.VOTE_SIGN_MECHANISM);
            signedPublishrequestSMIME = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.getUsuarioPruebas().getEmail(), 
                    ContextoPruebas.getControlAcceso().getNombreNormalizado(), 
                    eventStr, subject,  null);
            
            new TimeStampWorker(TIME_STAMP_WORKER, ContextoPruebas.getUrlTimeStampServer(),
                    this, signedPublishrequestSMIME.getTimeStampRequest(),
                    ContextoPruebas.getControlAcceso().getTimeStampCert()).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            countDownLatch.countDown();
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
                    logger.error("### ERROR - " + worker.getMessage());
                    countDownLatch.countDown();
                }
                break;
            case CA_CERT_INITIALIZER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    publishEvent();
                } else {
                    logger.error("### ERROR - " + worker.getMessage());
                    countDownLatch.countDown();
                }       
                break;   
            case TIME_STAMP_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        signedPublishrequestSMIME.setTimeStampToken((TimeStampWorker)worker);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        signedPublishrequestSMIME.writeTo(baos);
                        new DocumentSenderWorker(PUBLISH_CLAIM_WORKER, 
                                baos.toByteArray(), Contexto.SIGNED_CONTENT_TYPE,
                                ContextoPruebas.getClaimServiceURL(), this).execute();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        countDownLatch.countDown();
                    }
                } else {
                    logger.error("### ERROR - " + worker.getMessage());
                    countDownLatch.countDown();
                }
                break;
            case PUBLISH_CLAIM_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        byte[] responseBytes = worker.getMessage().getBytes();
                        FileUtils.copyStreamToFile(new ByteArrayInputStream(responseBytes), 
                            new File(ContextoPruebas.APPDIR + "VotingPublishReceipt"));
                        SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(null, 
                                new ByteArrayInputStream(responseBytes), 
                                "VotingPublishReceipt");
                        dnieMimeMessage.verify(
                                ContextoPruebas.INSTANCIA.getSessionPKIXParameters());
                        logger.debug("--- dnieMimeMessage.getSignedContent(): " 
                                + dnieMimeMessage.getSignedContent());
                        event = Evento.parse(dnieMimeMessage.getSignedContent());
                        logger.debug("Respuesta - Evento ID: " + event.getEventoId());
                        launchExecutors();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        countDownLatch.countDown();
                    }
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
            File jsonFile = File.createTempFile("ClaimProcessSimulation", ".json");
            jsonFile.deleteOnExit();
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("simulatorFiles/claimsSimulationData.json"), jsonFile); 
            simulationData = VotingSimulationData.parse(FileUtils.getStringFromFile(jsonFile));
        }
        ClaimProcessSimulator simuHelper = new ClaimProcessSimulator(simulationData);
        simuHelper.init();
    }
}
