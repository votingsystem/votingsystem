package org.sistemavotacion.test.simulacion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.tsp.TimeStampRequest;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import static org.sistemavotacion.test.ContextoPruebas.TIMESTAMP_VOTE_HASH;
import org.sistemavotacion.test.modelo.InfoVoto;
import org.sistemavotacion.worker.TimeStampWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class LanzadoraVoto 
    implements Callable<InfoVoto>, VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(LanzadoraVoto.class);

    InfoVoto infoVoto;
    private SMIMEMessageWrapper timeStampedDocument;
    
    public LanzadoraVoto (InfoVoto infoVoto) throws Exception {
        this.infoVoto = infoVoto;
    }
    

    @Override
    public InfoVoto call() throws Exception {
        String votoJSON = obtenerVotoParaEventoJSON(infoVoto.getVoto());
        File votoFirmado = new File(ContextoPruebas.getUserDirPath(infoVoto.getFrom())
                        + ContextoPruebas.VOTE_FILE + infoVoto.getVoto().getEventoId() + ".p7m");
        votoFirmado = infoVoto.getPkcs10WrapperClient().genSignedFile(
                infoVoto.getVoto().getHashCertificadoVotoBase64(), 
                infoVoto.getVoto().getControlAcceso().getNombreNormalizado(),
                votoJSON, "[VOTO]", null, SignedMailGenerator.Type.USER, votoFirmado);
        String urlVoto = ContextoPruebas.getURLVoto(
                infoVoto.getVoto().getCentroControl().getServerURL());
        ReciboVoto reciboVoto = null;
        TimeStampWorker timeStampWorker = getTimeStampedDocument(votoFirmado);
        timeStampWorker.get(30, TimeUnit.SECONDS);
        if(Respuesta.SC_OK != timeStampWorker.getStatusCode()) {
            infoVoto.setMensaje(timeStampWorker.getMessage());
            reciboVoto = new ReciboVoto(
                    timeStampWorker.getStatusCode(),timeStampWorker.getMessage());
            infoVoto.setReciboVoto(reciboVoto);
            return infoVoto;
        }
        //logger.info("Lanzando voto a " + urlVoto);
        HttpResponse response = Contexto.getHttpHelper().sendFile(
                    votoFirmado, Contexto.SIGNED_CONTENT_TYPE, urlVoto);
        
        infoVoto.setCodigoEstado(response.getStatusLine().getStatusCode());
        if (200 == response.getStatusLine().getStatusCode()) {
            byte[] votoValidadoBytes = EntityUtils.toByteArray(response.getEntity());
            SMIMEMessageWrapper votoValidado = new SMIMEMessageWrapper(null,
                new ByteArrayInputStream(votoValidadoBytes), null);                        
            reciboVoto = new ReciboVoto(200, votoValidado, 
                    infoVoto.getVoto().getOpcionSeleccionada().getId().toString());
        } else {
            //logger.error("Error " + response.getStatusLine());
            String mensaje = null;
            if (response.getEntity() != null ) mensaje = EntityUtils.toString(response.getEntity());
            infoVoto.setMensaje(mensaje);
            reciboVoto = new ReciboVoto(
                    response.getStatusLine().getStatusCode(),mensaje);
        }
        infoVoto.setReciboVoto(reciboVoto);
        EntityUtils.consume(response.getEntity());         
        return infoVoto;
    }

    private TimeStampWorker getTimeStampedDocument(File document) {
        if(document == null) return null;
        TimeStampWorker timeStampWorker = null;
        try {
            timeStampedDocument = new SMIMEMessageWrapper(null,document);
            String urlTimeStampServer = ContextoPruebas.getUrlTimeStampServer(
                    ContextoPruebas.getControlAcceso().getServerURL());
            TimeStampRequest timeStampRequest = 
                    timeStampedDocument.getTimeStampRequest(TIMESTAMP_VOTE_HASH);
            timeStampWorker = new TimeStampWorker(null, urlTimeStampServer,
                    this, timeStampedDocument.getTimeStampRequest(TIMESTAMP_VOTE_HASH));
            timeStampWorker.execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            return timeStampWorker;
        }
    }
    
   public String obtenerVotoParaEventoJSON(Evento evento) {
        logger.debug("obtenerVotoParaEventoJSON");
        Map map = new HashMap();
        map.put("eventoURL", ContextoPruebas.getURLEventoParaVotar(
                ContextoPruebas.getControlAcceso().getServerURL(), 
                evento.getEventoId()));
        map.put("opcionSeleccionadaId", evento.getOpcionSeleccionada().getId());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }

    @Override
    public void process(List<String> list) {  }

    @Override
    public void showResult(VotingSystemWorker worker) {
        logger.debug(" - showResult - worker: " + worker.getClass() 
                + " - statusCode:" + worker.getStatusCode());
    }
    
}
