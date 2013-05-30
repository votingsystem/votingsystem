package org.sistemavotacion.test.simulacion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import static org.sistemavotacion.Contexto.TIMESTAMP_DNIe_HASH;
import static org.sistemavotacion.Contexto.TIMESTAMP_VOTE_HASH;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.json.DeObjetoAJSON;
import org.sistemavotacion.test.modelo.InfoVoto;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.sistemavotacion.test.ContextoPruebas.*;
import org.sistemavotacion.worker.AccessRequestLauncherWorker;
import org.sistemavotacion.worker.NotificarVotoWorker;
import org.sistemavotacion.worker.TimeStampWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class LanzadoraVoto 
    implements Callable<InfoVoto>, VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(LanzadoraVoto.class);

    private static final int TIMESTAMP_ACCESS_REQUEST = 0;
    private static final int TIMESTAMP_VOTE           = 1;
    private static final int ACCESS_REQUEST_WORKER    = 2;
    private static final int NOTIFICAR_VOTO_WORKER    = 3;
    
    private InfoVoto infoVoto;
    private SMIMEMessageWrapper documentSMIME;
    private PKCS10WrapperClient wrapperClient;
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time

    private String urlTimeStampServer = null;
    private String urlAccessRequest = null;
        
    public LanzadoraVoto (InfoVoto infoVoto) 
            throws Exception {
        this.infoVoto = infoVoto; 
        urlTimeStampServer = ContextoPruebas.getControlAcceso().getServerURL() + "/timeStamp";
        urlAccessRequest = ContextoPruebas.getControlAcceso().getServerURL() + "/solicitudAcceso";
        infoVoto.getVoto().setUrlSolicitudAcceso(urlAccessRequest);
    }
    
    private InfoVoto getInfoVoto() {
        return infoVoto;
    }
    
    
    @Override
    public InfoVoto call() throws Exception { 
        File mockDnieFile = new File(ContextoPruebas.getUserKeyStorePath(infoVoto.getFrom()));
        byte[] mockDnieBytes = FileUtils.getBytesFromFile(mockDnieFile);
        logger.info("userID: " + infoVoto.getFrom() + 
                " - mockDnieFile: " + mockDnieFile.getAbsolutePath());
        String asuntoMensaje = ContextoPruebas.ASUNTO_MENSAJE_SOLICITUD_ACCESO 
                        + infoVoto.getVoto().getEventoId();
        //Header header = new Header (Contexto.NOMBRE_ENCABEZADO_HASH, 
        //    infoVoto.getVoto().getHashCertificadoVotoBase64());
        String anuladorVotoStr = null;
        synchronized(this) {
             anuladorVotoStr = DeObjetoAJSON.obtenerAnuladorDeVotoJSON(infoVoto.getVoto());
        }
        File anuladorVoto = new File(ContextoPruebas.getUserDirPath(infoVoto.getFrom())
                + ContextoPruebas.ANULACION_FILE + infoVoto.getVoto().getEventoId() + 
                "_usu" + infoVoto.getFrom() + ".json");
        FileUtils.copyStreamToFile(new ByteArrayInputStream(
                anuladorVotoStr.getBytes()), anuladorVoto);
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnieBytes, 
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        documentSMIME = signedMailGenerator.genMimeMessage(infoVoto.getFrom(), 
                infoVoto.getVoto().getControlAcceso().getNombreNormalizado(), 
                DeObjetoAJSON.obtenerSolicitudAccesoJSON(
                    ContextoPruebas.getControlAcceso().getServerURL(),infoVoto.getVoto()),
                asuntoMensaje, null);
        //mimeMessage.writeTo(new FileOutputStream(solicitudAcceso));
        
        new TimeStampWorker(TIMESTAMP_ACCESS_REQUEST, urlTimeStampServer,
                    this, documentSMIME.getTimeStampRequest(TIMESTAMP_DNIe_HASH),
                    ContextoPruebas.getControlAcceso().getTimeStampCert()).execute();
        
        countDownLatch.await();
        return getInfoVoto();
    }
    
    @Override public void process(List<String> list) { }

    @Override public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
                " - worker: " + worker.getClass().getSimpleName() + 
                " - workerId:" + worker.getId());
        infoVoto.setCodigoEstado(worker.getStatusCode());
        switch(worker.getId()) {
            case TIMESTAMP_ACCESS_REQUEST:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        documentSMIME.setTimeStampToken((TimeStampWorker)worker);
                        X509Certificate accesRequestCert = ContextoPruebas.
                                getControlAcceso().getCertificate();
                        new AccessRequestLauncherWorker(ACCESS_REQUEST_WORKER, 
                                documentSMIME, infoVoto.getVoto(), 
                                accesRequestCert, this).execute();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        infoVoto.setCodigoEstado(Respuesta.SC_ERROR_EJECUCION);
                        infoVoto.setError(InfoVoto.Error.ACCESS_REQUEST);
                        infoVoto.setMensaje(ex.getMessage());
                        countDownLatch.countDown();
                    }
                } else {
                    String msg = "ERROR obteniendo sello de tiempo de solicitud" +
                            "de acceso " + worker.getMessage();
                    logger.error(msg);
                    infoVoto.setError(InfoVoto.Error.ACCESS_REQUEST);
                    infoVoto.setMensaje(msg);
                    countDownLatch.countDown();
                }
                break;
            case ACCESS_REQUEST_WORKER:
                if (Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        wrapperClient = ((AccessRequestLauncherWorker)worker).
                            getPKCS10WrapperClient();
                        String votoJSON = obtenerVotoParaEventoJSON(infoVoto.getVoto());
                        documentSMIME = wrapperClient.genMimeMessage(
                                infoVoto.getVoto().getHashCertificadoVotoBase64(), 
                                infoVoto.getVoto().getControlAcceso().getNombreNormalizado(),
                                votoJSON, "[VOTO]", null);
                        new TimeStampWorker(TIMESTAMP_VOTE, urlTimeStampServer,
                            this, documentSMIME.getTimeStampRequest(TIMESTAMP_VOTE_HASH),
                            ContextoPruebas.getControlAcceso().getTimeStampCert()).execute();
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        infoVoto.setCodigoEstado(Respuesta.SC_ERROR_EJECUCION);
                        infoVoto.setError(InfoVoto.Error.ACCESS_REQUEST);
                        infoVoto.setMensaje(ex.getMessage());
                        countDownLatch.countDown();
                    }
                } else {
                    String msg = "ERROR enviando solicitud de acceso " + worker.getMessage();
                    logger.error(msg);
                    infoVoto.setError(InfoVoto.Error.ACCESS_REQUEST);
                    infoVoto.setMensaje(msg);
                    countDownLatch.countDown();
                }
                break;                
            case TIMESTAMP_VOTE:
                if(Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        documentSMIME.setTimeStampToken((TimeStampWorker)worker);
                        X509Certificate serverCert = ContextoPruebas.getCentroControl().getCertificate();
                        String urlVoto = ContextoPruebas.getURLVoto(
                            infoVoto.getVoto().getCentroControl().getServerURL());            
                        new NotificarVotoWorker(NOTIFICAR_VOTO_WORKER, 
                                infoVoto.getVoto(), urlVoto, serverCert, documentSMIME, 
                                wrapperClient, this).execute();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        infoVoto.setCodigoEstado(Respuesta.SC_ERROR_EJECUCION);
                        infoVoto.setError(InfoVoto.Error.VOTE);
                        infoVoto.setMensaje(ex.getMessage());
                        countDownLatch.countDown();
                    }
                } else {
                    String msg = "showResult - ERROR TIMESTAMP_VOTE - " + worker.getMessage();
                    logger.debug(msg); 
                    infoVoto.setError(InfoVoto.Error.VOTE);
                    infoVoto.setMensaje(msg);
                    countDownLatch.countDown();
                }
                break;       
            case NOTIFICAR_VOTO_WORKER:
                if (Respuesta.SC_OK == worker.getStatusCode()) {   
                    ReciboVoto recibo = ((NotificarVotoWorker)worker).getReciboVoto();
                    infoVoto.setReciboVoto(recibo);
                    //VotacionHelper.addRecibo(infoVoto.getVoto().getHashCertificadoVotoBase64(), recibo);
                } else {
                    infoVoto.setMensaje(worker.getMessage());
                    infoVoto.setError(InfoVoto.Error.VOTE);
                } 
                countDownLatch.countDown();
                break;
            default:
                logger.debug("*** UNKNOWN WORKER ID: '" + worker.getId() + "'");
        }
    }
    
    public String obtenerVotoParaEventoJSON(Evento evento) {
        logger.debug("obtenerVotoParaEventoJSON");
        Map map = new HashMap();
        map.put("eventoURL", ContextoPruebas.getURLEventoParaVotar(
                ContextoPruebas.getControlAcceso().getServerURL(), 
                evento.getEventoId()));
        map.put("opcionSeleccionadaId", evento.getOpcionSeleccionada().getId());
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }
}