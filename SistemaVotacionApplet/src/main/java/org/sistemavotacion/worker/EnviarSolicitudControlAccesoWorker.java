package org.sistemavotacion.worker;

import java.io.File;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class EnviarSolicitudControlAccesoWorker extends SwingWorker<Integer, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(EnviarSolicitudControlAccesoWorker.class);

    private String urlSolicitudAcceso;
    private VotingSystemWorkerListener workerListener;
    private File solicitudAcceso;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private Integer id = null;
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private Exception exception = null;
    
    public EnviarSolicitudControlAccesoWorker(Integer id, File solicitudAcceso,
            String urlSolicitudAcceso, PKCS10WrapperClient pkcs10WrapperClient, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.solicitudAcceso = solicitudAcceso;
        this.workerListener = workerListener;
        this.urlSolicitudAcceso = urlSolicitudAcceso;
        this.pkcs10WrapperClient = pkcs10WrapperClient;
    }
    
    @Override//on the EDT
    protected void done() {
        try {
            statusCode = get();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            exception = ex;
        } finally {
            workerListener.showResult(this);
        }
    }
    
    @Override protected Integer doInBackground() throws Exception {
        logger.debug("doInBackground - urlSolicitudAcceso: " + urlSolicitudAcceso 
                + " - solicitudAcceso: " + solicitudAcceso.getAbsolutePath());
        HttpResponse response = Contexto.getHttpHelper().enviarSolicitudAcceso(
                pkcs10WrapperClient.getPEMEncodedRequestCSR(), solicitudAcceso,
                urlSolicitudAcceso);
        statusCode = response.getStatusLine().getStatusCode();
        if (Respuesta.SC_OK == statusCode) {
            pkcs10WrapperClient.initSigner(EntityUtils.toByteArray(response.getEntity()));
        } else {
            message = EntityUtils.toString(response.getEntity());
        }
        EntityUtils.consume(response.getEntity());
        return statusCode;
    }
    
    public PKCS10WrapperClient getPKCS10WrapperClient() {
        return pkcs10WrapperClient;
    }

    @Override public String getMessage() {
        if(exception != null) return exception.getMessage();
        else return message;
    }

    @Override public int getId() {
        return this.id;
    }

    @Override public int getStatusCode() {
        return statusCode;
    }
    
}
