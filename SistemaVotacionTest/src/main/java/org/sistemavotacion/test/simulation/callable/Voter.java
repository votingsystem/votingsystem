package org.sistemavotacion.test.simulation.callable;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Voter implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(Voter.class);
   
    private Evento evento;
    private String nifFrom;
        
    public Voter (Evento evento) throws Exception {
        this.evento = evento; 
        nifFrom = evento.getUsuario().getNif();
        
    }
    
    @Override public Respuesta call() {
        try {
            KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(nifFrom);
            String msgSubject = ContextoPruebas.INSTANCE.getString(
                    "accessRequestMsgSubject", evento.getEventoId());

            /*String cancelVoteStr =  evento.getCancelVoteJSON().toString();
            File anuladorVoto = new File(Contexto.getUserDirPath(nifFrom,
                    ContextoPruebas.DEFAULTS.APPDIR)
                    + Contexto.CANCEL_VOTE_FILE + evento.getEventoId() + 
                    "_usu" + nifFrom + ".json");
            FileUtils.copyStreamToFile(new ByteArrayInputStream(
                    cancelVoteStr.getBytes()), anuladorVoto);*/
            
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    mockDnie, ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.PASSWORD.toCharArray(),
                    ContextoPruebas.DNIe_SIGN_MECHANISM);
            
            String accessRequestStr = evento.getAccessRequestJSON().toString();
            SMIMEMessageWrapper smimeMessage = signedMailGenerator.
                    genMimeMessage(nifFrom, 
                    evento.getControlAcceso().getNombreNormalizado(), 
                    accessRequestStr, msgSubject, null);
            evento.setUrlSolicitudAcceso(ContextoPruebas.INSTANCE.
                    getURLAccessRequest());
            
            X509Certificate destinationCert = ContextoPruebas.INSTANCE.
                        getAccessControl().getCertificate();            
            AccessRequestor accessWorker = new AccessRequestor( 
                    smimeMessage, evento, destinationCert);
            
            Respuesta respuesta = accessWorker.call();
            respuesta.setEvento(evento);
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                PKCS10WrapperClient wrapperClient = accessWorker.
                    getPKCS10WrapperClient();
                String votoJSON = evento.getVoteJSON().toString();
                String subject = ContextoPruebas.INSTANCE.getString(
                        "voteMsgSubject", evento.getEventoId());
                try {
                    smimeMessage = wrapperClient.genMimeMessage(
                        evento.getHashCertificadoVotoBase64(), 
                        evento.getControlAcceso().getNombreNormalizado(),
                        votoJSON, subject, null);
                } catch(Exception ex) {
                    logger.error(ex.getMessage() + " - Respuesta codigoEstado:" + 
                    respuesta.getCodigoEstado(), ex);
                }
                

                String urlVoteService = ContextoPruebas.getURLVoto(
                    evento.getCentroControl().getServerURL()); 
                SMIMESignedSender sender= new SMIMESignedSender(
                        null, smimeMessage, urlVoteService, wrapperClient.
                        getKeyPair(), null);
                Respuesta senderResponse = sender.call();
                senderResponse.setEvento(evento);
                if (Respuesta.SC_OK == senderResponse.getCodigoEstado()) {  
                    SMIMEMessageWrapper validatedVote =  
                            senderResponse.getSmimeMessage();
                    ReciboVoto reciboVoto = new ReciboVoto(
                                Respuesta.SC_OK, validatedVote, evento);
                    
                    senderResponse.setReciboVoto(reciboVoto);
                }
                respuesta = senderResponse;
            }
            return respuesta;
        } catch(Exception ex) {
            String msg = ex.getMessage() + " - nifFrom: " + nifFrom + 
                    " - hashVoto: " + evento.getHashCertificadoVotoBase64();
            logger.error(msg , ex);
            return new Respuesta(Respuesta.SC_ERROR, evento, msg);
        }  
    }
    
}