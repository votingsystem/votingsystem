package org.sistemavotacion.test.simulation.callable;

import java.io.File;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.RepresentativeRequestWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeRequestor implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(RepresentativeRequestor.class);

    private String representativeNIF;
    private File selectedImage;
    private Respuesta respuesta;
        
    public RepresentativeRequestor (String representativeNIF) 
            throws Exception {
        this.representativeNIF = representativeNIF;
    }
    
    
    @Override
    public Respuesta call() throws Exception {
        Respuesta respuesta = null;
        KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(representativeNIF);
        selectedImage = File.createTempFile("representativeImage", ".png");
        logger.info(" - selectedImage.getAbsolutePath(): " + selectedImage.getAbsolutePath());
        selectedImage.deleteOnExit();
        selectedImage =   FileUtils.copyStreamToFile(getClass().
                getResourceAsStream("/images/Group_128x128.png"), selectedImage);
        byte[] imageFileBytes = FileUtils.getBytesFromFile(selectedImage);
        MessageDigest messageDigest = MessageDigest.getInstance(
                            Contexto.VOTING_DATA_DIGEST);
        byte[] resultDigest =  messageDigest.digest(imageFileBytes);
        String base64ResultDigestStr = new String(Base64.encode(resultDigest));
        String representativeDataJSON = getRepresentativeDataJSON(
                representativeNIF, base64ResultDigestStr);

        ActorConIP controlAcceso = Contexto.INSTANCE.getAccessControl();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                mockDnie, ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.DNIe_SIGN_MECHANISM);
        
        String subject = ContextoPruebas.INSTANCE.
                getString("representativeRequestMsgSubject");

        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(
                representativeNIF, toUser, representativeDataJSON, subject , null);
        
        String urlService = ContextoPruebas.INSTANCE.getUrlRepresentativeService();
        
        RepresentativeRequestWorker worker = new RepresentativeRequestWorker(null, 
                smimeMessage, selectedImage, urlService, ContextoPruebas.INSTANCE.
                getAccessControl().getCertificate(),null);
        worker.execute();
        respuesta = worker.get();
        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            respuesta.setMensaje(representativeNIF);
        }
        return respuesta;
    }
   
    public static String getRepresentativeDataJSON(String representativeNIF,
            String imageDigestStr) {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_DATA");
        map.put("representativeInfo", " --- contenido del representante -" + representativeNIF);
        map.put("base64ImageHash", imageDigestStr);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }

    private Respuesta getResult() {
        return respuesta;
    }
    
}