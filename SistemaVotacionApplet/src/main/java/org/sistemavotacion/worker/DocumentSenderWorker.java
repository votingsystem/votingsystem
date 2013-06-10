package org.sistemavotacion.worker;

import java.io.File;
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

    private VotingSystemWorkerType workerType;
    private String urlToSendDocument;
    private VotingSystemWorkerListener workerListener;
    private Object documentoEnviado;
    private Respuesta respuesta  = new Respuesta(Respuesta.SC_ERROR);
    private String documentContentType = null;

    public DocumentSenderWorker(VotingSystemWorkerType workerType, 
            Object documentoEnviado, String documentContentType, 
            String urlToSendDocument, VotingSystemWorkerListener workerListener) {
        this.workerType = workerType;
        this.documentoEnviado = documentoEnviado;
        this.workerListener = workerListener;
        this.documentContentType = documentContentType;
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
        if(workerListener != null) workerListener.showResult(this);
    }
    
    @Override protected Respuesta doInBackground() throws Exception {
        logger.debug("doInBackground - urlToSendDocument: " + urlToSendDocument);
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
        
    @Override public String getErrorMessage() {
        if(workerType != null) return "### ERROR - " + workerType + " - msg: " 
                + respuesta.getMensaje(); 
        else return "### ERROR - msg: " + respuesta.getMensaje();  
    }

    @Override public int getStatusCode() {
        if(respuesta == null) return Respuesta.SC_ERROR;
        else return respuesta.getCodigoEstado();
    }
    
    @Override public VotingSystemWorkerType getType() {
        return workerType;
    }
    
}
