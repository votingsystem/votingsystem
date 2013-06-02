package org.sistemavotacion.test.simulation.launcher;

import java.io.File;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.KeyStoreHelper;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.TimeStampWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class TimeStampLauncher implements Callable<Respuesta>, 
        VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(TimeStampLauncher.class);

    private static final int TIME_STAMP_WORKER = 1;
    
    private SMIMEMessageWrapper documentSMIME;
    private String requestNIF;
    
    private String urlTimeStampServer;
    
    private Respuesta respuesta;
        
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time
    
        
    private String digestTokenStr = null;
    private String timeStampTokenStr = null;
    private String timeStampRequestStr;
    
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;
    
    public TimeStampLauncher (String requestNIF, String urlTimeStampServer) 
            throws Exception {
        this.requestNIF = requestNIF;
        this.urlTimeStampServer = urlTimeStampServer;
    }
        
    @Override
    public Respuesta call() throws Exception {
        //File file = new File(ContextoPruebas.getUserKeyStorePath(requestNIF));
        File file = File.createTempFile("TimeStampTestKeyStore" + requestNIF, ".jks");
        file.deleteOnExit();
        KeyStore mockDnie = KeyStoreHelper.crearMockDNIe(requestNIF, file,
                ContextoPruebas.getPrivateCredentialRaizAutoridad());
        logger.info("requestNIF: " + requestNIF + " - Dirs: " + file.getAbsolutePath());

        ActorConIP controlAcceso = ContextoPruebas.getControlAcceso();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.END_ENTITY_ALIAS, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        
        String subject = ContextoPruebas.getString("timeStampMsgSubject");
        
        documentSMIME = signedMailGenerator.genMimeMessage(
                requestNIF, toUser, getRequestDataJSON(), subject , null);

        new TimeStampWorker(TIME_STAMP_WORKER, urlTimeStampServer, this, 
                documentSMIME.getTimeStampRequest(),
                ContextoPruebas.getControlAcceso().getTimeStampCert()).execute();
        
        countDownLatch.await();
        return getResult();
    }
        
    public static String getRequestDataJSON() {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "TIMESTAMP_TEST");
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }

    @Override
    public void process(List<String> messages) {
        for(String message : messages)  {
            logger.debug("process -> " + message);
        }
    }

    @Override
    public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
         " - worker: " + worker.getClass().getSimpleName() + 
         " - workerId:" + worker.getId());
        respuesta = new Respuesta();
        respuesta.setCodigoEstado(worker.getStatusCode());
        switch(worker.getId()) {
            case TIME_STAMP_WORKER:
                if(Respuesta.SC_OK != worker.getStatusCode()) {
                    String msg = "showResult - ERROR obteniendo sello de tiempo";
                    try {
                        TimeStampToken tst = ((TimeStampWorker)worker).getTimeStampToken();
                        byte[]  digestToken = ((TimeStampWorker)worker).getDigestToken();
                        if(digestToken != null)
                            digestTokenStr = new String(Base64.encode(digestToken));
                        byte[] timeStampRequestBytes = ((TimeStampWorker)worker).getTimeStampRequest().getEncoded();
                        if(timeStampRequestBytes != null)
                            timeStampRequestStr =  new String(Base64.encode(timeStampRequestBytes));
                        if(tst != null)
                            timeStampTokenStr =  new String(Base64.encode(tst.getEncoded()));
                        logger.debug(" - timeStampRequestStr : " + timeStampRequestStr); 
                        logger.debug(" - timeStampTokenStr : " + timeStampTokenStr); 
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                    logger.debug(msg); 
                    respuesta.setMensaje(msg);
                }
                countDownLatch.countDown();
                break;
            default:
                logger.debug("*** UNKNOWN WORKER ID: '" + worker.getId() + "'");
        }
    }


    private Respuesta getResult() {
        return respuesta;
    }
}