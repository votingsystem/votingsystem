package org.sistemavotacion.test.simulation;

import com.itextpdf.text.pdf.PdfReader;
import org.sistemavotacion.test.modelo.SimulationData;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.Certificate;
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
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.pdf.PdfFormHelper;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.simulation.callable.BackupValidator;
import org.sistemavotacion.test.simulation.callable.ClaimSigner;
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
* 1)- Check Access Control and initialice CA cert
* 2)- Check if there's a Control Center associated and associated if not.
* 3)- Publish Event
* 4)- Send signatures to the event
*/
public class ClaimProcessSimulator extends Simulator<SimulationData>  
        implements ActionListener  {
    
    private static Logger logger = LoggerFactory.getLogger(ClaimProcessSimulator.class);

    private static ExecutorService simulatorExecutor;
    private static ExecutorService signClaimExecutor;
    private static CompletionService<Respuesta> signClaimCompletionService;
    
    private Evento event = null;
    private Evento.Estado nextEventState = null;
    private List<String> signerList = null;

    private SignedMailGenerator signedMailGenerator;
    private SimulatorListener simulationListener;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    
    public ClaimProcessSimulator(SimulationData simulationData, 
            SimulatorListener simulationListener) {
        super(simulationData);
        this.simulationListener = simulationListener;
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
            publishEvent();
        }
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
           }
        }
 
    }     
    
    private void launchSignature(String nif) throws Exception {
        signClaimCompletionService.submit(new ClaimSigner(
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
        }
        if(nextEventState != null) setNextEventState();
        countDownLatch.countDown();
    }

    private void setNextEventState() throws Exception {
        logger.debug("setNextEventState");
        String cancelDataStr = event.getCancelEventJSON(
            Contexto.INSTANCE.getAccessControl().getServerURL(), 
            nextEventState).toString();
        String msgSubject = ContextoPruebas.INSTANCE.getString("cancelClaimMsgSubject");
        SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(
                ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                Contexto.INSTANCE.getAccessControl().getNombreNormalizado(), 
                cancelDataStr, msgSubject,  null);
        SMIMESignedSenderWorker worker = new SMIMESignedSenderWorker(
                null, smimeDocument, ContextoPruebas.INSTANCE.getCancelEventURL(), 
                null, null, null);
        worker.execute();
        worker.get();
        if(Respuesta.SC_OK == worker.getStatusCode()) {
            if(simulationData.getBackupRequestEmail() != null) requestBackup();
        }
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
            if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
                addErrorList(respuesta.getErrorList());
            }
        } else logger.error(worker.getErrorMessage());
    }
    
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
    
    private void publishEvent() throws Exception {
        logger.debug("publishEvent");
           
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
        SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(
                ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                Contexto.INSTANCE.getAccessControl().getNombreNormalizado(), 
                eventStr, msgSubject,  null);
            
            
        SMIMESignedSenderWorker worker = new SMIMESignedSenderWorker(
                null, smimeDocument, ContextoPruebas.INSTANCE.getClaimServiceURL(), 
                null, null, null);
        worker.execute();
        worker.get();
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
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
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
            ActorConIP accessControl = ActorConIP.parse(worker.getMessage());
            String msg = SimulationUtils.checkActor(
                    accessControl, ActorConIP.Tipo.CONTROL_ACCESO);
            if(msg == null) {
                ContextoPruebas.INSTANCE.setControlAcceso(accessControl);
                initCA_AccessControl();
            } 
        } else logger.error(worker.getMessage());
                
        logger.debug("- await");
        countDownLatch.await();
        logger.debug("- shutdown executors");
        
        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        if(simulatorExecutor != null)  simulatorExecutor.shutdownNow();
        if(signClaimExecutor != null)  signClaimExecutor.shutdownNow();   
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
            logger.info(" ************* " + errorList.size() + " ERRORS: \n" + 
                        errorsMsg);
        }
        logger.debug("------------------ FINISHED -----------------------");
        if(simulationListener != null) 
            simulationListener.setSimulationResult(simulationData);
        
        return simulationData;
    }

}
