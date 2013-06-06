package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.modelo.SimulationData;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.pdf.PdfFormHelper;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.simulation.launcher.SignatureClaimLauncher;
import org.sistemavotacion.test.util.SimulationUtils;
import org.sistemavotacion.test.worker.BackupRequestWorker;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.SMIMESignedSenderWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.sistemavotacion.worker.VotingSystemWorkerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 1)- Check Access Control and initialice CA cert
* 2)- Check if there's a Control Center associated and associated if not.
* 3)- Publish Event
* 4)- Send signatures to the event
*/
public class ClaimProcessSimulator extends Simulator<SimulationData>  implements 
        VotingSystemWorkerListener, ActionListener  {
    
    private static Logger logger = LoggerFactory.getLogger(ClaimProcessSimulator.class);
    
    public enum Worker implements VotingSystemWorkerType{
        ACCESS_CONTROL_GETTER, CA_CERT_INITIALIZER, PUBLISH_CLAIM, CANCEL,
        BACKUP_REQUEST}

    private static ExecutorService simulatorExecutor;
    private static ExecutorService signClaimExecutor;
    private static CompletionService<Respuesta> signClaimCompletionService;
    
    private Evento event = null;
    private Evento.Estado nextEventState = null;
    private SimulationData simulationData = null;
    
    //private AtomicBoolean done = new AtomicBoolean(true);
    private List<String> signerList = null;

    private SignedMailGenerator signedMailGenerator;
    private SMIMEMessageWrapper smimeDocument;
    private SimulatorListener simulationListener;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    public ClaimProcessSimulator(SimulationData simulationData, 
            SimulatorListener simulationListener) {
        super(simulationData);
        this.simulationListener = simulationListener;
        this.simulationData = simulationData;
    }

    @Override public void init() throws Exception {
        logger.debug("inits - NumberOfRequestsProjected: " +  
                simulationData.getNumRequestsProjected());
        simulationData.setBegin(System.currentTimeMillis());
        String urlServidor = StringUtils.prepararURL(simulationData.getAccessControlURL());
        String urlInfoServidor = ContextoPruebas.getURLInfoServidor(urlServidor);
        new InfoGetterWorker(Worker.ACCESS_CONTROL_GETTER, urlInfoServidor, 
                null, this).execute();
        countDownLatch.await();
        finish();
    }
    
    public void launchRequests () throws Exception {
        logger.debug("launchRequests - number of projected requests: " + 
                simulationData.getNumRequestsProjected());
        signerList = new ArrayList<String>();
        for(int i = 0; i < simulationData.getNumRequestsProjected(); i++) {
            signerList.add(NifUtils.getNif(i));
        }
        if(simulationData.isTimerBased()) startTimer(this); 
        else {
            while(!signerList.isEmpty()) {//
               if((simulationData.getNumRequests() - 
                       simulationData.getNumRequestsColected()) < 
                       simulationData.getMaxPendingResponses()) {
                   int randomSigner = new Random().nextInt(signerList.size());
                   launchSignature(signerList.remove(randomSigner));
               } else Thread.sleep(200);
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
        simulationData.getAndIncrementNumRequests();
    }
    
    private void readResponses() throws Exception {
        logger.debug(" --- readResponses - NumRequestsProjected: " + 
                simulationData.getNumRequestsProjected());
        while (simulationData.getNumRequestsProjected() > 
                simulationData.getNumRequestsColected()) {
            try {
                Future<Respuesta> f = signClaimCompletionService.take();
                Respuesta respuesta = f.get();   
                if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    simulationData.getAndIncrementNumRequestsOK();
                } else {
                    String msg = "Signature ERROR - msg: " + respuesta.getMensaje();
                    logger.error(msg);
                    addErrorMsg(msg);
                    simulationData.getAndIncrementNumRequestsERROR();
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                String msg = "Signature ERROR - msg: " + ex.getMessage();
                addErrorMsg(msg);
                simulationData.getAndIncrementNumRequestsERROR();
            }
            //done.set(true);
        }
        if(nextEventState != null) cancelEvent();
        if(simulationData.getBackupRequestEmail() != null) requestBackup();
        else countDownLatch.countDown(); 
    }

    private void cancelEvent() throws Exception {
        logger.debug("cancelEvent");
        String cancelDataStr = event.getCancelEventJSON(
            Contexto.INSTANCE.getAccessControl().getServerURL(), 
            nextEventState).toString();
        String msgSubject = ContextoPruebas.INSTANCE.getString("cancelClaimMsgSubject");
        smimeDocument = signedMailGenerator.genMimeMessage(
                ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                Contexto.INSTANCE.getAccessControl().getNombreNormalizado(), 
                cancelDataStr, msgSubject,  null);
        new SMIMESignedSenderWorker(Worker.CANCEL, smimeDocument, 
                ContextoPruebas.INSTANCE.getCancelEventURL(), null, null, this).execute();
    }

    private void requestBackup() throws Exception {
        logger.debug("requestBackup");
        byte[] requestBackupPDFBytes = PdfFormHelper.getBackupRequest(
            event.getEventoId().toString(), event.getAsunto(), 
                            simulationData.getBackupRequestEmail());
        new BackupRequestWorker(Worker.BACKUP_REQUEST, 
                ContextoPruebas.INSTANCE.getUrlBackupEvents(), 
                requestBackupPDFBytes, this).execute(); 
    }
    
    @Override public void processVotingSystemWorkerMsg(List<String> messages) { }
    
    
    private void initExecutors(){
        simulatorExecutor = Executors.newFixedThreadPool(5);
        signClaimExecutor = Executors.newFixedThreadPool(100);
        signClaimCompletionService = 
                new ExecutorCompletionService<Respuesta>(signClaimExecutor);
        
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
            nextEventState = event.getNextState();
            String claimSubject = event.getAsunto()+ " -> " + dateStr;
            event.setAsunto(claimSubject);
            event.setTipo(Tipo.EVENTO_RECLAMACION);
            String eventStr = event.toJSON().toString();
            String msgSubject = ContextoPruebas.INSTANCE.
                    getString("publishClaimMsgSubject");
            signedMailGenerator = new SignedMailGenerator(
                    ContextoPruebas.INSTANCE.getUserTest().getKeyStore(),
                    ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.PASSWORD.toCharArray(),
                    ContextoPruebas.VOTE_SIGN_MECHANISM);
            smimeDocument = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                    Contexto.INSTANCE.getAccessControl().getNombreNormalizado(), 
                    eventStr, msgSubject,  null);
            
            
            new SMIMESignedSenderWorker(Worker.PUBLISH_CLAIM, smimeDocument, 
                    ContextoPruebas.INSTANCE.getClaimServiceURL(), 
                    null, null, this).execute();

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            countDownLatch.countDown();
        }

    }

    private void initilizeAuthorityCert() {
        try {
            byte[] rootCACertPemBytes = CertUtil.fromX509CertToPEM (
                ContextoPruebas.INSTANCE.getRootCACert());
            String urlAnyadirCertificadoCA = ContextoPruebas.getRootCAServiceURL(
                Contexto.INSTANCE.getAccessControl().getServerURL());
            new DocumentSenderWorker(Worker.CA_CERT_INITIALIZER, rootCACertPemBytes, 
                    null, urlAnyadirCertificadoCA, this).execute();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    
    @Override public void showResult(VotingSystemWorker worker)  {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
                " - worker: " + worker.getType());
        String msg = null;
        switch((Worker)worker.getType()) {
            case ACCESS_CONTROL_GETTER:           
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP accessControl = ActorConIP.parse(worker.getMessage());
                        msg = SimulationUtils.checkActor(accessControl, ActorConIP.Tipo.CONTROL_ACCESO);
                        if(msg == null) {
                            ContextoPruebas.INSTANCE.setControlAcceso(accessControl);
                            initilizeAuthorityCert();
                            return;
                        }
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                }
                break;
            case CA_CERT_INITIALIZER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    publishEvent();
                    return;
                }
                break;   
            case PUBLISH_CLAIM:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        byte[] responseBytes = worker.getMessage().getBytes();
                        FileUtils.copyStreamToFile(new ByteArrayInputStream(responseBytes), 
                            new File(ContextoPruebas.DEFAULTS.APPDIR + "VotingPublishReceipt"));
                        SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(null, 
                                new ByteArrayInputStream(responseBytes), 
                                "VotingPublishReceipt");
                        dnieMimeMessage.verify(ContextoPruebas.INSTANCE.
                                getSessionPKIXParameters());
                        event = Evento.parse(dnieMimeMessage.getSignedContent());
                        initExecutors();
                        return;
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                }
                break;
            case CANCEL:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    logger.debug("CANCEL OK - Event -> " + event.getEventoId() + 
                            " set to state " + nextEventState.toString());
                    try {
                        if(simulationData.getBackupRequestEmail() != null) 
                            requestBackup();
                        return;
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                }
                break;
            case BACKUP_REQUEST:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    MetaInf metaInf =  (MetaInf) worker.getRespuesta().getData();
                    logger.debug("BACKUP_REQUEST OK - Event -> " + event.getEventoId());
                    if(metaInf.getErrorsList() != null && 
                            !metaInf.getErrorsList().isEmpty()) {
                        addErrorList(metaInf.getErrorsList());
                    }
                    logger.debug("------------ META-INF BACKUP ------------");
                    logger.debug(metaInf.getFormattedInfo());
                    countDownLatch.countDown();
                    return;
                }
                break;
            default: msg = "UNKNOWN WORKER -> " + worker.getType();
        }
        if(msg == null) msg = worker.getErrorMessage();
        else msg = worker.getErrorMessage() + " - msg: " + msg; 
        logger.error(msg);
        addErrorMsg(msg);
        countDownLatch.countDown();
    }

    @Override public SimulationData getData() {
        return this.simulationData;
    }

    @Override public void finish() throws Exception {
        logger.debug("finish");
        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        if(simulatorExecutor != null)  simulatorExecutor.shutdownNow();
        if(signClaimExecutor != null)  signClaimExecutor.shutdownNow();         
        if(simulationListener != null) {           
            simulationListener.setSimulationResult(simulationData);
        } else { 
            logger.debug("--------------- SIMULATION RESULT------------------");  
            logger.info("Duration: " + simulationData.getDurationStr());
            logger.info("Number of projected requests: " + 
                    simulationData.getNumRequestsProjected());
            logger.info("Number of completed requests: " + 
                    simulationData.getNumRequestsColected());
            logger.info("Number of signatures OK: " + simulationData.getNumRequestsOK());
            logger.info("Number of signatures ERROR: " + simulationData.getNumRequestsERROR());
            String errorsMsg = getFormattedErrorList();
            if(errorsMsg != null) {
                logger.info(" ************* " + getErrorsList().size() + " ERRORS: \n" + 
                            errorsMsg);
            }
            logger.debug("------------------ FINISHED -----------------------");
            System.exit(0);
        }
    }


    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(timer)) {
            if(!signerList.isEmpty()) {
                try {
                    int randomElector = new Random().nextInt(signerList.size());
                    launchSignature(signerList.remove(randomElector));
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
            File jsonFile = File.createTempFile("ClaimProcessSimulation", ".json");
            jsonFile.deleteOnExit();
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("simulatorFiles/claimsSimulationData.json"), jsonFile); 
            simulationData = SimulationData.parse(FileUtils.getStringFromFile(jsonFile));
        }
        ClaimProcessSimulator simuHelper = new ClaimProcessSimulator(simulationData, null);
        simuHelper.init();
    }

}
