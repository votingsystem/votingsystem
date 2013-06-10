package org.sistemavotacion.worker;

import java.util.List;
import javax.swing.SwingWorker;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class InfoGetterWorker extends SwingWorker<Respuesta, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(InfoGetterWorker.class);

    private VotingSystemWorkerType workerType;
    private String urlDocument;
    private VotingSystemWorkerListener workerListener;
    private Respuesta respuesta  = new Respuesta(Respuesta.SC_ERROR);
    private String contentType = null;
    
    public InfoGetterWorker(VotingSystemWorkerType workerType, 
            String urlDocument, String contentType,
            VotingSystemWorkerListener workerListener) {
        this.workerType = workerType;
        this.urlDocument = urlDocument;
        this.workerListener = workerListener;
        this.contentType = contentType;
    }
    
    @Override protected void done() {//on the EDT
        if(workerListener != null) workerListener.showResult(this);
    }
    
    @Override//on the EDT
    protected void process(List<String> messages) {
        workerListener.processVotingSystemWorkerMsg(messages);
    }
    
    @Override protected Respuesta doInBackground() throws Exception{
        respuesta = Contexto.INSTANCE.getHttpHelper().
                getInfo(urlDocument, contentType);
        return respuesta;
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

    @Override  public int getStatusCode() {
        if(respuesta == null) return Respuesta.SC_ERROR;
        else return respuesta.getCodigoEstado();
    }

    @Override public VotingSystemWorkerType getType() {
        return workerType;
    }
    
}