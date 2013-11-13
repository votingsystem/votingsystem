package org.votingsystem.simulation.callable;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import org.votingsystem.model.EventVS
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.VoteVS
import org.votingsystem.signature.util.PKCS10WrapperClient;
import org.votingsystem.simulation.ContextService;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.simulation.ApplicationContextHolder as ACH;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Voter implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(Voter.class);
   
    private EventVS evento;
    private String nifFrom;
	private ContextService contextService = null;
        
    public Voter (EventVS evento) throws Exception {
        this.evento = evento; 
        nifFrom = evento.getUsuario().getNif();
		contextService = ACH.getSimulationContext();
    }
    
    @Override public ResponseVS call() {
        try {
            KeyStore mockDnie = contextService(nifFrom);
            String msgSubject = ACH.getMessage("accessRequestMsgSubject", evento.getEventoId());

            /*String cancelVoteStr =  evento.getCancelVoteJSON().toString();
            File anuladorVoto = new File(Contexto.getUserDirPath(nifFrom,
                    SimulationContext.DEFAULTS.APPDIR)
                    + Contexto.CANCEL_VOTE_FILE + evento.getEventoId() + 
                    "_usu" + nifFrom + ".json");
            FileUtils.copyStreamToFile(new ByteArrayInputStream(
                    cancelVoteStr.getBytes()), anuladorVoto);*/
            
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    mockDnie, contextService.END_ENTITY_ALIAS, 
                    contextService.PASSWORD.toCharArray(),
                    contextService.DNIe_SIGN_MECHANISM);
            
            String accessRequestStr = new JSONObject(evento.getAccessRequestDataMap()).toString();
            SMIMEMessageWrapper smimeMessage = signedMailGenerator.
                    genMimeMessage(nifFrom, 
                    evento.getControlAcceso().getNombreNormalizado(), 
                    accessRequestStr, msgSubject, null);
			String accessrequestURL = contextService.getAccessControl().getServerURL() + "/solicitudAcceso";
            evento.setUrlSolicitudAcceso(accessrequestURL);
            
            X509Certificate destinationCert = contextService.getAccessControl().getCertificate();            
            AccessRequestor accessWorker = new AccessRequestor( 
                    smimeMessage, evento, destinationCert);
            
            ResponseVS responseVS = accessWorker.call();
            responseVS.setEventVS(evento);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                PKCS10WrapperClient wrapperClient = accessWorker.
                    getPKCS10WrapperClient();
                String votoJSON = new JSONObject(evento.getVoteDataMap()).toString();
                String subject = ACH.getMessage("voteMsgSubject", evento.getEventoId());
                try {
                    smimeMessage = wrapperClient.genMimeMessage(
                        evento.getHashCertificadoVotoBase64(), 
                        evento.getControlAcceso().getNombreNormalizado(),
                        votoJSON, subject, null);
                } catch(Exception ex) {
                    log.error(ex.getMessage() + " - ResponseVS codigoEstado:" + 
                    responseVS.getStatusCode(), ex);
                }
                
                responseVS = sendVote(smimeMessage, wrapperClient.
                        getKeyPair());
                if (ResponseVS.SC_OK != responseVS.getStatusCode()) { 
                    log.debug("============= SEGUNDO INTENTO DE VOTO " + 
                          " - mensaje: " + responseVS.getMensaje());
                    respuesta = sendVote(smimeMessage, wrapperClient.getKeyPair());
                }
                /*String urlVoteService = SimulationContext.getURLVoto(
                    evento.getCentroControl().getServerURL()); 
                SMIMESignedSender sender= new SMIMESignedSender(
                        null, smimeMessage, urlVoteService, wrapperClient.
                        getKeyPair(), null);
                ResponseVS senderResponse = sender.call();
                senderResponse.setEvento(evento);
                if (ResponseVS.SC_OK == senderResponse.getStatusCode()) {  
                    SMIMEMessageWrapper validatedVote =  
                            senderResponse.getSmimeMessage();
                    ReciboVoto reciboVoto = new ReciboVoto(
                                ResponseVS.SC_OK, validatedVote, evento);
                    
                    senderResponse.setReciboVoto(reciboVoto);
                }
                respuesta = senderResponse;*/
            }
            responseVS.setSmimeMessage(smimeMessage);
            return respuesta;
        } catch(Exception ex) {
            String msg = ex.getMessage() + " - nifFrom: " + nifFrom + 
                    " - hashVoto: " + evento.getHashCertificadoVotoBase64();
            log.error(msg , ex);
            return new ResponseVS(ResponseVS.SC_ERROR, evento, msg);
        }  
    }
    
    private ResponseVS sendVote(
            SMIMEMessageWrapper smimeMessage, KeyPair keyPair) throws Exception {
        String urlVoteService = SimulationContext.getURLVoto(
            evento.getCentroControl().getServerURL()); 
        SMIMESignedSender sender= new SMIMESignedSender(
                null, smimeMessage, urlVoteService, keyPair, null);
        ResponseVS senderResponse = sender.call();
        senderResponse.setEvento(evento);
        if (ResponseVS.SC_OK == senderResponse.getStatusCode()) {  
            SMIMEMessageWrapper validatedVote =  
                    senderResponse.getSmimeMessage();
            VoteVS reciboVoto = new VoteVS(
                        ResponseVS.SC_OK, validatedVote, evento);

            senderResponse.setReciboVoto(reciboVoto);
        }
        return senderResponse;
    }
    
}