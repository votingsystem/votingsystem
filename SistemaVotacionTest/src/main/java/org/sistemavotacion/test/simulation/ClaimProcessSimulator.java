package org.sistemavotacion.test.simulation;

import com.itextpdf.text.pdf.PdfReader;
import org.sistemavotacion.test.modelo.SimulationData;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.management.ManagementFactory;
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
import org.sistemavotacion.callable.InfoGetter;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.pdf.PdfFormHelper;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.simulation.callable.ServerInitializer;
import org.sistemavotacion.test.simulation.callable.ClaimSigner;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.callable.PDFSignedSender;
import org.sistemavotacion.callable.SMIMESignedSender;
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
        SMIMESignedSender signedSender = new SMIMESignedSender(
                null, smimeDocument, ContextoPruebas.INSTANCE.getCancelEventURL(), 
                null, null);
        Respuesta respuesta = signedSender.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
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
        PDFSignedSender signedSender = new PDFSignedSender(null, 
                ContextoPruebas.INSTANCE.getUrlBackupEvents(), 
                null, null, null, requestBackupPDF, signerPrivateKey, 
                signerCertChain, null);
        Respuesta respuesta = signedSender.call();
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
                logger.debug("BackupRequestWorker - status: " + respuesta.getCodigoEstado());
                if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
                    addErrorList(respuesta.getErrorList());
                }*/
            } else logger.error(respuesta.getMensaje());
        } else logger.error(respuesta.getMensaje());
    }
    
    private void initExecutors(){
        simulatorExecutor = Executors.newFixedThreadPool(100);
        signClaimCompletionService = 
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

        SMIMESignedSender signedSender = new SMIMESignedSender(
                null, smimeDocument, ContextoPruebas.INSTANCE.getClaimServiceURL(), 
                null, null);
        Respuesta respuesta = signedSender.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            try {
                byte[] responseBytes = respuesta.getMessageBytes();
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
        System.exit(0);
    }

    @Override  public Respuesta call() throws Exception {
        logger.debug("call - NumberOfRequestsProjected: " +  
                simulationData.getNumRequestsProjected() + " - process: " + 
                ManagementFactory.getRuntimeMXBean().getName());
        simulationData.setBegin(System.currentTimeMillis());
        ServerInitializer accessControlInitializer = 
                new ServerInitializer(simulationData.getAccessControlURL(), 
                ActorConIP.Tipo.CONTROL_ACCESO);
        Respuesta respuesta = accessControlInitializer.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
             publishEvent();
        } else logger.error(respuesta.getMensaje());
      
        logger.debug("- await");
        countDownLatch.await();
        logger.debug("- shutdown executors");
        
        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        if(simulatorExecutor != null)  simulatorExecutor.shutdownNow();
        logger.debug("------- SIMULATION RESULT - Event: " + event.getId());  
        simulationData.setFinish(System.currentTimeMillis());
                logger.info("Begin: " + DateUtils.getStringFromDate(
                simulationData.getBeginDate())  + " - Duration: " + 
                simulationData.getDurationStr());
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
        respuesta = new Respuesta(Respuesta.SC_FINALIZADO,simulationData);
        if(simulationListener != null) 
            simulationListener.processResponse(respuesta);      
        return respuesta;
    }

}
