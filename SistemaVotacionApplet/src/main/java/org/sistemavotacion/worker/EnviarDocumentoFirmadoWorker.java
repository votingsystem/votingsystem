package org.sistemavotacion.worker;

import static org.sistemavotacion.Contexto.*;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class EnviarDocumentoFirmadoWorker extends SwingWorker<Respuesta, String> {
    
    private static Logger logger = LoggerFactory.getLogger(EnviarDocumentoFirmadoWorker.class);

    private String urlDestino;
    private WorkerListener workerListener;
    private Object documentoEnviado;
    
    public EnviarDocumentoFirmadoWorker(Object documentoEnviado, String urlDestino, 
            WorkerListener workerListener) {
        this.documentoEnviado = documentoEnviado;
        this.workerListener = workerListener;
        this.urlDestino = urlDestino;
    }
    
    public EnviarDocumentoFirmadoWorker setDocumentoEnviado(Object documentoEnviado) {
        this.documentoEnviado = documentoEnviado;
        return this;
    }
    
    @Override//on the EDT
    protected void done() {
        try {
            workerListener.showResult(this, get());
        } catch (CancellationException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            workerListener.showResult(this, respuesta);
        }
    }
    
    @Override
    protected Respuesta doInBackground() throws Exception {
        logger.debug("doInBackground - urlDestino: " + urlDestino);
        Respuesta respuesta = null;
        String msg = "<html><b>" + getString("connectionMsg") + "...</b></html>";
        workerListener.process(Arrays.asList(msg));
        HttpResponse response = null;
        if(documentoEnviado instanceof File) {
            response = Contexto.getHttpHelper().enviarArchivoFirmado(
                (File)documentoEnviado, urlDestino);
        } else if(documentoEnviado instanceof byte[]) {
            response = Contexto.getHttpHelper().enviarByteArray(
                (byte[])documentoEnviado, urlDestino);
        } else if(documentoEnviado instanceof String) { 
            response = Contexto.getHttpHelper().enviarCadena(
                (String)documentoEnviado, urlDestino);
        }
        if (200 == response.getStatusLine().getStatusCode()) {
            String mensajeRespuesta = EntityUtils.toString(response.getEntity());
            respuesta = new Respuesta(response.getStatusLine().getStatusCode(), mensajeRespuesta);
        } else {
            respuesta = new Respuesta(response.getStatusLine().getStatusCode(),
                EntityUtils.toString(response.getEntity()));
        }
        EntityUtils.consume(response.getEntity());
        return respuesta;
    }
    
}
