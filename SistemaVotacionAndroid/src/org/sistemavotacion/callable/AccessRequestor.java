package org.sistemavotacion.callable;

import static org.sistemavotacion.android.Aplicacion.KEY_SIZE;
import static org.sistemavotacion.android.Aplicacion.PROVIDER;
import static org.sistemavotacion.android.Aplicacion.SIG_NAME;
import static org.sistemavotacion.android.Aplicacion.VOTE_SIGN_MECHANISM;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.mail.Header;

import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.HttpHelper;

import android.util.Log;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestor implements Callable<Respuesta> {
    
	public static final String TAG = "AccessRequestor";

    private SMIMEMessageWrapper accessRequets;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private X509Certificate destinationCert = null;
    private String serviceURL = null;
    
    public AccessRequestor (SMIMEMessageWrapper accessRequets,
            Evento evento, X509Certificate destinationCert, 
            String serviceURL) throws Exception {
        this.accessRequets = accessRequets;
        this.serviceURL = serviceURL;
        this.destinationCert = destinationCert;
        this.pkcs10WrapperClient = new PKCS10WrapperClient(
                KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM, PROVIDER, 
                evento.getControlAcceso().getServerURL(), 
                evento.getEventoId().toString(), 
                evento.getHashCertificadoVotoHex());
    }
    
    @Override public Respuesta call() { 
        Log.d(TAG + ".call", " - urlAccessRequest: " + serviceURL);

        try {
            MessageTimeStamper timeStamper = new MessageTimeStamper(accessRequets);
            Respuesta respuesta = timeStamper.call();
            if(Respuesta.SC_OK != respuesta.getCodigoEstado()) return respuesta;
            accessRequets = timeStamper.getSmimeMessage();

            Header header = new Header("votingSystemMessageType", "voteCsr");
            byte[] csrEncryptedBytes = Encryptor.encryptMessage(pkcs10WrapperClient.
            		getPEMEncodedRequestCSR(), destinationCert, header);

            byte[] csrEncryptedAccessRequestBytes = Encryptor.encryptSMIME(
                    accessRequets, destinationCert);
            String csrFileName = Aplicacion.CSR_FILE_NAME + ":" + 
                    Aplicacion.ENCRYPTED_CONTENT_TYPE;

            String accessRequestFileName = Aplicacion.ACCESS_REQUEST_FILE_NAME + ":" + 
                    Aplicacion.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, csrEncryptedBytes);
            mapToSend.put(accessRequestFileName, csrEncryptedAccessRequestBytes);

            respuesta = HttpHelper.sendObjectMap(mapToSend, serviceURL);
            if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                byte[] encryptedData = respuesta.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, 
                        pkcs10WrapperClient.getPublicKey(), 
                        pkcs10WrapperClient.getPrivateKey());
                pkcs10WrapperClient.initSigner(decryptedData);
            }
            
            return respuesta;
        } catch(Exception ex) {
            ex.printStackTrace();
            return new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        }
    }
    
    public PKCS10WrapperClient getPKCS10WrapperClient() {
        return pkcs10WrapperClient;
    }
    
}