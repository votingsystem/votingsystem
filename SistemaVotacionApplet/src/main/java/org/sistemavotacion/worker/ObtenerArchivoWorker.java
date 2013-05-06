package org.sistemavotacion.worker;

import java.util.List;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.AppletFirma;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class ObtenerArchivoWorker extends SwingWorker<Integer, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(ObtenerArchivoWorker.class);

    String urlArchivo;
    VotingSystemWorkerListener workerListener;
    private Integer id = null;
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private Exception exception = null;
    private byte[] bytesArchivo = null;

    public ObtenerArchivoWorker(Integer id, String urlArchivo, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.urlArchivo = urlArchivo;
        this.workerListener = workerListener;
    }

    @Override//on the EDT
    protected void done() {
        try {
            statusCode = get();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            exception = ex;
        } finally {workerListener.showResult(this);}
    }
    
    @Override//on the EDT
    protected void process(List<String> messages) {
        workerListener.process(messages);
    }
    
    @Override protected Integer doInBackground() throws Exception {
        try {
            HttpResponse response = Contexto.getInstancia().getHttpHelper().
                    obtenerArchivo(urlArchivo);
            statusCode = response.getStatusLine().getStatusCode();
            if (Respuesta.SC_OK == statusCode) {
                bytesArchivo = EntityUtils.toByteArray(response.getEntity());
            } else message = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
        } catch(HttpHostConnectException ex) {
            logger.error(ex.getMessage(), ex);
            statusCode = Respuesta.SC_ERROR_EJECUCION;
            exception = new Exception(Contexto.getString(
                    "hostConnectionErrorMsg") + " con " + urlArchivo);
        }catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            statusCode = Respuesta.SC_ERROR_EJECUCION;
            exception = ex;
        } finally {
            return statusCode;
        }
    }
    
    public byte[] getBytesArchivo() {
        return bytesArchivo;
    }

    @Override public String getMessage() {
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
