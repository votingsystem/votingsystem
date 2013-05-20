package org.sistemavotacion.worker;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import static org.sistemavotacion.Contexto.getString;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class FileMapLauncherWorker extends SwingWorker<Integer, String> 
        implements VotingSystemWorker {
    
        
    private static Logger logger = LoggerFactory.getLogger(FileMapLauncherWorker.class);

    private String serverURL;
    private VotingSystemWorkerListener workerListener;
    private Map<String, Object> fileMap;
    private Integer id = null;
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private Exception exception = null;
    
    public FileMapLauncherWorker(Integer id, Map<String, Object> fileMap, String serverURL, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.fileMap = fileMap;
        this.workerListener = workerListener;
        this.serverURL = serverURL;
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
        logger.debug("doInBackground - serverURL: " + serverURL);
        String msg = "<html><b>" + getString("connectionMsg") + "...</b></html>";
        workerListener.process(Arrays.asList(msg));
        HttpResponse response = Contexto.getHttpHelper().
                sendObjectMap(fileMap, serverURL);
        statusCode = response.getStatusLine().getStatusCode();
        message = EntityUtils.toString(response.getEntity());
        logger.debug("doInBackground - message: " + message);
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
