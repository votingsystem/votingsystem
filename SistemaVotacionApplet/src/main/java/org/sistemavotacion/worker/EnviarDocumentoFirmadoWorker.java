package org.sistemavotacion.worker;

import static org.sistemavotacion.Contexto.*;

import java.io.File;
import java.util.Arrays;
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
public class EnviarDocumentoFirmadoWorker extends SwingWorker<Integer, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(EnviarDocumentoFirmadoWorker.class);

    private String urlDestino;
    private VotingSystemWorkerListener workerListener;
    private Object documentoEnviado;
    private Integer id = null;
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private Exception exception = null;
    
    public EnviarDocumentoFirmadoWorker(Integer id, Object documentoEnviado, String urlDestino, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.documentoEnviado = documentoEnviado;
        this.workerListener = workerListener;
        this.urlDestino = urlDestino;
    }
    
    public EnviarDocumentoFirmadoWorker(Integer id, String urlDestino, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
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
            statusCode = get();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            exception = ex;
        } finally {
            workerListener.showResult(this);
        }
    }
    
    @Override protected Integer doInBackground() throws Exception {
        logger.debug("doInBackground - urlDestino: " + urlDestino);
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
        statusCode = response.getStatusLine().getStatusCode();
        message = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        return statusCode;
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
