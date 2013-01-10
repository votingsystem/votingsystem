package org.sistemavotacion.test.simulacion;

import java.io.File;
import java.security.KeyStore;
import java.util.concurrent.Callable;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.KeyStoreHelper;
import org.sistemavotacion.test.json.DeObjetoAJSON;
import org.sistemavotacion.test.modelo.InfoVoto;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class LanzadoraSolicitudAcceso  implements Callable<InfoVoto> {
    
    private static Logger logger = LoggerFactory.getLogger(LanzadoraSolicitudAcceso.class);

    private InfoVoto infoVoto;
    
    public LanzadoraSolicitudAcceso (InfoVoto infoVoto) 
            throws Exception {
        this.infoVoto = infoVoto;
    }
    
    
    @Override
    public InfoVoto call() throws Exception { 
        File file = new File(ContextoPruebas.getUserKeyStorePath(infoVoto.getFrom()));
        KeyStore mockDnie = KeyStoreHelper.crearMockDNIe(infoVoto.getFrom(), file,
                ContextoPruebas.getPrivateCredentialMockRaizDNIe());
        logger.info("userID: " + infoVoto.getFrom() + " - Creado directorio: " + file.getAbsolutePath());
        String asuntoMensaje = ContextoPruebas.ASUNTO_MENSAJE_SOLICITUD_ACCESO 
                        + infoVoto.getVoto().getEventoId();
        //Header header = new Header (Contexto.NOMBRE_ENCABEZADO_HASH, 
        //    infoVoto.getVoto().getHashCertificadoVotoBase64());
        String anuladorVotoStr = null;
        synchronized(this) {
             anuladorVotoStr = DeObjetoAJSON.obtenerAnuladorDeVotoJSON(infoVoto.getVoto());
        }
        File anuladorVotoTemp = FileUtils.getFileFromString(anuladorVotoStr);
        File anuladorVoto = new File(ContextoPruebas.getUserDirPath(infoVoto.getFrom())
                + ContextoPruebas.ANULACION_FILE + infoVoto.getVoto().getEventoId() + 
                "_usu" + infoVoto.getFrom() + ".json");
        FileUtils.copy(anuladorVotoTemp, anuladorVoto);
        PKCS10WrapperClient wrapperClient = new PKCS10WrapperClient(
                1024, "RSA", "SHA1withRSA", "BC", 
                infoVoto.getVoto().getControlAcceso().getServerURL(), 
                infoVoto.getVoto().getEventoId().toString(),
                infoVoto.getVoto().getHashCertificadoVotoHex());
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.VOTE_SIGN_MECHANISM);
        File solicitudAcceso = signedMailGenerator.genFile(infoVoto.getFrom(), 
                infoVoto.getVoto().getControlAcceso().getNombreNormalizado(), 
                DeObjetoAJSON.obtenerSolicitudAccesoJSON(
                    ContextoPruebas.getControlAcceso().getServerURL(),infoVoto.getVoto()),
                asuntoMensaje, null, SignedMailGenerator.Type.USER); 
        File recibo = new File(ContextoPruebas.getUserDirPath(infoVoto.getFrom())
                + ContextoPruebas.SOLICITUD_FILE + infoVoto.getVoto().getEventoId() + 
                "_usu" + infoVoto.getFrom() + ".p7m");
        FileUtils.copy(solicitudAcceso, recibo);
        logger.debug("call - infoVoto: " + infoVoto.getFrom() + " - Hash Solicitud Acceso: " + infoVoto.getVoto().getHashSolicitudAccesoBase64()
                + " - Solicitud Acceso: " + recibo.getAbsolutePath());
        HttpResponse response = Contexto.getHttpHelper().enviarSolicitudAcceso(
                wrapperClient.getPEMEncodedRequestCSR(), solicitudAcceso,
                ContextoPruebas.getURLSolicitudAcceso(
                ContextoPruebas.getControlAcceso().getServerURL()));
        infoVoto.setCodigoEstado(response.getStatusLine().getStatusCode());
        if (200 == response.getStatusLine().getStatusCode()) {
            wrapperClient.initSigner(EntityUtils.toByteArray(response.getEntity()));;
        } else {
            infoVoto.setCodigoEstado(response.getStatusLine().getStatusCode());
            infoVoto.setMensaje(EntityUtils.toString(response.getEntity()));
            wrapperClient = null;
        }
        EntityUtils.consume(response.getEntity());
        infoVoto.setPkcs10WrapperClient(wrapperClient);
        return infoVoto;
    }

    
}