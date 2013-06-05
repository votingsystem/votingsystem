package org.sistemavotacion.worker;

import java.io.File;
import java.util.Arrays;
import javax.swing.SwingWorker;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class DocumentSenderWorker extends SwingWorker<Respuesta, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(DocumentSenderWorker.class);

    private String urlToSendDocument;
    private VotingSystemWorkerListener workerListener;
    private Object documentoEnviado;
    private Respuesta respuesta = null;
    private Integer id = null;
    private String documentContentType = null;
    
    public DocumentSenderWorker(Integer id, Object documentoEnviado, 
            String documentContentType, String urlToSendDocument, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.documentoEnviado = documentoEnviado;
        this.workerListener = workerListener;
        this.documentContentType = documentContentType;
        this.urlToSendDocument = urlToSendDocument;
    }
    
    public DocumentSenderWorker(Integer id, String urlToSendDocument, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.workerListener = workerListener;
        this.urlToSendDocument = urlToSendDocument;
    }
    
    public DocumentSenderWorker setDocumentoEnviado(Object documentoEnviado,
            String documentContentType) {
        this.documentoEnviado = documentoEnviado;
        this.documentContentType = documentContentType;
        return this;
    }
    
    @Override protected void done() {//on the EDT
        try {
            respuesta = get();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta.appendMessage(ex.getMessage());
        } 
        workerListener.showResult(this);
    }
    
    @Override protected Respuesta doInBackground() throws Exception {
        logger.debug("doInBackground - urlToSendDocument: " + urlToSendDocument);
        String msg = Contexto.INSTANCE.getString("connectionMsg");
        workerListener.processVotingSystemWorkerMsg(Arrays.asList(msg));
        if(documentoEnviado instanceof File) {
            respuesta = Contexto.INSTANCE.getHttpHelper().sendFile((File)documentoEnviado, 
                    documentContentType, urlToSendDocument);
        } else if(documentoEnviado instanceof byte[]) {
            respuesta = Contexto.INSTANCE.getHttpHelper().sendByteArray(
                (byte[])documentoEnviado, documentContentType, urlToSendDocument);
        }
        return respuesta;
    }

    public byte[] getMessageBytes() {
        if(respuesta == null) return null;
        else return respuesta.getBytesArchivo();
    }
    
    @Override public String getMessage() {
        if(respuesta == null) return null;
        else return respuesta.getMensaje();
    }

    @Override public int getId() {
        return this.id;
    }

    @Override public int getStatusCode() {
        if(respuesta == null) return Respuesta.SC_ERROR;
        else return respuesta.getCodigoEstado();
    }
    
    @Override public Respuesta getRespuesta() {
        return respuesta;
    }
    
}
