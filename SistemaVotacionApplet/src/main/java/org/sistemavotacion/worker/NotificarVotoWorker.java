package org.sistemavotacion.worker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.dialogo.PreconditionsCheckerDialog;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.EncryptionHelper;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class NotificarVotoWorker extends SwingWorker<Integer, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(NotificarVotoWorker.class);

    private String urlServidorRecolectorVotos;
    private VotingSystemWorkerListener workerListener;
    private File votoFirmado;
    private Evento evento;
    private Integer id = null;
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private Exception exception = null;
    private ReciboVoto reciboVoto = null;
    private X509Certificate controlCenterCert = null;
    private PKCS10WrapperClient pkcs10WrapperClient;
    
    public NotificarVotoWorker(Integer id, Evento evento, String urlServidorRecolectorVotos, 
            File votoFirmado, PKCS10WrapperClient pkcs10WrapperClient, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.evento = evento;        
        this.urlServidorRecolectorVotos = urlServidorRecolectorVotos;
        this.workerListener = workerListener;
        this.votoFirmado = votoFirmado;
        this.pkcs10WrapperClient = pkcs10WrapperClient;
        this.controlCenterCert = PreconditionsCheckerDialog.getCert(
                evento.getCentroControl().getServerURL());
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
        EncryptionHelper.encryptSMIMEFile(votoFirmado, controlCenterCert);
        HttpResponse response = Contexto.getHttpHelper().
                enviarArchivoFirmado(votoFirmado, urlServidorRecolectorVotos);
        statusCode = response.getStatusLine().getStatusCode();
        if (Respuesta.SC_OK == statusCode) {
            byte[] votoValidadoBytes = EntityUtils.toByteArray(response.getEntity());
            
            SMIMEMessageWrapper votoValidado = 
                    EncryptionHelper.decryptSMIMEMessage(votoValidadoBytes, 
                    pkcs10WrapperClient.getCertificate(), pkcs10WrapperClient.getPrivateKey());
                     
            reciboVoto = new ReciboVoto(Respuesta.SC_OK, votoValidado, evento);
        } else {
            message = EntityUtils.toString(response.getEntity());
        }
        EntityUtils.consume(response.getEntity());   
        return statusCode;
    }

    public ReciboVoto getReciboVoto() {
        return reciboVoto;
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
