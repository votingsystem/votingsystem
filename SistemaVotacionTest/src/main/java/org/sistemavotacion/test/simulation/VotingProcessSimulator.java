package org.sistemavotacion.test.simulation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import javax.mail.internet.MimeMessage;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.json.DeJSONAObjeto;
import org.sistemavotacion.test.json.DeObjetoAJSON;
import org.sistemavotacion.test.modelo.SimulationData;
import org.sistemavotacion.test.modelo.UserBaseData;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.InfoGetterWorker;
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
public class VotingProcessSimulator implements SimulatorListener, 
        VotingSystemWorkerListener{
    
    private static Logger logger = LoggerFactory.getLogger(VotingProcessSimulator.class);
    
    public enum Simulation {VOTING, ACCESS_REQUEST}
    
    private static final int ACCESS_CONTROL_GETTER_WORKER          = 0;
    private static final int CONTROL_CENTER_GETTER_WORKER          = 1;
    private static final int CA_CERT_INITIALIZER                   = 2;    
    private static final int PUBLISH_DOCUMENT_WORKER               = 3;
    private static final int ASSOCIATE_CONTROL_CENTER_WORKER       = 4;

    private static Simulation simulation = Simulation.VOTING;
    private Evento evento = null;
    private UserBaseData userBaseData = null;
    private SimulationData simulationData = null;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    
    public VotingProcessSimulator(SimulationData simulationData) {
        this.simulationData = simulationData;
        this.userBaseData = simulationData.getUserBaseData();
        try {
            ContextoPruebas.inicializar();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }      
    }
    
    public void init() {
        try {
            setupAccesControl(simulationData.getAccessControlURL());
            countDownLatch.await();
            logger.debug("--------------- FINISHED --------------------------");
            System.exit(0);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    @Override
    public void setSimulationMessage(String message) { }

    @Override
    public void setSimulationErrorMessage(String message) {  }

    @Override
    public void setSimulationResult(Simulator simulator, Object data) {
        logger.debug("data: " + data + " - simulator: " + simulator.getClass());
        if(simulator instanceof UserBaseDataSimulator) {
            UserBaseData ubd = (UserBaseData)data;
            ubd.setEvento(evento);
            simulationData.setUserBaseData(ubd);
            switch(simulation) {
                case ACCESS_REQUEST:
                    AccessRequestSimulator accessRequest = new AccessRequestSimulator(simulationData, this);
                    accessRequest.init();
                    break;
                case VOTING:
                    VotingSimulator votacion = new VotingSimulator(simulationData, this);
                    votacion.init();
                    break;
            }

        } else if (simulator instanceof VotingSimulator) {
            logger.debug("data: " + data);
            countDownLatch.countDown();
        }
    }

    @Override
    public void process(List<String> messages) { }
    
    
    private void inicializarBaseUsuarios(){
        final UserBaseDataSimulator creacionBaseUsuarios = 
            new  UserBaseDataSimulator(userBaseData, this);
        creacionBaseUsuarios.lanzar();
    }
    
    private void setupAccesControl(String controlAccessServerURL){
        logger.debug("setupAccesControl");
        String urlServidor = StringUtils.prepararURL(controlAccessServerURL);
        String urlInfoServidor = ContextoPruebas.getURLInfoServidor(urlServidor);
        new InfoGetterWorker(ACCESS_CONTROL_GETTER_WORKER, urlInfoServidor, 
                null, this).execute();
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
            
            Calendar today = Calendar.getInstance();
            Calendar dateInit = Calendar.getInstance();
            dateInit.add(Calendar.DATE, - 1);
            Calendar dateFinish  = Calendar.getInstance();
            dateFinish.add(Calendar.DATE,  30);
            
            evento = new Evento();
            evento.setAsunto("Asunto " + dateStr);
            evento.setContenido("Contenido " + dateStr);
            evento.setFechaInicio(today.getTime());
            evento.setFechaFin(dateFinish.getTime());
  
            List<OpcionEvento> opcionesDeEvento = new ArrayList<OpcionEvento>();
            OpcionEvento opcionDeEvento1 = new OpcionEvento();
            opcionDeEvento1.setContenido("Si");
            opcionesDeEvento.add(opcionDeEvento1);
            OpcionEvento opcionDeEvento2 = new OpcionEvento();
            opcionDeEvento2.setContenido("No");
            opcionesDeEvento.add(opcionDeEvento2);            

            evento.setOpciones(opcionesDeEvento);
            
            evento.setCentroControl(ContextoPruebas.getCentroControl());
            SignedMailGenerator signedMailGenerator =  new SignedMailGenerator(
                ContextoPruebas.getUsuarioPruebas().getKeyStore(),
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.VOTE_SIGN_MECHANISM);
            File eventToPublish = File.createTempFile("eventToPublish", ".json"); 
            eventToPublish.deleteOnExit();
            logger.debug("publishing event: " + eventToPublish.getAbsolutePath());
            String eventoParaPublicar = DeObjetoAJSON.obtenerEventoJSON(evento);
            MimeMessage mimeMessage = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.getUsuarioPruebas().getEmail(), 
                    ContextoPruebas.getControlAcceso().getNombreNormalizado(), 
                    eventoParaPublicar, "Solicitud Publicación convocatoria",
                    null);
            mimeMessage.writeTo(new FileOutputStream(eventToPublish));
            new DocumentSenderWorker(
                    PUBLISH_DOCUMENT_WORKER, eventToPublish, 
                    Contexto.SIGNED_CONTENT_TYPE,
                    ContextoPruebas.getURLGuardarEventoParaVotar(
                    ContextoPruebas.getControlAcceso().getServerURL()), this).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            countDownLatch.countDown();
        }
    }
    
    private void associateControlCenter() {
        try {
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    ContextoPruebas.getUsuarioPruebas().getKeyStore(),
                    ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                    ContextoPruebas.VOTE_SIGN_MECHANISM);
            File solicitudAsociacion = File.createTempFile(
                    "SolicitudAsociacion", ".p7s");
            solicitudAsociacion.deleteOnExit();
            String documentoAsociacion = DeObjetoAJSON.obtenerDocumentoAsociacionJSON(
            		simulationData.getControlCenterURL());
            MimeMessage mimeMessage = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.getUsuarioPruebas().getEmail(), 
                    ContextoPruebas.getControlAcceso().getNombreNormalizado(), 
                    documentoAsociacion, "Solicitud Asociacion de Centro de Control", null);
            mimeMessage.writeTo(new FileOutputStream(solicitudAsociacion));
            new DocumentSenderWorker(
                    ASSOCIATE_CONTROL_CENTER_WORKER, solicitudAsociacion, 
                    Contexto.SIGNED_CONTENT_TYPE,
                    ContextoPruebas.getURLAsociarActorConIP(
                    ContextoPruebas.getControlAcceso().getServerURL()), this).execute();
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
    public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
                " - worker: " + worker.getClass().getSimpleName() + 
                " - workerId:" + worker.getId());
        switch(worker.getId()) {
            case ACCESS_CONTROL_GETTER_WORKER:           
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP controlAcceso = DeJSONAObjeto.obtenerActorConIP(worker.getMessage());
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
                    String mensaje = "Error - " + worker.getMessage();
                    logger.error("### ERROR - " + mensaje);
                    countDownLatch.countDown();
                }
                break;
            case CA_CERT_INITIALIZER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    Set<ActorConIP> controlCenters = ContextoPruebas.getControlAcceso().getCentrosDeControl();        
                    if(controlCenters == null || controlCenters.isEmpty()) {
                        associateControlCenter();
                    } else {
                        setupControlCenter(controlCenters.iterator().
                                next().getServerURL());
                    }
                } else {
                    String mensaje = "Error añadiendo Autoridad Certificadora de pruebas - " + 
                                    worker.getMessage();
                    logger.error("### ERROR - " + mensaje);
                    countDownLatch.countDown();
                } 
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
                                ContextoPruebas.INSTANCIA.getSessionPKIXParameters());
                        logger.debug("--- mimeMessage.getSignedContent(): " 
                                + mimeMessage.getSignedContent());
                        evento = DeJSONAObjeto.obtenerEvento(mimeMessage.getSignedContent());
                        logger.debug("Respuesta - Evento ID: " + evento.getEventoId());
                        inicializarBaseUsuarios();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        countDownLatch.countDown();
                    }
                } else {
                    logger.error("ERROR - " + worker.getMessage());
                    countDownLatch.countDown();
                }
                break;
            case ASSOCIATE_CONTROL_CENTER_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP actorConIP = DeJSONAObjeto.obtenerActorConIP(worker.getMessage());
                        if(ActorConIP.Tipo.CENTRO_CONTROL == actorConIP.getTipo()) {
                            ContextoPruebas.setCentroControl(actorConIP);
                            //Loaded Access Control and Control Center. Now we can publish  
                            publishEvent();
                            return;
                        } else {
                            logger.error("### ERROR - El servidor no es un Centro Control");
                        }
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                } else {
                    logger.error("ERROR ASSOCIATING CONTROL CENTER - " + worker.getMessage());
                }
                countDownLatch.countDown();
                break;
            case CONTROL_CENTER_GETTER_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        ActorConIP actorConIP = DeJSONAObjeto.obtenerActorConIP(worker.getMessage());
                        if(ActorConIP.Tipo.CENTRO_CONTROL == actorConIP.getTipo()) {
                            ContextoPruebas.setCentroControl(actorConIP);
                            //Loaded Access Control and Control Center. Now we can publish  
                            publishEvent();
                            return;
                        } else {
                            logger.error("### ERROR - El servidor no es un Centro Control");
                        }
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                } else {
                    String mensaje = "Error Cargando centro Control <br/>" + worker.getMessage();
                    logger.error("### ERROR - " + mensaje);
                }
                associateControlCenter();
                break;
        }
    }
    
    public static void generate (String operacionStr) throws Exception {
        logger.debug("- generate: '" + operacionStr + "'");
        if(operacionStr == null) return;
        JSON datosJSON = JSONSerializer.toJSON(operacionStr);
        if(JSONNull.getInstance().equals(datosJSON)) return;
        JSONObject operacionJSON = null;
        if(datosJSON instanceof JSONArray) {
            operacionJSON = ((JSONArray)datosJSON).getJSONObject(0);
        } else operacionJSON = (JSONObject)datosJSON;
    }
    
    
    public static void main(String[] args) throws Exception {
        SimulationData simulationData = null;
        if(args != null && args.length > 0) {
            logger.debug("args[0]");
            simulationData = SimulationData.parse(args[0]);
            if(args[1] != null) simulation = Simulation.valueOf(args[1]);
        } else {
            simulation = Simulation.VOTING;
            File jsonFile = File.createTempFile("VotingProcessSimulation", ".json");
            jsonFile.deleteOnExit();
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("simulatorFiles/votingSimulationData.json"), jsonFile); 
            simulationData = SimulationData.parse(FileUtils.getStringFromFile(jsonFile));
            logger.debug("Simulation for Access Control: " + simulationData.getAccessControlURL());
            
        }
        VotingProcessSimulator simuHelper = new VotingProcessSimulator(simulationData);
        simuHelper.init();
    }
}
