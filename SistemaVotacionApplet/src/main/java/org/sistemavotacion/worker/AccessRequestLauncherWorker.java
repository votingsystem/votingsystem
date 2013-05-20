package org.sistemavotacion.worker;

import java.io.File;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.EncryptionHelper;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import org.sistemavotacion.dialogo.PreconditionsCheckerDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class AccessRequestLauncherWorker extends SwingWorker<Integer, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(AccessRequestLauncherWorker.class);

    private Evento evento;
    private VotingSystemWorkerListener workerListener;
    private File solicitudAcceso;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private Integer id = null;
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private Exception exception = null;
    private X509Certificate accesRequestServerCert = null;
    
    public AccessRequestLauncherWorker(Integer id, File solicitudAcceso,
            Evento evento, PKCS10WrapperClient pkcs10WrapperClient, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.solicitudAcceso = solicitudAcceso;
        this.workerListener = workerListener;
        this.evento = evento;
        accesRequestServerCert = PreconditionsCheckerDialog.getCert(
                evento.getControlAcceso().getServerURL());
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
        logger.debug("doInBackground - urlSolicitudAcceso: " + evento.getUrlSolicitudAcceso() 
                + " - solicitudAcceso: " + solicitudAcceso.getAbsolutePath());
        File csrEncryptedFile = File.createTempFile("csrEncryptedFile", ".p7m");
        csrEncryptedFile.deleteOnExit();
        EncryptionHelper.encryptText(pkcs10WrapperClient.getPEMEncodedRequestCSR(), 
        			csrEncryptedFile, accesRequestServerCert);
        EncryptionHelper.encryptSMIMEFile(solicitudAcceso, accesRequestServerCert);
        
        String accessRequestFileName = Contexto.ACCESS_REQUEST_FILE_NAME + ":" + 
                Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
        Map<String, Object> mapToSend = new HashMap<String, Object>();
        mapToSend.put(Contexto.CSR_FILE_NAME, csrEncryptedFile);
        mapToSend.put(accessRequestFileName, solicitudAcceso);
        
        HttpResponse response = Contexto.getHttpHelper().sendObjectMap(
                mapToSend, evento.getUrlSolicitudAcceso());
        statusCode = response.getStatusLine().getStatusCode();
        if (Respuesta.SC_OK == statusCode) {
            byte[] encryptedData = EntityUtils.toByteArray(response.getEntity());
            Object decryptedData = EncryptionHelper.decryptMessage(
                    encryptedData, null, pkcs10WrapperClient.getPrivateKey());
            byte[] decryptedDataBytes = null;
            if(decryptedData instanceof byte[]) {
                decryptedDataBytes = (byte[]) decryptedData;
            } else if(decryptedData instanceof String) {
                decryptedDataBytes = ((String)decryptedData).getBytes();
            }
            pkcs10WrapperClient.initSigner(decryptedDataBytes); 
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