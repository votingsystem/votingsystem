package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.modelo.SimulationData;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
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
import org.sistemavotacion.test.modelo.VotingSimulationData;
import org.sistemavotacion.test.modelo.UserBaseSimulationData;
import org.sistemavotacion.test.util.SimulationUtils;
import org.sistemavotacion.test.worker.BackupRequestWorker;
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
* 4)- Initialice user base data
* 5)- When finishes user base data initilization, 'setSimulationResult' inits simulation 
*/
public class VotingProcessSimulator extends  Simulator<VotingSimulationData>  
        implements VotingSystemWorkerListener, SimulatorListener {
    
    private static Logger logger = LoggerFactory.getLogger(VotingProcessSimulator.class);

    public enum Simulation {VOTING, ACCESS_REQUEST}
    
    public enum Worker implements VotingSystemWorkerType{
        ACCESS_CONTROL_GETTER, CONTROL_CENTER_GETTER, 
        ACCESSCONTROL_CA_CERT_INITIALIZER, CONTROLCENTER_CA_CERT_INITIALIZER, 
        ASSOCIATE_CONTROL_CENTER, PUBLISH_DOCUMENT, CANCEL_EVENT, BACKUP_REQUEST}

    private static Simulation simulation = Simulation.VOTING;
    private Evento event = null;
    private Evento.Estado nextEventState = null;
    private UserBaseSimulationData userBaseData = null;
    private VotingSimulationData simulationData = null;
        
    private SMIMEMessageWrapper smimeDocument;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    private SimulatorListener simulationListener;
    private final SignedMailGenerator signedMailGenerator;
    
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
    
    @Override public void init() throws Exception {
        try {
            String urlInfoServidor = ContextoPruebas.getURLInfoServidor(
                    StringUtils.prepararURL(simulationData.getAccessControlURL()));
            new InfoGetterWorker(Worker.ACCESS_CONTROL_GETTER, urlInfoServidor, 
                null, this).execute();
            countDownLatch.await();
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }
        finish();
    }

    private void cancelEvent() throws Exception {
        logger.debug("cancelEvent");
        //Before sending de cancelation document we must send the test CA cert 
        //to control center
        byte[] rootCACertPEMBytes = CertUtil.fromX509CertToPEM (
            ContextoPruebas.INSTANCE.getRootCACert());
        String rootCAServiceURL = ContextoPruebas.INSTANCE.
                getControlCenterRootCAServiceURL();
        new DocumentSenderWorker(Worker.CONTROLCENTER_CA_CERT_INITIALIZER, 
            rootCACertPEMBytes, null, rootCAServiceURL, this).execute();
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
    
    @Override public void setSimulationResult(SimulationData data) {
        logger.debug("setSimulationResult - data: " + data.getClass().getSimpleName());

        
        if(data instanceof UserBaseSimulationData) {
            UserBaseSimulationData ubd = (UserBaseSimulationData)data;
            simulationData.setUserBaseData(ubd);
            switch(simulation) {
                case ACCESS_REQUEST:
                    AccessRequestSimulator accessRequest = 
                            new AccessRequestSimulator(simulationData, this);
                    accessRequest.init();
                    break;
                case VOTING:
                    VotingSimulator votacion = 
                            new VotingSimulator(simulationData, this);
                    votacion.init();
                    break;
            }

        } else if (data instanceof VotingSimulationData) {
            this.simulationData = (VotingSimulationData)data;
            addErrorList(data.getErrorsList());
            try {
                if(nextEventState != null) {               
                    cancelEvent();
                    return;
                } 
                if(simulationData.getBackupRequestEmail() != null) {
                    requestBackup();
                    return;
                } 
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            countDownLatch.countDown();
        }
    }
    
    @Override public void processVotingSystemWorkerMsg(List<String> messages) {}
    
    
    private void inicializarBaseUsuarios(){
        final UserBaseDataSimulator creacionBaseUsuarios = 
            new  UserBaseDataSimulator(userBaseData, this);
        creacionBaseUsuarios.init();
    }
    
    private void publishEvent() {
        logger.debug("publishEvent");
        try {
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
            smimeDocument = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                    ContextoPruebas.INSTANCE.getAccessControl().getNombreNormalizado(), 
                    eventParaPublicar, subject, null);
            
            new SMIMESignedSenderWorker(Worker.PUBLISH_DOCUMENT, smimeDocument, 
                    ContextoPruebas.INSTANCE. getURLGuardarEventoParaVotar(), 
                    null, null, this).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            countDownLatch.countDown();
        }
    }
    
    private void associateControlCenter() {
        try {
            String documentoAsociacion = ActorConIP.getAssociationDocumentJSON(
                    simulationData.getControlCenterURL()).toString();
            String msgSubject = ContextoPruebas.INSTANCE.getString(
                "associateControlCenterMsgSubject");
            smimeDocument = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                    ContextoPruebas.INSTANCE.getAccessControl().getNombreNormalizado(), 
                    documentoAsociacion, msgSubject, null);
   
            new SMIMESignedSenderWorker(Worker.ASSOCIATE_CONTROL_CENTER, 
                    smimeDocument, ContextoPruebas.INSTANCE.getURLAsociarActorConIP(), 
                    null, null, this).execute();
            
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            countDownLatch.countDown();
        }
    }
    
    @Override public VotingSimulationData getData() {
        return simulationData;
    }

    @Override public void finish() throws Exception {
        if(simulationListener != null) {           
            simulationListener.setSimulationResult(simulationData);
        } else {
            logger.debug("--------------- SIMULATION RESULT------------------");
            logger.info("Duration: " + simulationData.getDurationStr());
            logger.info("Number of requests projected: " + simulationData.getNumOfElectors());
            logger.info("Number access request ERRORs: " + simulationData.getNumAccessRequestsERROR());
            logger.info("Number of vote requests: " + simulationData.getNumVotingRequests());
            logger.info("Number of votes OK: " +  simulationData.getNumVotingRequestsOK());
            logger.info("Number of votes ERROR: " + simulationData.getNumVotingRequestsERROR());    
            String errorsMsg = getFormattedErrorList();
            if(errorsMsg != null) {
                logger.info(" ************* " + getErrorsList().size() + " ERRORS: \n" + 
                            errorsMsg);
            }
            logger.debug("------------------- FINISHED --------------------------");
            System.exit(0);
        }
    }
    

    @Override public void updateSimulationData(SimulationData data) {
        if(data instanceof VotingSimulationData) {
            VotingSimulationData simulData = (VotingSimulationData)data;
            logger.debug("updateSimulationData - status: " + simulData.getStatusCode());
            if(Respuesta.SC_OK == simulData.getStatusCode()) {
            } else {
            }
        } else {
            
        }
    }

    @Override public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
                " - worker: " + worker.getType());
        String msg = null;
        switch((Worker)worker.getType()) {
            case ACCESS_CONTROL_GETTER:           
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP accessControl = ActorConIP.parse(worker.getMessage());
                        msg = SimulationUtils.checkActor(
                                accessControl, ActorConIP.Tipo.CONTROL_ACCESO);
                        if(msg == null) {
                            ContextoPruebas.INSTANCE.setControlAcceso(accessControl);
                            //we need to add test Authority Cert to system in order
                            //to validate signatures
                            byte[] rootCACertPEMBytes = CertUtil.fromX509CertToPEM (
                                ContextoPruebas.INSTANCE.getRootCACert());
                            String rootCAServiceURL = ContextoPruebas.INSTANCE.
                                    getAccessControlRootCAServiceURL();
                            new DocumentSenderWorker(Worker.ACCESSCONTROL_CA_CERT_INITIALIZER, 
                                rootCACertPEMBytes, null, rootCAServiceURL, this).execute();
                            return;
                        }
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                }
                break;
            case ACCESSCONTROL_CA_CERT_INITIALIZER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
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
                        new InfoGetterWorker(Worker.CONTROL_CENTER_GETTER, 
                                urlInfoServidor, null, this).execute();
                        return;
                    }
                }
                break; 
            case ASSOCIATE_CONTROL_CENTER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP controlCenter = ActorConIP.parse(worker.getMessage());
                        msg = SimulationUtils.checkActor(
                                controlCenter, ActorConIP.Tipo.CENTRO_CONTROL);
                        if(msg == null) {
                            publishEvent();
                            return;
                        } 
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                }
                break;                
            case PUBLISH_DOCUMENT:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        byte[] responseBytes = worker.getMessage().getBytes();
                        FileUtils.copyStreamToFile(new ByteArrayInputStream(responseBytes), 
                            new File(ContextoPruebas.DEFAULTS.APPDIR + "VotingPublishReceipt"));
                        SMIMEMessageWrapper mimeMessage = new SMIMEMessageWrapper(null, 
                                new ByteArrayInputStream(responseBytes), 
                                "VotingPublishReceipt");
                        mimeMessage.verify(
                                ContextoPruebas.INSTANCE.getSessionPKIXParameters());
                        logger.debug("--- mimeMessage.getSignedContent(): " 
                                + mimeMessage.getSignedContent());
                        event = Evento.parse(mimeMessage.getSignedContent());
                        simulationData.setEvento(event);
                        logger.debug("Respuesta - Evento ID: " + event.getEventoId() + 
                                " - url: " + event.getUrl());
                        inicializarBaseUsuarios();
                        return;
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                }
                break;
            case CONTROL_CENTER_GETTER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP controlCenter = ActorConIP.parse(worker.getMessage());
                        msg = SimulationUtils.checkActor(
                                controlCenter, ActorConIP.Tipo.CENTRO_CONTROL);
                        if(msg == null) {
                            ContextoPruebas.INSTANCE.setControlCenter(controlCenter);
                            //Loaded Access Control and Control Center. Now we can publish  
                            publishEvent();
                            return;
                        }
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                }
                break;
            case CONTROLCENTER_CA_CERT_INITIALIZER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        String cancelDataStr = event.getCancelEventJSON(
                                Contexto.INSTANCE.getAccessControl().getServerURL(), 
                                nextEventState).toString();
                        String msgSubject = ContextoPruebas.INSTANCE.getString(
                            "cancelEventMsgSubject") + event.getEventoId();     
                        SMIMEMessageWrapper cancelSmimeDocument = 
                                signedMailGenerator.genMimeMessage(
                                ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                                ContextoPruebas.INSTANCE.getAccessControl().
                                getNombreNormalizado(), cancelDataStr, msgSubject, null);
                        new SMIMESignedSenderWorker(Worker.CANCEL_EVENT, 
                                cancelSmimeDocument, ContextoPruebas.INSTANCE.
                                getCancelEventURL(), null, null, this).execute();
                        return;
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                }
                break;
            case CANCEL_EVENT:
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
        }
        if(msg == null) msg = worker.getErrorMessage();
        else msg = worker.getErrorMessage() + " - msg: " + msg; 
        logger.error(msg);
        addErrorMsg(msg);
        countDownLatch.countDown();
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
        simuHelper.init();
    }
}
