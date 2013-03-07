package org.sistemavotacion.test.simulacion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.json.DeObjetoAJSON;
import org.sistemavotacion.test.modelo.SolicitudAcceso;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class LanzadoraAnulacionSolicitudAcceso  implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(LanzadoraAnulacionSolicitudAcceso.class);

    private SolicitudAcceso solicitudAcceso;
    
    public LanzadoraAnulacionSolicitudAcceso (SolicitudAcceso solicitudAcceso) 
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
             anulador = signedMailGenerator.genFile(solicitudAcceso.getUserNif(), 
                ContextoPruebas.getControlAcceso().getNombreNormalizado(), 
                DeObjetoAJSON.obtenerAnuladorDeVotoJSON(solicitudAcceso),
                asuntoMensaje, null, SignedMailGenerator.Type.USER, anulador); 
        }
        HttpResponse response = Contexto.getHttpHelper().enviarArchivoFirmado(
            anulador, ContextoPruebas.getURLAnulacionVoto(
            		ContextoPruebas.getControlAcceso().getServerURL()));
        if (Respuesta.SC_OK == response.getStatusLine().getStatusCode()) {                    
            SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(null,
                    new ByteArrayInputStream(EntityUtils.toByteArray(response.getEntity())),
                    "ReciboAnulacionVoto");
            respuesta = new Respuesta(response.getStatusLine().getStatusCode(), 
                    dnieMimeMessage, ContextoPruebas.INSTANCIA.getSessionPKIXParameters());
        } else {
            respuesta = new Respuesta(
                    response.getStatusLine().getStatusCode(), 
                    EntityUtils.toString(response.getEntity()));
        }
        respuesta.setObjeto(solicitudAcceso);
        EntityUtils.consume(response.getEntity());
        return respuesta;
    }

    
}