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
* Licencia: http://bit.ly/j9jZQH
*/
public class EnviarMultipartEntityWorker extends SwingWorker<Respuesta, String> {
    
    private static Logger logger = LoggerFactory.getLogger(EnviarMultipartEntityWorker.class);

    private String urlDestino;
    private LanzadorWorker lanzadorWorker;
    private File archivoEnviado;
    private String cadenaEnviada;
    
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
            String mensaje = "Respuesta nula";
            Respuesta respuesta = get();
            if(respuesta != null) {
                mensaje = "Respuesta no nula - mensaje: " + respuesta.getMensaje();
            }
            logger.debug("done - Codigo Estado: " + 
                    respuesta.getCodigoEstado() + " - mensaje: " + mensaje);
            lanzadorWorker.mostrarResultadoOperacion(this, get());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            lanzadorWorker.mostrarResultadoOperacion(this, respuesta);
        }
    }
    
    @Override
    protected Respuesta doInBackground() throws Exception {
        logger.debug("doInBackground - urlDestino: " + urlDestino);
        Respuesta respuesta = null;
        HttpResponse response = null;
        logger.debug("--- cadenaEnviada: " + cadenaEnviada);
        if(archivoEnviado != null) response = Contexto.getHttpHelper().
                enviarArchivoFirmado(archivoEnviado, urlDestino);
        else if (cadenaEnviada != null) {
            logger.debug("--- Enviando cadena firmada");
            response = Contexto.getHttpHelper().
                enviarCadena(cadenaEnviada, urlDestino);
        } 
        respuesta = new Respuesta(response.getStatusLine().getStatusCode(),
                EntityUtils.toString(response.getEntity()));
        logger.debug("doInBackground - response.getStatusLine().getStatusCode(): " + response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());
        return respuesta;
    }
    
}
