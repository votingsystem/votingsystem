package org.sistemavotacion.test.simulation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.VotingSimulationData;
import org.sistemavotacion.test.modelo.UserBaseSimulationData;
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
* 5)- When finishes user base data initilization, 'setSimulationResult' inits simulation 
*/
public class VotingProcessSimulator extends  Simulator<VotingSimulationData>  
        implements VotingSystemWorkerListener, SimulatorListener {
    
    private static Logger logger = LoggerFactory.getLogger(VotingProcessSimulator.class);

    public enum Simulation {VOTING, ACCESS_REQUEST}
    
    private static final int ACCESS_CONTROL_GETTER_WORKER                      = 0;
    private static final int CONTROL_CENTER_GETTER_WORKER                      = 1;
    private static final int CA_CERT_INITIALIZER                               = 2;
    private static final int TIME_STAMP_ASSOCIATE_CONTROL_CENTER_GETTER_WORKER = 3;
    private static final int TIME_STAMP_PUBLISH_WORKER                         = 4;
    private static final int PUBLISH_DOCUMENT_WORKER                           = 5;
    private static final int ASSOCIATE_CONTROL_CENTER_WORKER                   = 6;

    private static Simulation simulation = Simulation.VOTING;
    private Evento evento = null;
    private UserBaseSimulationData userBaseData = null;
    private VotingSimulationData simulationData = null;
        
    private SMIMEMessageWrapper smimeDocument;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    
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
    
    @Override public void process(List<String> messages) { }
    
    
    private void inicializarBaseUsuarios(){
        final UserBaseDataSimulator creacionBaseUsuarios = 
            new  UserBaseDataSimulator(userBaseData, this);
        creacionBaseUsuarios.init();
    }
    
    private void setupControlCenter(String urlCentroControl){
        logger.debug("setupControlCenter");
        String urlServidor = StringUtils.prepararURL(urlCentroControl);
        String urlInfoServidor = ContextoPruebas.getURLInfoServidor(urlServidor);
        new InfoGetterWorker(CONTROL_CENTER_GETTER_WORKER, urlInfoServidor, 
                null, this).execute();
    }
    
    private void publishEvent() {
        logger.debug("publishEvent");
        try {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            String dateStr = formatter.format(date);

            
            evento = new Evento();
            evento.setAsunto("Asunto " + dateStr);
            evento.setContenido("Contenido " + dateStr);
            evento.setFechaInicio(simulationData.getDateBeginDocument());
            evento.setFechaFin(simulationData.getDateFinishDocument());
  
            List<OpcionEvento> opcionesDeEvento = new ArrayList<OpcionEvento>();
            OpcionEvento opcionDeEvento1 = new OpcionEvento();
            opcionDeEvento1.setContenido("Si");
            opcionesDeEvento.add(opcionDeEvento1);
            OpcionEvento opcionDeEvento2 = new OpcionEvento();
            opcionDeEvento2.setContenido("No");
            opcionesDeEvento.add(opcionDeEvento2);            

            evento.setOpciones(opcionesDeEvento);
            
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
                    ContextoPruebas.INSTANCE.getControlAcceso().getNombreNormalizado(), 
                    eventoParaPublicar, subject, null);
            
            new TimeStampWorker(TIME_STAMP_PUBLISH_WORKER, 
                    ContextoPruebas.INSTANCE.getUrlTimeStampServer(),
                    this, smimeDocument.getTimeStampRequest(),
                    ContextoPruebas.INSTANCE.getControlAcceso().getTimeStampCert()).execute();

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
            smimeDocument = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                    ContextoPruebas.INSTANCE.getControlAcceso().getNombreNormalizado(), 
                    documentoAsociacion, "Solicitud Asociacion de Centro de Control", null);
   
            new TimeStampWorker(TIME_STAMP_ASSOCIATE_CONTROL_CENTER_GETTER_WORKER, 
                    ContextoPruebas.INSTANCE.getUrlTimeStampServer(),
                    this, smimeDocument.getTimeStampRequest(),
                    ContextoPruebas.INSTANCE.getControlAcceso().getTimeStampCert()).execute();
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
            logger.info("Number of requests projected: " + simulationData.getNumberOfElectors());
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
    
    private String checkActor(ActorConIP actorConIP, ActorConIP.Tipo tipo) {
        String result = null;
        if(tipo != actorConIP.getTipo()) return "SERVER IS NOT " + tipo.toString();
        if(ActorConIP.EnvironmentMode.TEST != actorConIP.getEnvironmentMode()) {
            return "SERVER NOT IN TEST MODE. Server mode:" + actorConIP.getEnvironmentMode();
        }
        return null;
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
                        msg = checkActor(accessControl, ActorConIP.Tipo.CONTROL_ACCESO);
                        if(msg != null) {
                            addErrorMsg(msg);
                            logger.error(msg);
                            countDownLatch.countDown();
                            return;
                        }
                        ContextoPruebas.INSTANCE.setControlAcceso(accessControl);
                        //we need to add test Authority Cert to system in order
                        //to validate signatures
                        byte[] rootCACertPEMBytes = CertUtil.fromX509CertToPEM (
                            ContextoPruebas.INSTANCE.getRootCACert());
                        String rootCAServiceURL = ContextoPruebas.INSTANCE.
                                getRootCAServiceURL();
                        new DocumentSenderWorker(CA_CERT_INITIALIZER, 
                            rootCACertPEMBytes, null, rootCAServiceURL, this).execute();
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = "Error GETTING ACCESS CONTROL - " + ex.getMessage();
                        addErrorMsg(msg);
                        countDownLatch.countDown();
                    }
                }else {
                    msg = "Error GETTING ACCESS CONTROL - " + worker.getMessage();
                    addErrorMsg(msg);
                    countDownLatch.countDown();
                }
                break;
            case CA_CERT_INITIALIZER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    Set<ActorConIP> controlCenters = ContextoPruebas.INSTANCE.
                            getControlAcceso().getCentrosDeControl();        
                    if(controlCenters == null || controlCenters.isEmpty()) {
                        associateControlCenter();
                    } else {
                        setupControlCenter(controlCenters.iterator().
                                next().getServerURL());
                    }
                } else {
                    msg = "Error añadiendo Autoridad Certificadora de pruebas - " + 
                                    worker.getMessage();
                    addErrorMsg(msg);
                    countDownLatch.countDown();
                } 
                break;    
            case TIME_STAMP_ASSOCIATE_CONTROL_CENTER_GETTER_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        smimeDocument.setTimeStampToken((TimeStampWorker)worker);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        smimeDocument.writeTo(baos);
                        
                        new DocumentSenderWorker(ASSOCIATE_CONTROL_CENTER_WORKER, 
                                baos.toByteArray(), Contexto.SIGNED_CONTENT_TYPE,
                                ContextoPruebas.getURLAsociarActorConIP(
                                ContextoPruebas.INSTANCE.getControlAcceso().getServerURL()), this).execute();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        addErrorMsg("ERROR WITH ASSOCIATION DOCUMENT TIME STAMP " 
                                + ex.getMessage());
                        countDownLatch.countDown();
                    }
                } else {
                    logger.error("### ERROR - " + worker.getMessage());
                    addErrorMsg("ERROR WITH ASSOCIATION DOCUMENT TIME STAMP " 
                                + worker.getMessage());
                    countDownLatch.countDown();
                }
                
                break;
            case ASSOCIATE_CONTROL_CENTER_WORKER:
                
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP controlCenter = ActorConIP.parse(worker.getMessage());
                        msg = checkActor(controlCenter, ActorConIP.Tipo.CENTRO_CONTROL);
                        if(msg == null) {
                            publishEvent();
                            return;
                        } 
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = "ERROR ASSOCIATING CONTROL CENTER - " + ex.getMessage();
                    }
                } else msg = "ERROR ASSOCIATING CONTROL CENTER - " + worker.getMessage();
                logger.error(msg);
                addErrorMsg(msg);
                countDownLatch.countDown();
                break;                
            case TIME_STAMP_PUBLISH_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        smimeDocument.setTimeStampToken((TimeStampWorker)worker);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        smimeDocument.writeTo(baos);
                        new DocumentSenderWorker(PUBLISH_DOCUMENT_WORKER, 
                                baos.toByteArray(), Contexto.SIGNED_CONTENT_TYPE,
                                ContextoPruebas.INSTANCE.getURLGuardarEventoParaVotar(), this).execute();
                        return;
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = "ERROR WITH PUBLISH DOCUMENT TIME STAMP " 
                                + ex.getMessage();
                    }
                } else msg = "ERROR WITH PUBLISH DOCUMENT TIME STAMP "  + worker.getMessage();
                logger.error(msg);
                addErrorMsg(msg);
                countDownLatch.countDown();
                break;
            case PUBLISH_DOCUMENT_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        byte[] responseBytes = worker.getMessage().getBytes();
                        FileUtils.copyStreamToFile(new ByteArrayInputStream(responseBytes), 
                            new File(ContextoPruebas.APPDIR + "VotingPublishReceipt"));
                        SMIMEMessageWrapper mimeMessage = new SMIMEMessageWrapper(null, 
                                new ByteArrayInputStream(responseBytes), 
                                "VotingPublishReceipt");
                        mimeMessage.verify(
                                ContextoPruebas.INSTANCE.getSessionPKIXParameters());
                        logger.debug("--- mimeMessage.getSignedContent(): " 
                                + mimeMessage.getSignedContent());
                        evento = Evento.parse(mimeMessage.getSignedContent());
                        simulationData.setEvento(evento);
                        logger.debug("Respuesta - Evento ID: " + evento.getEventoId());
                        inicializarBaseUsuarios();
                        return;
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = "ERROR PUBLISHING DOCUMENT " + ex.getMessage();
                    }
                } else msg = "ERROR PUBLISHING DOCUMENT " + worker.getMessage();
                logger.error(msg);
                addErrorMsg(msg);
                countDownLatch.countDown();
                break;
            case CONTROL_CENTER_GETTER_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP controlCenter = ActorConIP.parse(worker.getMessage());
                        msg = checkActor(controlCenter, ActorConIP.Tipo.CENTRO_CONTROL);
                        if(msg == null) {
                            ContextoPruebas.INSTANCE.setCentroControl(controlCenter);
                            //Loaded Access Control and Control Center. Now we can publish  
                            publishEvent();
                            return;
                        }
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        msg = "PROBLEM GETTING CONTROL CENTER:" +  ex.getMessage();
                    }
                } else msg = "PROBLEM GETTING CONTROL CENTER " + worker.getMessage();
                logger.error(msg);
                addErrorMsg(msg);
                countDownLatch.countDown();
                break;
        }
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
