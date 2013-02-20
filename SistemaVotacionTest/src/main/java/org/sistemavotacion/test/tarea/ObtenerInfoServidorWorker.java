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
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class ObtenerInfoServidorWorker extends SwingWorker<String, String> {
    
    private static Logger logger = LoggerFactory.getLogger(ObtenerInfoServidorWorker.class);

    private String urlInfoServidor;
    private LanzadorWorker lanzadorWorker;
    private String message = null;
    private int statusCode = Respuesta.SC_ERROR_EJECUCION;
    
    
    public ObtenerInfoServidorWorker(String urlInfoServidor, LanzadorWorker lanzadorWorker) {
        this.urlInfoServidor = urlInfoServidor;
        this.lanzadorWorker = lanzadorWorker;
    }
            
    @Override//on the EDT
    protected void done() {
        try {
        	message = get();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            message = "Problemas de conexión con el Control de Acceso: " + ex.getMessage();
        } finally {
        	lanzadorWorker.mostrarResultadoOperacion(this);
        }
    }
    
    @Override//on the EDT
    protected void process(List<String> messages) {
        lanzadorWorker.process(messages, this);
    }
    
    @Override
    protected String doInBackground() throws Exception {
        logger.debug("doInBackground - urlInfoServidor: " + urlInfoServidor);   
        String result = null;
        HttpResponse response = Contexto.getHttpHelper().obtenerArchivo(
                urlInfoServidor);
        statusCode = response.getStatusLine().getStatusCode();
        result = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        return result;
    }
    
	public String getMessage() {
		return message;
	}
    
    public int getStatusCode() {
    	return statusCode;
    }
    
}
