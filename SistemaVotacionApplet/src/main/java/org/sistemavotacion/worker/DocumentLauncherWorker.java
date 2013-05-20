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
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class DocumentLauncherWorker extends SwingWorker<Integer, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(DocumentLauncherWorker.class);

    private String urlDestino;
    private VotingSystemWorkerListener workerListener;
    private Object documentoEnviado;
    private Integer id = null;
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private String documentContentType = null;
    private Exception exception = null;
    
    public DocumentLauncherWorker(Integer id, Object documentoEnviado, 
            String documentContentType, String urlDestino, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.documentoEnviado = documentoEnviado;
        this.workerListener = workerListener;
        this.documentContentType = documentContentType;
        this.urlDestino = urlDestino;
    }
    
    public DocumentLauncherWorker(Integer id, String urlDestino, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.workerListener = workerListener;
        this.urlDestino = urlDestino;
    }
    
    public DocumentLauncherWorker setDocumentoEnviado(Object documentoEnviado,
            String documentContentType) {
        this.documentoEnviado = documentoEnviado;
        this.documentContentType = documentContentType;
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
            response = Contexto.getHttpHelper().sendFile((File)documentoEnviado, 
                    documentContentType, urlDestino);
        } else if(documentoEnviado instanceof byte[]) {
            response = Contexto.getHttpHelper().sendByteArray(
                (byte[])documentoEnviado, null, urlDestino);
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
