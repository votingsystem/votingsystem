package org.sistemavotacion.worker;

import java.util.Arrays;
import java.util.Map;
import javax.swing.SwingWorker;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class FileMapLauncherWorker extends SwingWorker<Respuesta, String> 
        implements VotingSystemWorker {
    
        
    private static Logger logger = LoggerFactory.getLogger(FileMapLauncherWorker.class);

    private String serverURL;
    private VotingSystemWorkerListener workerListener;
    private Map<String, Object> fileMap;
    private Integer id = null;
    private Respuesta respuesta = null;
    
    public FileMapLauncherWorker(Integer id, Map<String, Object> fileMap, String serverURL, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.fileMap = fileMap;
        this.workerListener = workerListener;
        this.serverURL = serverURL;
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
    
    @Override protected Respuesta doInBackground() throws Exception {
        logger.debug("doInBackground - serverURL: " + serverURL);
        String msg = "<html><b>" + Contexto.INSTANCE.getString(
                "connectionMsg") + "...</b></html>";
        workerListener.processVotingSystemWorkerMsg(Arrays.asList(msg));
        return Contexto.INSTANCE.getHttpHelper().sendObjectMap(fileMap, serverURL);
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
