package org.sistemavotacion.test.simulation.launcher;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.concurrent.Callable;
import javax.mail.internet.MimeMessage;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.AccessRequestBackup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class CancelAccessRequestLauncher  implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(CancelAccessRequestLauncher.class);

    private AccessRequestBackup request;
    
    public CancelAccessRequestLauncher (AccessRequestBackup request) 
            throws Exception {
        this.request = request;
    }
    
    
    @Override
    public Respuesta call() throws Exception {
        Respuesta respuesta = null;
        String nif = request.getUsuario().getNif();
        File file = new File(ContextoPruebas.getUserKeyStorePath(nif));
        KeyStore mockDnie = KeyStoreUtil.getKeyStoreFromFile(
                file, ContextoPruebas.PASSWORD.toCharArray());
        logger.info("nif: " + nif);
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.VOTE_SIGN_MECHANISM);
        String subject = ContextoPruebas.INSTANCE.getString(
                "cancelAccessRequestMsgSubject") + request.getEventoId();
                        ;
        File anulador = new File(ContextoPruebas.getUserDirPath(nif) + 
                ContextoPruebas.ANULACION_FIRMADA_FILE + request.getEventoId() + 
                "_usu" + nif + ".p7m");
        synchronized(this) {
            MimeMessage mimeMessage = signedMailGenerator.genMimeMessage(
                nif,  ContextoPruebas.INSTANCE.getControlAcceso().getNombreNormalizado(), 
                request.toJSON().toString(),
                subject, null);
            mimeMessage.writeTo(new FileOutputStream(anulador));
        }
        respuesta = Contexto.INSTANCE.getHttpHelper().sendFile(anulador, 
                Contexto.SIGNED_CONTENT_TYPE, 
                ContextoPruebas.INSTANCE.getURLAnulacionVoto());
        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {                    
            SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(null,
                    new ByteArrayInputStream(respuesta.getMensaje().getBytes()),
                    "ReciboAnulacionVoto");
            respuesta = new Respuesta(respuesta.getCodigoEstado(), 
                    dnieMimeMessage, ContextoPruebas.INSTANCE.getSessionPKIXParameters());
        }
        respuesta.setData(request);
        return respuesta;
    }

    
}