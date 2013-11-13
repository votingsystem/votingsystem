package org.votingsystem.simulation.callable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.concurrent.Callable;
import javax.mail.internet.MimeMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.simulation.model.AccessRequestBackup
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.smime.SignedMailValidator;
import org.apache.log4j.Logger;

import org.votingsystem.simulation.ApplicationContextHolder as ACH;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class CancelAccessRequestRequestor  implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(CancelAccessRequestRequestor.class);

    private AccessRequestBackup request;
    
    public CancelAccessRequestRequestor (AccessRequestBackup request) 
            throws Exception {
        this.request = request;
    }
    
    
    @Override
    public ResponseVS call() throws Exception {
        ResponseVS respuesta = null;
        String nif = request.getUsuario().getNif();
        File file = new File(Contexto.getUserKeyStorePath(nif, 
                SimulationContext.DEFAULTS.APPDIR));
        KeyStore mockDnie = KeyStoreUtil.getKeyStoreFromFile(
                file, SimulationContext.PASSWORD.toCharArray());
        log.info("nif: " + nif);
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                SimulationContext.DEFAULTS.END_ENTITY_ALIAS, 
                SimulationContext.PASSWORD.toCharArray(),
                SimulationContext.VOTE_SIGN_MECHANISM);
        String subject = SimulationContext.INSTANCE.getString(
                "cancelAccessRequestMsgSubject") + request.getEventoId();
                        ;
        File anulador = new File(Contexto.getUserDirPath(nif, 
                SimulationContext.DEFAULTS.APPDIR) + 
                Contexto.CANCEL_VOTE_FILE + request.getEventoId() + 
                "_usu" + nif + ".p7m");
        synchronized(this) {
            MimeMessage mimeMessage = signedMailGenerator.genMimeMessage(
                nif,  SimulationContext.INSTANCE.getAccessControl().getNombreNormalizado(), 
                request.toJSON().toString(),
                subject, null);
            mimeMessage.writeTo(new FileOutputStream(anulador));
        }
        respuesta = Contexto.INSTANCE.getHttpHelper().sendFile(anulador, 
                Contexto.SIGNED_CONTENT_TYPE, 
                SimulationContext.INSTANCE.getURLAnulacionVoto());
        if (ResponseVS.SC_OK == respuesta.getStatusCode()) {                    
            SMIMEMessageWrapper mimeMessage = new SMIMEMessageWrapper(null,
                    new ByteArrayInputStream(respuesta.getMensaje().getBytes()),
                    "ReciboAnulacionVoto");

            SignedMailValidator.ValidationResult validationResult = mimeMessage.verify(
                    SimulationContext.INSTANCE.getSessionPKIXParameters());        
            if (!validationResult.isValidSignature()) {
                log.error("Error validating receipt");
            } 
            respuesta.setData(request);
        }
        return respuesta;
    }
    
}