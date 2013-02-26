package org.sistemavotacion.test.simulacion;

import java.io.File;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.tsp.TimeStampRequest;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.KeyStoreHelper;
import org.sistemavotacion.test.json.DeObjetoAJSON;
import org.sistemavotacion.test.modelo.InfoVoto;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.sistemavotacion.test.ContextoPruebas.*;
import org.sistemavotacion.worker.TimeStampWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class LanzadoraSolicitudAcceso 
    implements Callable<InfoVoto>, VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(LanzadoraSolicitudAcceso.class);

    private InfoVoto infoVoto;
    private SMIMEMessageWrapper timeStampedDocument;
    private final CountDownLatch timeStampLatch = new CountDownLatch(1); // just one time

    
    public LanzadoraSolicitudAcceso (InfoVoto infoVoto) 
            throws Exception {
        this.infoVoto = infoVoto;
    }
    
    
    @Override
    public InfoVoto call() throws Exception { 
        File file = new File(ContextoPruebas.getUserKeyStorePath(infoVoto.getFrom()));
        KeyStore mockDnie = KeyStoreHelper.crearMockDNIe(infoVoto.getFrom(), file,
                ContextoPruebas.getPrivateCredentialRaizAutoridad());
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
        PKCS10WrapperClient wrapperClient = new PKCS10WrapperClient(KEY_SIZE, SIG_NAME, 
                VOTE_SIGN_MECHANISM, PROVIDER, 
                infoVoto.getVoto().getControlAcceso().getServerURL(), 
                infoVoto.getVoto().getEventoId().toString(),
                infoVoto.getVoto().getHashCertificadoVotoHex());
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        File solicitudAcceso = new File(ContextoPruebas.getUserDirPath(infoVoto.getFrom())
                + ContextoPruebas.SOLICITUD_FILE + infoVoto.getVoto().getEventoId() + 
                "_usu" + infoVoto.getFrom() + ".p7m");
        solicitudAcceso = signedMailGenerator.genFile(infoVoto.getFrom(), 
                infoVoto.getVoto().getControlAcceso().getNombreNormalizado(), 
                DeObjetoAJSON.obtenerSolicitudAccesoJSON(
                    ContextoPruebas.getControlAcceso().getServerURL(),infoVoto.getVoto()),
                asuntoMensaje, null, SignedMailGenerator.Type.USER, solicitudAcceso); 
        TimeStampWorker timeStampWorker = getTimeStampedDocument(solicitudAcceso);
        timeStampWorker.get(30, TimeUnit.SECONDS);
        if(Respuesta.SC_OK != timeStampWorker.getStatusCode()) {
            infoVoto.setCodigoEstado(timeStampWorker.getStatusCode());
            infoVoto.setMensaje(timeStampWorker.getMessage());
            return infoVoto;
        }
        solicitudAcceso = timeStampedDocument.setTimeStampToken(timeStampWorker);
        logger.debug("call - infoVoto: " + infoVoto.getFrom() + " - Hash Solicitud Acceso: " + infoVoto.getVoto().getHashSolicitudAccesoBase64()
                + " - Solicitud Acceso: " + solicitudAcceso.getAbsolutePath());
        HttpResponse response = null;
        try {
            response = Contexto.getHttpHelper().enviarSolicitudAcceso(
                wrapperClient.getPEMEncodedRequestCSR(), solicitudAcceso,
                ContextoPruebas.getURLSolicitudAcceso(
                ContextoPruebas.getControlAcceso().getServerURL()));
        } catch(ConnectionPoolTimeoutException ex) {
            logger.debug("");
            logger.error(ex.getMessage(), ex);
            infoVoto.setCodigoEstado(Respuesta.SC_REQUEST_TIMEOUT);
            return infoVoto;
        }
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

    
    private TimeStampWorker getTimeStampedDocument(File document) {
        if(document == null) return null;
        TimeStampWorker timeStampWorker = null;
        try {
            timeStampedDocument = new SMIMEMessageWrapper(null,document);
            String urlTimeStampServer = ContextoPruebas.getUrlTimeStampServer(
                    ContextoPruebas.getControlAcceso().getServerURL());
            TimeStampRequest timeStampRequest = timeStampedDocument.getTimeStampRequest(TIMESTAMP_HASH);
            timeStampWorker = new TimeStampWorker(null, urlTimeStampServer,
                    this, timeStampedDocument.getTimeStampRequest(TIMESTAMP_HASH));
            timeStampWorker.execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            return timeStampWorker;
        }
    }
    
    @Override
    public void process(List<String> list) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showResult(VotingSystemWorker vsw) {
        timeStampLatch.countDown();

    }

    
}