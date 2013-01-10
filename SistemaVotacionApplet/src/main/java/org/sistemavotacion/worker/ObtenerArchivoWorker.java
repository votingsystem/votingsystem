package org.sistemavotacion.worker;

import java.util.List;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class ObtenerArchivoWorker extends SwingWorker<Respuesta, String> {
    
    private static Logger logger = LoggerFactory.getLogger(ObtenerArchivoWorker.class);

    String urlArchivo;
    WorkerListener workerListener;

    public ObtenerArchivoWorker(String urlArchivo, WorkerListener workerListener) {
        this.urlArchivo = urlArchivo;
        this.workerListener = workerListener;
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
    
    @Override//on the EDT
    protected void process(List<String> messages) {
        workerListener.process(messages);
    }
    
    @Override
    protected Respuesta doInBackground() throws Exception {
        Respuesta respuesta = null;
        try {
            HttpResponse response = Contexto.getInstancia().getHttpHelper().
                    obtenerArchivo(urlArchivo);
            respuesta = new Respuesta(response.getStatusLine().getStatusCode());
            if (200 == response.getStatusLine().getStatusCode()) {
                byte[] bytesArchivo = EntityUtils.toByteArray(response.getEntity());
                logger.info("bytesArchivo.length: " + bytesArchivo.length);
                respuesta.setBytesArchivo(bytesArchivo);
            } else respuesta.setMensaje( EntityUtils.toString(response.getEntity()));
            EntityUtils.consume(response.getEntity());
        } catch(HttpHostConnectException ex) {
            return new Respuesta(500, "Imposible conectar con servidor");
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            //publish(ex.getMessage());
            return new Respuesta(500, ex.getMessage());
        }
        return respuesta;
    }

    
}
