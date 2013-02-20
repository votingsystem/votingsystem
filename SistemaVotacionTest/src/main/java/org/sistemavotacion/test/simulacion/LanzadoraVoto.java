package org.sistemavotacion.test.simulacion;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.InfoVoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class LanzadoraVoto implements Callable<InfoVoto> {
    
    private static Logger logger = LoggerFactory.getLogger(LanzadoraVoto.class);

    InfoVoto infoVoto;
    
    public LanzadoraVoto (InfoVoto infoVoto) throws Exception {
        this.infoVoto = infoVoto;
    }
    

    @Override
    public InfoVoto call() throws Exception {
        String votoJSON = obtenerVotoParaEventoJSON(infoVoto.getVoto());
        String votoFirmado = infoVoto.getPkcs10WrapperClient().genSignedString(
                infoVoto.getFrom(), infoVoto.getVoto().getControlAcceso().getNombreNormalizado(), 
                votoJSON, "[VOTO]", null, SignedMailGenerator.Type.USER);
        String urlVoto = ContextoPruebas.getURLVoto(
                infoVoto.getVoto().getCentroControl().getServerURL());
        //logger.info("Lanzando voto a " + urlVoto);
        HttpResponse response = Contexto.getHttpHelper().enviarCadena(
                    votoFirmado, urlVoto);
        ReciboVoto reciboVoto = null;
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
    
}
