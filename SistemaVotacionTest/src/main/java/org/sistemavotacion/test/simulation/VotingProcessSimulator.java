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
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.VotingSimulationData;
import org.sistemavotacion.test.modelo.UserBaseSimulationData;
import org.sistemavotacion.test.util.SimulationUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.sistemavotacion.worker.SMIMESignedSenderWorker;
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
* 5)- When finishes user base data initilization, 'setSimulationResult' inits simulation 
*/
public class VotingProcessSimulator extends  Simulator<VotingSimulationData>  
        implements VotingSystemWorkerListener, SimulatorListener {
    
    private static Logger logger = LoggerFactory.getLogger(VotingProcessSimulator.class);

    public enum Simulation {VOTING, ACCESS_REQUEST}
    
    private static final int ACCESS_CONTROL_GETTER_WORKER     = 0;
    private static final int CONTROL_CENTER_GETTER_WORKER     = 1;
    private static final int CA_CERT_INITIALIZER              = 2;
    private static final int PUBLISH_DOCUMENT_WORKER          = 3;
    private static final int ASSOCIATE_CONTROL_CENTER_WORKER  = 4;

    private static Simulation simulation = Simulation.VOTING;
    private Evento evento = null;
    private UserBaseSimulationData userBaseData = null;
    private VotingSimulationData simulationData = null;
        
    private SMIMEMessageWrapper smimeDocument;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    private SimulatorListener simulationListener;
    
    public VotingProcessSimulator(VotingSimulationData simulationData,
             SimulatorListener simulationListener) {
        super(simulationData);
        this.simulationData = simulationData;
        this.userBaseData = simulationData.getUserBaseData();   
        this.simulationListener = simulationListener;
    }
    
    @Override public void init() throws Exception {
        try {
            String urlInfoServidor = ContextoPruebas.getURLInfoServidor(
                    StringUtils.prepararURL(simulationData.getAccessControlURL()));
            new InfoGetterWorker(ACCESS_CONTROL_GETTER_WORKER, urlInfoServidor, 
                null, this).execute();
            countDownLatch.await();
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }
        finish();
    }


    @Override public void setSimulationResult(Simulator simulator) {
        logger.debug("Getting response from simulator: " + 
                simulator.getClass().getSimpleName());
        if(simulator instanceof UserBaseDataSimulator) {
            UserBaseSimulationData ubd = ((UserBaseDataSimulator)simulator).getData();
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

        } else if ((simulator instanceof VotingSimulator) ||
                (simulator instanceof AccessRequestSimulator)) {
            this.simulationData = (VotingSimulationData)simulator.getData();
            addErrorList(simulator.getErrorsList());
            try {
                finish();
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
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
            evento = simulationData.getEvento();
            String subjectDoc = evento.getAsunto()+ " -> " + dateStr;
            evento.setAsunto(subjectDoc);    
            evento.setTipo(Tipo.VOTACION);
            evento.setCentroControl(ContextoPruebas.INSTANCE.getCentroControl());
            SignedMailGenerator signedMailGenerator =  new SignedMailGenerator(
                ContextoPruebas.INSTANCE.getUserTest().getKeyStore(),
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.VOTE_SIGN_MECHANISM);
            String eventoParaPublicar = evento.toJSON().toString();
            String subject = ContextoPruebas.INSTANCE.getString("votingPublishMsgSubject");
            smimeDocument = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                    ContextoPruebas.INSTANCE.getAccessControl().getNombreNormalizado(), 
                    eventoParaPublicar, subject, null);
            
            new SMIMESignedSenderWorker(PUBLISH_DOCUMENT_WORKER, smimeDocument, 
                    ContextoPruebas.INSTANCE. getURLGuardarEventoParaVotar(), 
                    null, null, this).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            countDownLatch.countDown();
        }
    }
    
    private void associateControlCenter() {
        try {
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    ContextoPruebas.INSTANCE.getUserTest().getKeyStore(),
                    ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.PASSWORD.toCharArray(),
                    ContextoPruebas.VOTE_SIGN_MECHANISM);
            String documentoAsociacion = ActorConIP.getAssociationDocumentJSON(
                    simulationData.getControlCenterURL()).toString();
            String msgSubject = ContextoPruebas.INSTANCE.getString(
                "associateControlCenterMsgSubject");
            smimeDocument = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                    ContextoPruebas.INSTANCE.getAccessControl().getNombreNormalizado(), 
                    documentoAsociacion, msgSubject, null);
   
            new SMIMESignedSenderWorker(ASSOCIATE_CONTROL_CENTER_WORKER, 
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

    @Override public VotingSimulationData finish() throws Exception {
        if(simulationListener != null) {           
            simulationListener.setSimulationResult(this);
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
        return simulationData;
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
                " - worker: " + worker.getClass().getSimpleName() + 
                " - workerId:" + worker.getId());
        String msg = null;
        switch(worker.getId()) {
            case ACCESS_CONTROL_GETTER_WORKER:           
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
                                    getRootCAServiceURL();
                            new DocumentSenderWorker(CA_CERT_INITIALIZER, 
                                rootCACertPEMBytes, null, rootCAServiceURL, this).execute();
                            return;
                        }
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                }else msg = worker.getMessage();
                msg = "###ERROR ACCESS_CONTROL_GETTER_WORKER:" + msg;
                break;
            case CA_CERT_INITIALIZER:
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
                        new InfoGetterWorker(CONTROL_CENTER_GETTER_WORKER, 
                                urlInfoServidor, null, this).execute();
                        return;
                    }
                } else msg = worker.getMessage();
                msg = "###ERROR CA_CERT_INITIALIZER:" + msg;
                break;  
            case ASSOCIATE_CONTROL_CENTER_WORKER:
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
                } else msg = worker.getMessage();
                msg = "###ERROR ASSOCIATE_CONTROL_CENTER_WORKER" + msg;
                break;                
            case PUBLISH_DOCUMENT_WORKER:
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
                        evento = Evento.parse(mimeMessage.getSignedContent());
                        simulationData.setEvento(evento);
                        logger.debug("Respuesta - Evento ID: " + evento.getEventoId() + 
                                " - url: " + evento.getUrl());
                        inicializarBaseUsuarios();
                        return;
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                } else msg = worker.getMessage();
                msg = "### ERROR PUBLISH_DOCUMENT_WORKER: " + msg;
                break;
            case CONTROL_CENTER_GETTER_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP controlCenter = ActorConIP.parse(worker.getMessage());
                        msg = SimulationUtils.checkActor(
                                controlCenter, ActorConIP.Tipo.CENTRO_CONTROL);
                        if(msg == null) {
                            ContextoPruebas.INSTANCE.setCentroControl(controlCenter);
                            //Loaded Access Control and Control Center. Now we can publish  
                            publishEvent();
                            return;
                        }
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = ex.getMessage();
                    }
                } else msg = worker.getMessage();
                msg = "### ERROR CONTROL_CENTER_GETTER_WORKER:" + msg;
                break;
        }
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
