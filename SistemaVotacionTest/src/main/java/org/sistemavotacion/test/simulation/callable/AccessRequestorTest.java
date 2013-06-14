package org.sistemavotacion.test.simulation.callable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta; 
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestorTest implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(AccessRequestorTest.class);
    
    private Respuesta respuesta;
    private Evento evento;
    private String nifFrom;
    private String urlAccessRequest = null;
        
    public AccessRequestorTest (Evento evento) throws Exception {
        this.evento = evento; 
        this.nifFrom = evento.getUsuario().getNif();
        urlAccessRequest = ContextoPruebas.INSTANCE.getURLAccessRequest();
        evento.setUrlSolicitudAcceso(urlAccessRequest);
    }
    
    @Override public Respuesta call() throws Exception { 
        File mockDnieFile = new File(Contexto.getUserKeyStorePath(nifFrom,
                ContextoPruebas.DEFAULTS.APPDIR));
        byte[] mockDnieBytes = FileUtils.getBytesFromFile(mockDnieFile);
        logger.info("userID: " + nifFrom + 
                " - mockDnieFile: " + mockDnieFile.getAbsolutePath());
        String subject = ContextoPruebas.INSTANCE.getString(
                "accessRequestMsgSubject") + evento.getEventoId();
        
        String anuladorVotoStr = evento.getCancelVoteJSON().toString();
        File anuladorVoto = new File(Contexto.getUserDirPath(nifFrom,
                ContextoPruebas.DEFAULTS.APPDIR)
                + Contexto.CANCEL_VOTE_FILE + evento.getEventoId() + 
                "_usu" + nifFrom + ".json");
        FileUtils.copyStreamToFile(new ByteArrayInputStream(
                anuladorVotoStr.getBytes()), anuladorVoto);

        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnieBytes, 
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        String accessRequestStr = evento.getAccessRequestJSON().toString();
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(nifFrom, 
                evento.getControlAcceso().getNombreNormalizado(), 
                accessRequestStr, subject, null);
        
        X509Certificate accesRequestCert = ContextoPruebas.INSTANCE.
            getAccessControl().getCertificate();
        AccessRequestor accessRequestor = new AccessRequestor(
                smimeMessage, evento, accesRequestCert);
        respuesta = accessRequestor.call();
        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            try {
                PKCS10WrapperClient wrapperClient = accessRequestor.
                        getPKCS10WrapperClient();
                String votoJSON = evento.getVoteJSON().toString();   
                smimeMessage = wrapperClient.genMimeMessage(
                        evento.getHashCertificadoVotoBase64(), 
                        evento.getControlAcceso().getNombreNormalizado(),
                        votoJSON, "[VOTO]", null);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
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