package org.sistemavotacion.test.tarea;

import java.util.List;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class ObtenerInfoServidorWorker extends SwingWorker<Respuesta, String> {
    
    private static Logger logger = LoggerFactory.getLogger(ObtenerInfoServidorWorker.class);

    private String urlInfoServidor;
    private LanzadorWorker lanzadorWorker;
    
    public ObtenerInfoServidorWorker(String urlInfoServidor, LanzadorWorker lanzadorWorker) {
        this.urlInfoServidor = urlInfoServidor;
        this.lanzadorWorker = lanzadorWorker;
    }
            
    @Override//on the EDT
    protected void done() {
        try {
            lanzadorWorker.mostrarResultadoOperacion(this, get());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            lanzadorWorker.mostrarResultadoOperacion(this, respuesta);
        }
    }
    
    @Override//on the EDT
    protected void process(List<String> messages) {
        lanzadorWorker.process(messages);
    }
    
    @Override
    protected Respuesta doInBackground() throws Exception {
        logger.debug("doInBackground - urlInfoServidor: " + urlInfoServidor);        
        Respuesta respuesta = new Respuesta();
        try {
            HttpResponse response = Contexto.getHttpHelper().obtenerArchivo(
                    urlInfoServidor);
            respuesta.setCodigoEstado(response.getStatusLine().getStatusCode());
            respuesta.setMensaje(EntityUtils.toString(response.getEntity()));
            EntityUtils.consume(response.getEntity());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            String mensajeError = "Problemas de conexión con el Control de Acceso: "
                    + ex.getMessage();
            respuesta = new Respuesta(Respuesta.SC_ERROR, mensajeError);
        }
        return respuesta;
    }
    
}
