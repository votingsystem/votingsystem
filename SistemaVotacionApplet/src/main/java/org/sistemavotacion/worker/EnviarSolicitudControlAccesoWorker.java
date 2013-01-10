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
public class EnviarSolicitudControlAccesoWorker extends SwingWorker<Respuesta, String> {
    
    private static Logger logger = LoggerFactory.getLogger(EnviarSolicitudControlAccesoWorker.class);

    private String urlSolicitudAcceso;
    private WorkerListener workerListener;
    private File solicitudAcceso;
    private PKCS10WrapperClient pkcs10WrapperClient;
    
    public EnviarSolicitudControlAccesoWorker(File solicitudAcceso,
            String urlSolicitudAcceso, PKCS10WrapperClient pkcs10WrapperClient, WorkerListener workerListener) {
        this.solicitudAcceso = solicitudAcceso;
        this.workerListener = workerListener;
        this.urlSolicitudAcceso = urlSolicitudAcceso;
        this.pkcs10WrapperClient = pkcs10WrapperClient;
    }
    
    @Override//on the EDT
    protected void done() {
        try {
            workerListener.showResult(this, get());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            workerListener.showResult(this, respuesta);
        }
    }
    
    @Override
    protected Respuesta doInBackground() throws Exception {
        logger.debug("doInBackground - urlSolicitudAcceso: " + urlSolicitudAcceso);
        Respuesta respuesta = null;
        HttpResponse response = Contexto.getHttpHelper().enviarSolicitudAcceso(
                pkcs10WrapperClient.getPEMEncodedRequestCSR(), solicitudAcceso,
                urlSolicitudAcceso);
        if (Respuesta.SC_OK == response.getStatusLine().getStatusCode()) {
            pkcs10WrapperClient.initSigner(EntityUtils.toByteArray(response.getEntity()));
            respuesta = new Respuesta(Respuesta.SC_OK, pkcs10WrapperClient);
        } else {
            respuesta = new Respuesta(response.getStatusLine().getStatusCode(), 
                    EntityUtils.toString(response.getEntity()));
        }
        EntityUtils.consume(response.getEntity());
        return respuesta;
    }
    
}
