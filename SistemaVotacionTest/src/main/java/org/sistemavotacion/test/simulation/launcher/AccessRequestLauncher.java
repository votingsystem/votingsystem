package org.sistemavotacion.test.simulation.launcher;

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
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta; 
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.json.DeObjetoAJSON;
import org.sistemavotacion.test.modelo.InfoVoto;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sistemavotacion.worker.AccessRequestLauncherWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestLauncher 
    implements Callable<InfoVoto>, VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(AccessRequestLauncher.class);

    private static final int ACCESS_REQUEST_WORKER    = 1;
    
    private InfoVoto infoVoto;
    private SMIMEMessageWrapper documentSMIME;
    private PKCS10WrapperClient wrapperClient;
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time

    private String urlTimeStampServer = null;
    private String urlAccessRequest = null;
        
    public AccessRequestLauncher (InfoVoto infoVoto) 
            throws Exception {
        this.infoVoto = infoVoto; 
        urlTimeStampServer = ContextoPruebas.getUrlTimeStampServer(
                ContextoPruebas.getControlAcceso().getServerURL());
        urlAccessRequest = ContextoPruebas.getURLAccessRequest(
                ContextoPruebas.getControlAcceso().getServerURL());
        infoVoto.getVoto().setUrlSolicitudAcceso(urlAccessRequest);
    }
    
    private InfoVoto getInfoVoto() {
        return infoVoto;
    }
    
    SignedMailGenerator signedMailGenerator;
    
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

        
        signedMailGenerator = new SignedMailGenerator(mockDnieBytes, 
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        documentSMIME = signedMailGenerator.genMimeMessage(infoVoto.getFrom(), 
                infoVoto.getVoto().getControlAcceso().getNombreNormalizado(), 
                DeObjetoAJSON.obtenerSolicitudAccesoJSON(
                    ContextoPruebas.getControlAcceso().getServerURL(),infoVoto.getVoto()),
                asuntoMensaje, null);
        //mimeMessage.writeTo(new FileOutputStream(solicitudAcceso));
        X509Certificate accesRequestCert = ContextoPruebas.
                        getControlAcceso().getCertificate();
        infoVoto.getVoto().setUrlRecolectorVotosCentroControl(anuladorVotoStr);
        Usuario usuario = new Usuario(infoVoto.getFrom());
        new AccessRequestLauncherWorker(ACCESS_REQUEST_WORKER, 
                documentSMIME, infoVoto.getVoto(), accesRequestCert, this).execute();
        
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
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        infoVoto.setCodigoEstado(Respuesta.SC_ERROR_EJECUCION);
                        infoVoto.setError(InfoVoto.Error.ACCESS_REQUEST);
                        infoVoto.setMensaje(ex.getMessage());
                        
                    }
                    
                } else {
                    String msg = "ERROR enviando solicitud de acceso " + worker.getMessage();
                    logger.error(msg);
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