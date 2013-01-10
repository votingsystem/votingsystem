package org.sistemavotacion.worker;

import java.io.ByteArrayInputStream;
import java.io.File;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class NotificarVotoWorker extends SwingWorker<Respuesta, String> {
    
    private static Logger logger = LoggerFactory.getLogger(NotificarVotoWorker.class);

    private String urlServidorRecolectorVotos;
    private WorkerListener workerListener;
    private File votoFirmado;
    private Evento evento;
    
    public NotificarVotoWorker(Evento evento, String urlServidorRecolectorVotos, 
            File votoFirmado, WorkerListener workerListener) {
        this.evento = evento;        
        this.urlServidorRecolectorVotos = urlServidorRecolectorVotos;
        this.workerListener = workerListener;
        this.votoFirmado = votoFirmado;
    }
            
    @Override//on the EDT
    protected void done() {
        try {
            workerListener.showResult(this, get());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            workerListener.showResult(this, respuesta);
        }
    }
    @Override
    protected Respuesta doInBackground() throws Exception {
        Respuesta respuesta;
        ReciboVoto reciboVoto;
        HttpResponse response = Contexto.getHttpHelper().
                enviarArchivoFirmado(votoFirmado, urlServidorRecolectorVotos);
        if (200 == response.getStatusLine().getStatusCode()) {
            byte[] votoValidadoBytes = EntityUtils.toByteArray(response.getEntity());
            SMIMEMessageWrapper votoValidado = SMIMEMessageWrapper.build(
                new ByteArrayInputStream(votoValidadoBytes), null);                        
            reciboVoto = new ReciboVoto(200, votoValidado, evento);
            respuesta = new Respuesta(
                response.getStatusLine().getStatusCode(), reciboVoto);
        } else {
            respuesta = new Respuesta(response.getStatusLine().getStatusCode(), 
                    EntityUtils.toString(response.getEntity()));
        }
        EntityUtils.consume(response.getEntity());   
        return respuesta;
    }
    
}
