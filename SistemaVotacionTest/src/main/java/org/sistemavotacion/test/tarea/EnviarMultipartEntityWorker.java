package org.sistemavotacion.test.tarea;

import java.io.File;
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
public class EnviarMultipartEntityWorker extends SwingWorker<String, String> {
    
    private static Logger logger = LoggerFactory.getLogger(EnviarMultipartEntityWorker.class);

    private String urlDestino;
    private LanzadorWorker lanzadorWorker;
    private File archivoEnviado;
    private String cadenaEnviada;
    private Exception exception = null;
    private String message = null;
    private int statusCode = Respuesta.SC_ERROR;
    
    public EnviarMultipartEntityWorker(File archivoEnviado, String urlDestino, 
            LanzadorWorker lanzadorWorker) {
        this.archivoEnviado = archivoEnviado;
        this.lanzadorWorker = lanzadorWorker;
        this.urlDestino = urlDestino;
    }
    
    public EnviarMultipartEntityWorker(String cadenaEnviada, String urlDestino, 
            LanzadorWorker lanzadorWorker) {
        this.cadenaEnviada = cadenaEnviada;
        this.lanzadorWorker = lanzadorWorker;
        this.urlDestino = urlDestino;
    }
    
    public void setFile(File archivoEnviado) {
        this.archivoEnviado = archivoEnviado;
    }
    
    @Override//on the EDT
    protected void done() {	
        try {
        	message = get();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            message = ex.getMessage();
        } finally {
        	lanzadorWorker.mostrarResultadoOperacion(this);
        }
    }
    
    @Override
    protected String doInBackground() throws Exception {
        logger.debug("doInBackground - urlDestino: " + urlDestino);
        String result = null;
        HttpResponse response = null;
        logger.debug("--- cadenaEnviada: " + cadenaEnviada);
        if(archivoEnviado != null) response = Contexto.getHttpHelper().
                enviarArchivoFirmado(archivoEnviado, urlDestino);
        else if (cadenaEnviada != null) {
            logger.debug("--- Enviando cadena firmada");
            response = Contexto.getHttpHelper().
                enviarCadena(cadenaEnviada, urlDestino);
        } 
        statusCode =response.getStatusLine().getStatusCode();
        result = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        return result;
    }
    
	public String getMessage() {
		if(exception != null) return exception.getMessage();
		return message;
	}
    
    public int getStatusCode() {
    	return statusCode;
    }
    
}
