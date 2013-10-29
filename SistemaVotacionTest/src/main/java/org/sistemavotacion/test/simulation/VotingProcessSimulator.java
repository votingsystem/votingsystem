package org.sistemavotacion.test.simulation;

import com.itextpdf.text.pdf.PdfReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.callable.InfoGetter;
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
import org.sistemavotacion.test.simulation.callable.ServerInitializer;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.callable.InfoSender;
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
* 4)- Initialice user base data
* 5)- When finishes user base data initilization, 'setSimulationResult' inits simulation 
*/
public class VotingProcessSimulator extends  Simulator<VotingSimulationData> 
    implements SimulatorListener {
    
    private static Logger logger = LoggerFactory.getLogger(
            VotingProcessSimulator.class);

    @Override public void processResponse(Respuesta respuesta) { }
    
    
    public enum Simulation {VOTING, ACCESS_REQUEST}
    

    private static Simulation simulation = Simulation.VOTING;
    private Evento event = null;
    private Evento.Estado nextEventState = null;
    private UserBaseSimulationData userBaseData = null;
    private VotingSimulationData simulationData = null;
    
    private SimulatorListener simulationListener;
    private final SignedMailGenerator signedMailGenerator;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Override public Respuesta call() throws Exception {
        logger.debug("call - process: " + ManagementFactory.getRuntimeMXBean().getName());
        ServerInitializer accessControlInitializer = 
                new ServerInitializer(simulationData.getAccessControlURL(),
                ActorConIP.Tipo.CONTROL_ACCESO);
        Respuesta respuesta = accessControlInitializer.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            initControlCenter();
        } else logger.error(respuesta.getMensaje());
                
        countDownLatch.await();
        logger.debug("--------- SIMULATION RESULT - EVENT: " + event.getEventoId() 
                + " -----------------");
        logger.info("Begin: " + DateUtils.getStringFromDate(
                simulationData.getBeginDate())  + " - Duration: " + 
                simulationData.getDurationStr());
        logger.info("Number of requests projected: " + simulationData.getNumOfElectors());
        logger.info("Number access request ERRORs: " + simulationData.getNumAccessRequestsERROR());
        logger.info("Number of vote requests: " + simulationData.getNumVotingRequests());
        logger.info("Number of votes OK: " +  simulationData.getNumVotingRequestsOK());
        logger.info("Number of votes ERROR: " + simulationData.getNumVotingRequestsERROR());    
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
    
    public VotingProcessSimulator(VotingSimulationData simulationData,
             SimulatorListener simulationListener) throws Exception {
        super(simulationData);
        this.simulationData = simulationData;
        nextEventState = simulationData.getEvento().getNextState();
        this.userBaseData = simulationData.getUserBaseData();   
        this.simulationListener = simulationListener;
        signedMailGenerator = new SignedMailGenerator(
                    ContextoPruebas.INSTANCE.getUserTest().getKeyStore(),
                    ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.PASSWORD.toCharArray(),
                    ContextoPruebas.VOTE_SIGN_MECHANISM);
    }
    
    private void initControlCenter() throws Exception {
        logger.debug("initControlCenter");
        Set<ActorConIP> controlCenters = ContextoPruebas.INSTANCE.
                    getAccessControl().getCentrosDeControl();        
        if(controlCenters == null || controlCenters.isEmpty()) {
            associateControlCenter();
            return;
        } else {
            ServerInitializer controlCenterInitializer = 
                new ServerInitializer(simulationData.getControlCenterURL(),
                ActorConIP.Tipo.CENTRO_CONTROL);
            Respuesta respuesta = controlCenterInitializer.call();
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                publishEvent();
            } else logger.error(respuesta.getMensaje());
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
        InfoSender caSenderWorker = new InfoSender(
                null, rootCACertPEMBytes, null, rootCAServiceURL);
        Respuesta respuesta = caSenderWorker.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
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
            SMIMESignedSender eventStateChangerWorker = 
                    new SMIMESignedSender(null,
                    cancelSmimeDocument, ContextoPruebas.INSTANCE.
                    getCancelEventURL(), null, null);
            respuesta = eventStateChangerWorker.call();
            logger.debug("eventStateChangerWorker - status: " + 
                    respuesta.getCodigoEstado());
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                if(simulationData.getBackupRequestEmail() != null) requestBackup();
            } else logger.error(respuesta.getMensaje());
        } else logger.error(respuesta.getMensaje());
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
            infoGetter.call();
            /*byte[] backupZipBytes = respuesta.getMessageBytes();
            if(backupZipBytes != null) {
                FutureTask<Respuesta> future = new FutureTask<Respuesta>(
                    new BackupValidator(backupZipBytes));
                simulatorExecutor.execute(future);
                respuesta = future.get();
            } else logger.error("ZIP file null");
            logger.debug("BackupRequestWorker - status: " + respuesta.getCodigoEstado());*/
        } else logger.error(respuesta.getMensaje());
    }
    
    private void inicializarBaseUsuarios() throws Exception{
        logger.debug("inicializarBaseUsuarios");
        UserBaseSimulationData ubd = ContextoPruebas.INSTANCE.getUserBaseData();
        if(ubd == null) {
            Future<Respuesta<UserBaseSimulationData>> future = ContextoPruebas.
                INSTANCE.submitSimulation(new  UserBaseDataSimulator(userBaseData));
            ubd = future.get().getData();
        } else logger.debug("UserBaseSimulationData from Context");
        logger.debug("UserBaseSimulationData - status: " + ubd.getStatusCode());
        if(Respuesta.SC_OK != ubd.getStatusCode()) { 
            logger.error(ubd.getMessage());
            errorList.addAll(ubd.getErrorList());
            
        }
        switch(simulation) {
            case ACCESS_REQUEST:
                AccessRequestSimulator accessRequest = 
                        new AccessRequestSimulator(simulationData, null);
                accessRequest.call();
                break;
            case VOTING:
                Future<Respuesta<VotingSimulationData>> futureVotingData = 
                        ContextoPruebas.INSTANCE.submitSimulation(new VotingSimulator(
                        simulationData, this));
                VotingSimulationData simulData = futureVotingData.get().getData();
                if(nextEventState != null) {               
                    setNextEventState();
                }
                break;
        }
        
        countDownLatch.countDown();
    }
    
    private void publishEvent() throws Exception {
        logger.debug("publishEvent");
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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

        SMIMESignedSender worker = new SMIMESignedSender(null, 
                smimeDocument, ContextoPruebas.INSTANCE. getURLGuardarEventoParaVotar(), 
                null, null);
        Respuesta respuesta = worker.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            byte[] responseBytes = respuesta.getMessageBytes();
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
        } else logger.error(respuesta.getMensaje());
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

        SMIMESignedSender worker =new SMIMESignedSender(null, 
                smimeDocument, ContextoPruebas.INSTANCE.getURLAsociarActorConIP(), 
                null, null);
        Respuesta respuesta = worker.call();

        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            ServerInitializer controlCenterInitializer = 
                new ServerInitializer(simulationData.getControlCenterURL(),
                ActorConIP.Tipo.CENTRO_CONTROL);
            respuesta = controlCenterInitializer.call();
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                publishEvent();
            } else logger.error(respuesta.getMensaje());
        } else logger.error(respuesta.getMensaje());
    }

    public static void main(String[] args) throws Exception {
        VotingSimulationData simulationData = null;
        if(args != null && args.length > 0) {
            logger.debug("args[0]");
            simulationData = VotingSimulationData.parse(args[0]);
            if(args.length > 1 && args[1] != null) simulation = Simulation.valueOf(args[1]);
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
        System.exit(0);
    }
}
