package org.sistemavotacion.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.X509Certificate;
import javax.mail.internet.MimeMessage;
import javax.swing.SwingWorker;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class NotificarVotoWorker extends SwingWorker<Respuesta, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(NotificarVotoWorker.class);

    private String urlServidorRecolectorVotos;
    private VotingSystemWorkerListener workerListener;
    private SMIMEMessageWrapper votoFirmado;
    private Evento evento;
    private Integer id = null;
    private ReciboVoto reciboVoto = null;
    private Respuesta respuesta = null;
    private X509Certificate controlCenterCert = null;
    private PKCS10WrapperClient pkcs10WrapperClient;
    
    public NotificarVotoWorker(Integer id, Evento evento,  
            String urlServidorRecolectorVotos, X509Certificate controlCenterCert, 
            SMIMEMessageWrapper smimeDocument, PKCS10WrapperClient pkcs10WrapperClient, 
            VotingSystemWorkerListener workerListener) {
        this.id = id;
        this.evento = evento;        
        this.urlServidorRecolectorVotos = urlServidorRecolectorVotos;
        this.workerListener = workerListener;
        this.votoFirmado = smimeDocument;
        this.pkcs10WrapperClient = pkcs10WrapperClient;
        this.controlCenterCert = controlCenterCert;
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
        MimeMessage encryptedMessage = Encryptor.encryptSMIME(
                votoFirmado, controlCenterCert);
        File encryptedVote = File.createTempFile("EncryptedVote", ".p7s");
        encryptedVote.deleteOnExit();
        encryptedMessage.writeTo(new FileOutputStream(encryptedVote));
        
        respuesta = Contexto.INSTANCE.getHttpHelper().sendFile(encryptedVote, 
                Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE, 
                urlServidorRecolectorVotos);
        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            byte[] votoValidadoBytes = respuesta.getBytesArchivo();
            SMIMEMessageWrapper votoValidado = Encryptor.decryptSMIMEMessage(
                    votoValidadoBytes, pkcs10WrapperClient.getPublicKey(), 
                    pkcs10WrapperClient.getPrivateKey());
                     
            reciboVoto = new ReciboVoto(Respuesta.SC_OK, votoValidado, evento);
        }
        return respuesta;
    }

    public ReciboVoto getReciboVoto() {
        return reciboVoto;
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
