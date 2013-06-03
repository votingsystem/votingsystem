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

    String urlArchivo;
    VotingSystemWorkerListener workerListener;
    private Integer id = null;
    private Respuesta respuesta = null;
    private String contentType = null;

    public InfoGetterWorker(Integer id, String urlArchivo, String contentType,
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.urlArchivo = urlArchivo;
        this.workerListener = workerListener;
        this.contentType = contentType;
    }

    @Override protected void done() {//on the EDT
        try {
            respuesta = get();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        } 
        workerListener.showResult(this);
    }
    
    @Override//on the EDT
    protected void process(List<String> messages) {
        workerListener.process(messages);
    }
    
    @Override protected Respuesta doInBackground() throws Exception{
        respuesta = Contexto.INSTANCE.getHttpHelper().
                getInfo(urlArchivo, contentType);
        return respuesta;
    }

   @Override public String getMessage() {
        if(respuesta == null) return null;
        else return respuesta.getMensaje();
    }

    @Override public int getId() {
        return this.id;
    }

    @Override  public int getStatusCode() {
        if(respuesta == null) return Respuesta.SC_ERROR;
        else return respuesta.getCodigoEstado();
    }
    
    @Override public Respuesta getRespuesta() {
        return respuesta;
    }
    
}