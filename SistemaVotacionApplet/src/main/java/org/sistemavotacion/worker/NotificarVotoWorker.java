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
public class NotificarVotoWorker extends SwingWorker<Integer, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(NotificarVotoWorker.class);

    private String urlServidorRecolectorVotos;
    private VotingSystemWorkerListener workerListener;
    private File votoFirmado;
    private Evento evento;
    private Integer id = null;
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private Exception exception = null;
    private ReciboVoto reciboVoto = null;
    
    public NotificarVotoWorker(Integer id, Evento evento, String urlServidorRecolectorVotos, 
            File votoFirmado, VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.evento = evento;        
        this.urlServidorRecolectorVotos = urlServidorRecolectorVotos;
        this.workerListener = workerListener;
        this.votoFirmado = votoFirmado;
    }
            
    @Override//on the EDT
    protected void done() {
        try {
            statusCode = get();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            exception = ex;
        } finally {
            workerListener.showResult(this);
        }
    }
    @Override protected Integer doInBackground() throws Exception {
        HttpResponse response = Contexto.getHttpHelper().
                enviarArchivoFirmado(votoFirmado, urlServidorRecolectorVotos);
        statusCode = response.getStatusLine().getStatusCode();
        if (Respuesta.SC_OK == statusCode) {
            byte[] votoValidadoBytes = EntityUtils.toByteArray(response.getEntity());
            SMIMEMessageWrapper votoValidado = new SMIMEMessageWrapper(null,
                new ByteArrayInputStream(votoValidadoBytes), null);                        
            reciboVoto = new ReciboVoto(Respuesta.SC_OK, votoValidado, evento);
        } else {
            message = EntityUtils.toString(response.getEntity());
        }
        EntityUtils.consume(response.getEntity());   
        return statusCode;
    }

    public ReciboVoto getReciboVoto() {
        return reciboVoto;
    }
    
    @Override
    public String getMessage() {
        if(exception != null) return exception.getMessage();
        else return message;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
    
}
