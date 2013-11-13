package org.votingsystem.simulation.callable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import org.votingsystem.model.EventVS
import org.votingsystem.model.ResponseVS; 
import org.votingsystem.signature.util.PKCS10WrapperClient;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.simulation.ContextService;
import org.apache.log4j.Logger;
import org.votingsystem.util.FileUtils;

import org.votingsystem.simulation.ApplicationContextHolder as ACH;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestorTest implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(AccessRequestorTest.class);
    
    private ResponseVS respuesta;
    private EventVS evento;
    private String nifFrom;
    private String urlAccessRequest = null;
        
    public AccessRequestorTest (EventVS evento) throws Exception {
        this.evento = evento; 
        this.nifFrom = evento.getUsuario().getNif();
        urlAccessRequest = SimulationContext.INSTANCE.getURLAccessRequest();
        evento.setUrlSolicitudAcceso(urlAccessRequest);
    }
    
    @Override public ResponseVS call() throws Exception { 
        File mockDnieFile = new File(Contexto.getUserKeyStorePath(nifFrom,
                SimulationContext.DEFAULTS.APPDIR));
        byte[] mockDnieBytes = FileUtils.getBytesFromFile(mockDnieFile);
        log.info("userID: " + nifFrom + 
                " - mockDnieFile: " + mockDnieFile.getAbsolutePath());
        String subject = SimulationContext.INSTANCE.getString(
                "accessRequestMsgSubject") + evento.getEventoId();
        
        String anuladorVotoStr = evento.getCancelVoteJSON().toString();
        File anuladorVoto = new File(Contexto.getUserDirPath(nifFrom,
                SimulationContext.DEFAULTS.APPDIR)
                + Contexto.CANCEL_VOTE_FILE + evento.getEventoId() + 
                "_usu" + nifFrom + ".json");
        FileUtils.copyStreamToFile(new ByteArrayInputStream(
                anuladorVotoStr.getBytes()), anuladorVoto);

        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnieBytes, 
                SimulationContext.DEFAULTS.END_ENTITY_ALIAS, 
                SimulationContext.PASSWORD.toCharArray(),
                SimulationContext.DNIe_SIGN_MECHANISM);
        String accessRequestStr = evento.getAccessRequestJSON().toString();
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(nifFrom, 
                evento.getControlAcceso().getNombreNormalizado(), 
                accessRequestStr, subject, null);
        
        X509Certificate accesRequestCert = SimulationContext.INSTANCE.
            getAccessControl().getCertificate();
        AccessRequestor accessRequestor = new AccessRequestor(
                smimeMessage, evento, accesRequestCert);
        respuesta = accessRequestor.call();
        if (ResponseVS.SC_OK == respuesta.getStatusCode()) {
            try {
                PKCS10WrapperClient wrapperClient = accessRequestor.
                        getPKCS10WrapperClient();
                String votoJSON = evento.getVoteJSON().toString();   
                smimeMessage = wrapperClient.genMimeMessage(
                        evento.getHashCertificadoVotoBase64(), 
                        evento.getControlAcceso().getNombreNormalizado(),
                        votoJSON, "[VOTO]", null);
            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
                respuesta.appendErrorMessage(ex.getMessage());
                respuesta.setData("ERROR_ACCESS_REQUEST from: " + nifFrom);
            }
        } else {
            respuesta.setData("ERROR_ACCESS_REQUEST from: " + nifFrom);
        }
        respuesta.setEvento(evento);
        return respuesta;
    }
    
}