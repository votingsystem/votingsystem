package org.sistemavotacion.test.simulation;

import com.itextpdf.text.pdf.PdfReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.sistemavotacion.test.modelo.VotingSimulationData;
import org.sistemavotacion.test.modelo.UserBaseSimulationData;
import org.sistemavotacion.test.simulation.callable.BackupValidator;
import org.sistemavotacion.test.util.SimulationUtils;
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
* 4)- Initialice user base data
* 5)- When finishes user base data initilization, 'setSimulationResult' inits simulation 
*/
public class VotingProcessSimulator extends  Simulator<VotingSimulationData>  {
    
    private static Logger logger = LoggerFactory.getLogger(VotingProcessSimulator.class);
    
    private final ExecutorService simulatorExecutor;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Override public VotingSimulationData call() throws Exception {
        logger.debug("call");
        String urlInfoServidor = ContextoPruebas.getURLInfoServidor(
                StringUtils.prepararURL(simulationData.getAccessControlURL()));
        InfoGetterWorker worker = new InfoGetterWorker(
                null, urlInfoServidor, null, null);
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
        } else logger.error(worker.getErrorMessage());
        
        
        countDownLatch.await();
        if(simulatorExecutor != null) simulatorExecutor.shutdown();
        
        logger.debug("--------------- SIMULATION RESULT------------------");
        logger.info("Duration: " + simulationData.getDurationStr());
        logger.info("Number of requests projected: " + simulationData.getNumOfElectors());
        logger.info("Number access request ERRORs: " + simulationData.getNumAccessRequestsERROR());
        logger.info("Number of vote requests: " + simulationData.getNumVotingRequests());
        logger.info("Number of votes OK: " +  simulationData.getNumVotingRequestsOK());
        logger.info("Number of votes ERROR: " + simulationData.getNumVotingRequestsERROR());    
        String errorsMsg = getFormattedErrorList();
        if(errorsMsg != null) {
            logger.info(" ************* " + geterrorList().size() + " ERRORS: \n" + 
                        errorsMsg);
        }
        logger.debug("------------------- FINISHED --------------------------");        
        if(simulationListener != null)
            simulationListener.setSimulationResult(simulationData);
        return simulationData;
    }

    public enum Simulation {VOTING, ACCESS_REQUEST}
    

    private static Simulation simulation = Simulation.VOTING;
    private Evento event = null;
    private Evento.Estado nextEventState = null;
    private UserBaseSimulationData userBaseData = null;
    private VotingSimulationData simulationData = null;
    
    private SimulatorListener simulationListener;
    private final SignedMailGenerator signedMailGenerator;
    
    public VotingProcessSimulator(VotingSimulationData simulationData,
             SimulatorListener simulationListener) throws Exception {
        super(simulationData);
        this.simulationData = simulationData;
        nextEventState = simulationData.getEvento().getNextState();
        this.userBaseData = simulationData.getUserBaseData();   
        this.simulationListener = simulationListener;
        simulatorExecutor = Executors.newFixedThreadPool(5);
        signedMailGenerator = new SignedMailGenerator(
                    ContextoPruebas.INSTANCE.getUserTest().getKeyStore(),
                    ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.PASSWORD.toCharArray(),
                    ContextoPruebas.VOTE_SIGN_MECHANISM);
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
            initControlCenter();
        } else logger.error(worker.getErrorMessage());
    }
    
    private void initControlCenter() throws Exception {
        logger.debug("initControlCenter");
        Set<ActorConIP> controlCenters = ContextoPruebas.INSTANCE.
                    getAccessControl().getCentrosDeControl();        
        if(controlCenters == null || controlCenters.isEmpty()) {
            associateControlCenter();
            return;
        } else {
            String urlServidor = StringUtils.prepararURL(
                controlCenters.iterator().next().getServerURL());
            String urlInfoServidor = ContextoPruebas.
                getURLInfoServidor(urlServidor);
            InfoGetterWorker worker = new InfoGetterWorker(null, 
                    urlInfoServidor, null, null);
            worker.execute();
            worker.get();
            if(Respuesta.SC_OK == worker.getStatusCode()) {
                ActorConIP controlCenter = ActorConIP.parse(worker.getMessage());
                String msg = SimulationUtils.checkActor(
                        controlCenter, ActorConIP.Tipo.CENTRO_CONTROL);
                if(msg == null) {
                    ContextoPruebas.INSTANCE.setControlCenter(controlCenter);
                    //Loaded Access Control and Control Center. Now we can publish  
                    publishEvent();
                }
            } else logger.error(worker.getErrorMessage());
        }
    }
    
    private void setNextEventState() throws Exception {
        logger.debug("setNextEventState");
        //Before sending de cancelation document we must send the test CA cert 
        //to control center
        byte[] rootCACertPEMBytes = CertUtil.fromX509CertToPEM (
            ContextoPruebas.INSTANCE.getRootCACert());
        String rootCAServiceURL = ContextoPruebas.INSTANCE.
                getControlCenterRootCAServiceURL();
        DocumentSenderWorker caSenderWorker = new DocumentSenderWorker(
                null, rootCACertPEMBytes, null, rootCAServiceURL, null);
        caSenderWorker.execute();
        caSenderWorker.get();//wait to get worker response
        if(Respuesta.SC_OK == caSenderWorker.getStatusCode()) {
            String cancelDataStr = event.getCancelEventJSON(
                    Contexto.INSTANCE.getAccessControl().getServerURL(),
                    nextEventState).toString();
            String msgSubject = ContextoPruebas.INSTANCE.getString(
                "setNextEventStateMsgSubject") + event.getEventoId();
            SMIMEMessageWrapper cancelSmimeDocument =
                    signedMailGenerator.genMimeMessage(
                    ContextoPruebas.INSTANCE.getUserTest().getEmail(),
                    ContextoPruebas.INSTANCE.getAccessControl().
                    getNombreNormalizado(), cancelDataStr, msgSubject, null);
            SMIMESignedSenderWorker eventStateChangerWorker = 
                    new SMIMESignedSenderWorker(null,
                    cancelSmimeDocument, ContextoPruebas.INSTANCE.
                    getCancelEventURL(), null, null, null);
            eventStateChangerWorker.execute();
            eventStateChangerWorker.get();
            logger.debug("eventStateChangerWorker - status: " + 
                    eventStateChangerWorker.getStatusCode());
            if(Respuesta.SC_OK == eventStateChangerWorker.getStatusCode()) {
                if(simulationData.getBackupRequestEmail() != null) requestBackup();
            } else logger.error(eventStateChangerWorker.getErrorMessage());
        } else logger.error(caSenderWorker.getErrorMessage());
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
    
    private void inicializarBaseUsuarios() throws Exception{
        logger.debug("inicializarBaseUsuarios");
        FutureTask<UserBaseSimulationData> futureUserBase = 
                new FutureTask<UserBaseSimulationData>(
                new  UserBaseDataSimulator(userBaseData));
        simulatorExecutor.execute(futureUserBase);
        UserBaseSimulationData ubd = futureUserBase.get();
        logger.debug("UserBaseSimulationData - status: " + ubd.getStatusCode());
        if(Respuesta.SC_OK == ubd.getStatusCode()) {
            switch(simulation) {
                case ACCESS_REQUEST:
                    AccessRequestSimulator accessRequest = 
                            new AccessRequestSimulator(simulationData, null);
                    accessRequest.call();
                    break;
                case VOTING:
                    FutureTask<VotingSimulationData> futureVotingData = 
                            new FutureTask<VotingSimulationData>(
                            new VotingSimulator(simulationData));
                    simulatorExecutor.execute(futureVotingData);
                    VotingSimulationData simulData = futureVotingData.get();
                    logger.debug("VotingSimulationData - status: " + simulData.getStatusCode());
                    if(Respuesta.SC_OK == simulData.getStatusCode()) {
                        if(nextEventState != null) {               
                            setNextEventState();
                        }
                    }
                    break;
            }
        } else logger.error(ubd.getMessage());
        
        countDownLatch.countDown();
    }
    
    private void publishEvent() throws Exception {
        logger.debug("publishEvent");
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String dateStr = formatter.format(date);
        event = simulationData.getEvento();
        nextEventState = event.getNextState();
        String subjectDoc = event.getAsunto()+ " -> " + dateStr;
        event.setAsunto(subjectDoc);    
        event.setTipo(Tipo.VOTACION);
        event.setCentroControl(ContextoPruebas.INSTANCE.getControlCenter());
        String eventParaPublicar = event.toJSON().toString();
        String subject = ContextoPruebas.INSTANCE.getString("votingPublishMsgSubject");
        SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(
                ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                ContextoPruebas.INSTANCE.getAccessControl().getNombreNormalizado(), 
                eventParaPublicar, subject, null);

        SMIMESignedSenderWorker worker = new SMIMESignedSenderWorker(null, 
                smimeDocument, ContextoPruebas.INSTANCE. getURLGuardarEventoParaVotar(), 
                null, null, null);
        worker.execute();
        worker.get();
        if(Respuesta.SC_OK == worker.getStatusCode()) {
            byte[] responseBytes = worker.getMessage().getBytes();
            FileUtils.copyStreamToFile(new ByteArrayInputStream(responseBytes), 
                new File(ContextoPruebas.DEFAULTS.APPDIR + "VotingPublishReceipt"));
            SMIMEMessageWrapper mimeMessage = new SMIMEMessageWrapper(null, 
                    new ByteArrayInputStream(responseBytes), 
                    "VotingPublishReceipt");
            mimeMessage.verify(
                    ContextoPruebas.INSTANCE.getSessionPKIXParameters());
            event = Evento.parse(mimeMessage.getSignedContent());
            simulationData.setEvento(event);
            logger.debug("Respuesta - Evento ID: " + event.getEventoId() + 
                    " - url: " + event.getUrl());
            inicializarBaseUsuarios();
        } else logger.error(worker.getErrorMessage());
    }
    
    private void associateControlCenter() throws Exception {
        String documentoAsociacion = ActorConIP.getAssociationDocumentJSON(
                simulationData.getControlCenterURL()).toString();
        String msgSubject = ContextoPruebas.INSTANCE.getString(
            "associateControlCenterMsgSubject");
        SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(
                ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                ContextoPruebas.INSTANCE.getAccessControl().getNombreNormalizado(), 
                documentoAsociacion, msgSubject, null);

        SMIMESignedSenderWorker worker =new SMIMESignedSenderWorker(null, 
                smimeDocument, ContextoPruebas.INSTANCE.getURLAsociarActorConIP(), 
                null, null, null);
        worker.execute();
        worker.get();
        if(Respuesta.SC_OK == worker.getStatusCode()) {
            ActorConIP controlCenter = ActorConIP.parse(worker.getMessage());
            ContextoPruebas.INSTANCE.setControlCenter(controlCenter);
            String msg = SimulationUtils.checkActor(
                    controlCenter, ActorConIP.Tipo.CENTRO_CONTROL);
            if(msg == null) {
                publishEvent();
            } 
        } else logger.error(worker.getErrorMessage());
    }

    public static void main(String[] args) throws Exception {
        VotingSimulationData simulationData = null;
        if(args != null && args.length > 0) {
            logger.debug("args[0]");
            simulationData = VotingSimulationData.parse(args[0]);
            if(args[1] != null) simulation = Simulation.valueOf(args[1]);
        } else {
            simulation = Simulation.VOTING;
            File jsonFile = File.createTempFile("VotingProcessSimulation", ".json");
            jsonFile.deleteOnExit();
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("simulatorFiles/votingSimulationData.json"), jsonFile); 
            simulationData = VotingSimulationData.parse(FileUtils.getStringFromFile(jsonFile));
            logger.debug("Simulation for Access Control: " + simulationData.getAccessControlURL());
        }
        VotingProcessSimulator simuHelper = new 
                VotingProcessSimulator(simulationData, null);
        simuHelper.call();
    }
}
