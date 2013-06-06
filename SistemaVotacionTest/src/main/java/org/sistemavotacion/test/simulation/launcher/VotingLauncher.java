package org.sistemavotacion.test.simulation.launcher;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sistemavotacion.worker.AccessRequestWorker;
import org.sistemavotacion.worker.SMIMESignedSenderWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.sistemavotacion.worker.VotingSystemWorkerType;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingLauncher implements Callable<Respuesta>, VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(VotingLauncher.class);
        
    public enum Worker implements VotingSystemWorkerType{
        ACCESS_REQUEST, VOTING}
    
    private Respuesta respuesta;
    private Evento evento;
    private String nifFrom;
    private SMIMEMessageWrapper smimeMessage;
    private PKCS10WrapperClient wrapperClient;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
        
    public VotingLauncher (Evento evento) 
            throws Exception {
        this.evento = evento; 
        nifFrom = evento.getUsuario().getNif();
    }
    
    @Override public Respuesta call() {
        try {
            KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(nifFrom);
            String asuntoMensaje = ContextoPruebas.INSTANCE.getString("voteMsgSubject") + 
                    evento.getEventoId();

            String anuladorVotoStr =  evento.getCancelVoteJSON().toString();
            File anuladorVoto = new File(Contexto.getUserDirPath(nifFrom,
                    ContextoPruebas.DEFAULTS.APPDIR)
                    + Contexto.CANCEL_VOTE_FILE + evento.getEventoId() + 
                    "_usu" + nifFrom + ".json");
            FileUtils.copyStreamToFile(new ByteArrayInputStream(
                    anuladorVotoStr.getBytes()), anuladorVoto);
            
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    mockDnie, ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.PASSWORD.toCharArray(),
                    ContextoPruebas.DNIe_SIGN_MECHANISM);
            
            String accessRequestStr = evento.getAccessRequestJSON().toString();
            smimeMessage = signedMailGenerator.genMimeMessage(nifFrom, 
                    evento.getControlAcceso().getNombreNormalizado(), 
                    accessRequestStr, asuntoMensaje, null);
            evento.setUrlSolicitudAcceso(ContextoPruebas.INSTANCE.
                    getURLAccessRequest());
            
            X509Certificate destinationCert = ContextoPruebas.INSTANCE.
                        getAccessControl().getCertificate();            
            new AccessRequestWorker(Worker.ACCESS_REQUEST, 
                    smimeMessage, evento, destinationCert, this).execute();
            
            countDownLatch.await();
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        }
        return getResult();
    }
    
    @Override public void processVotingSystemWorkerMsg(List<String> list) { }

    @Override public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
                " - worker: " + worker);
        respuesta = worker.getRespuesta();
        String msg = null;
        switch((Worker)worker.getType()) {
            case ACCESS_REQUEST:
                if (Respuesta.SC_OK == worker.getStatusCode()) {
                    try {
                        wrapperClient = ((AccessRequestWorker)worker).
                            getPKCS10WrapperClient();
                        String votoJSON = evento.getVoteJSON().toString();
                        String subject = ContextoPruebas.INSTANCE.getString("voteMsgSubject");
                        smimeMessage = wrapperClient.genMimeMessage(
                                evento.getHashCertificadoVotoBase64(), 
                                evento.getControlAcceso().getNombreNormalizado(),
                                votoJSON, subject, null);
                        String urlVoteService = ContextoPruebas.getURLVoto(
                            evento.getCentroControl().getServerURL()); 
                        new SMIMESignedSenderWorker(Worker.VOTING, 
                                smimeMessage, urlVoteService, wrapperClient.
                                getKeyPair(), null, this).execute();
                        return;
                    } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        respuesta.appendErrorMessage(ex.getMessage());
                    }
                }
                respuesta.setData("### ERROR - ACCESS_REQUEST from: " + nifFrom);
                break;                     
            case VOTING:
                if (Respuesta.SC_OK == worker.getStatusCode()) {  
                    try {
                        SMIMEMessageWrapper validatedVote = respuesta.getSmimeMessage();
                        ReciboVoto reciboVoto = new ReciboVoto(
                                Respuesta.SC_OK, validatedVote, evento);
                        respuesta.setReciboVoto(reciboVoto);
                        countDownLatch.countDown();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        respuesta.appendErrorMessage(ex.getMessage());
                    }
                }
                respuesta.setData("### ERROR - VOTING from: " + nifFrom);
                break;
            default:
                logger.debug("*** UNKNOWN WORKER: " + worker);
        }
        countDownLatch.countDown();
    }
        
    private Respuesta getResult() {
        if(respuesta != null) respuesta.setEvento(evento);
        return respuesta;
    }
    
}