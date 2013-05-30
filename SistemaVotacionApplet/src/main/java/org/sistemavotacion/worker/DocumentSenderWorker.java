package org.sistemavotacion.worker;

import static org.sistemavotacion.Contexto.*;

import java.io.File;
import java.util.Arrays;
import javax.swing.SwingWorker;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class DocumentSenderWorker extends SwingWorker<Integer, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(DocumentSenderWorker.class);

    private String urlDestino;
    private VotingSystemWorkerListener workerListener;
    private Object documentoEnviado;
    private Integer id = null;
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private byte[] messageBytes = null;
    private String documentContentType = null;
    private Exception exception = null;
    
    public DocumentSenderWorker(Integer id, Object documentoEnviado, 
            String documentContentType, String urlDestino, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.documentoEnviado = documentoEnviado;
        this.workerListener = workerListener;
        this.documentContentType = documentContentType;
        this.urlDestino = urlDestino;
    }
    
    public DocumentSenderWorker(Integer id, String urlDestino, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.workerListener = workerListener;
        this.urlDestino = urlDestino;
    }
    
    public DocumentSenderWorker setDocumentoEnviado(Object documentoEnviado,
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
        Respuesta respuesta = null;
        if(documentoEnviado instanceof File) {
            respuesta = Contexto.getHttpHelper().sendFile((File)documentoEnviado, 
                    documentContentType, urlDestino);
        } else if(documentoEnviado instanceof byte[]) {
            respuesta = Contexto.getHttpHelper().sendByteArray(
                (byte[])documentoEnviado, null, urlDestino);
        }
        statusCode = respuesta.getCodigoEstado();
        message = respuesta.getMensaje();
        messageBytes = respuesta.getBytesArchivo();
        return statusCode;
    }

    public byte[] getMessageBytes() {
        return messageBytes;
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
