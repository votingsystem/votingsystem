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
import org.sistemavotacion.test.json.DeObjetoAJSON;
import org.sistemavotacion.test.modelo.SolicitudAcceso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class CancelAccessRequestLauncher  implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(CancelAccessRequestLauncher.class);

    private SolicitudAcceso solicitudAcceso;
    
    public CancelAccessRequestLauncher (SolicitudAcceso solicitudAcceso) 
            throws Exception {
        this.solicitudAcceso = solicitudAcceso;
    }
    
    
    @Override
    public Respuesta call() throws Exception {
        Respuesta respuesta = null;
        File file = new File(ContextoPruebas.getUserKeyStorePath(solicitudAcceso.getUserNif()));
        KeyStore mockDnie = KeyStoreUtil.getKeyStoreFromFile(
                file, ContextoPruebas.PASSWORD.toCharArray());
        logger.info("userID: " + solicitudAcceso.getUserNif());
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.VOTE_SIGN_MECHANISM);
        String asuntoMensaje = ContextoPruebas.ASUNTO_MENSAJE_ANULACION_SOLICITUD_ACCESO
                        + solicitudAcceso.getEventoId();
        File anulador = new File(ContextoPruebas.getUserDirPath(solicitudAcceso.getUserNif())
                + ContextoPruebas.ANULACION_FIRMADA_FILE + solicitudAcceso.getEventoId() + 
                "_usu" + solicitudAcceso.getUserNif() + ".p7m");;
        synchronized(this) {
            MimeMessage mimeMessage = signedMailGenerator.genMimeMessage(
               solicitudAcceso.getUserNif(), 
                ContextoPruebas.getControlAcceso().getNombreNormalizado(), 
                DeObjetoAJSON.obtenerAnuladorDeVotoJSON(solicitudAcceso),
                asuntoMensaje, null);
            mimeMessage.writeTo(new FileOutputStream(anulador));
        }
        respuesta = Contexto.getHttpHelper().sendFile(anulador, 
                Contexto.SIGNED_CONTENT_TYPE, ContextoPruebas.getURLAnulacionVoto(
                ContextoPruebas.getControlAcceso().getServerURL()));
        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {                    
            SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(null,
                    new ByteArrayInputStream(respuesta.getMensaje().getBytes()),
                    "ReciboAnulacionVoto");
            respuesta = new Respuesta(respuesta.getCodigoEstado(), 
                    dnieMimeMessage, ContextoPruebas.INSTANCIA.getSessionPKIXParameters());
        }
        respuesta.setObjeto(solicitudAcceso);
        return respuesta;
    }

    
}