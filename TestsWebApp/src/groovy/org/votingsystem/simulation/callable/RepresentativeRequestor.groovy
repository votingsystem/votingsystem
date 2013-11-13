package org.votingsystem.simulation.callable;

import java.io.File;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.bouncycastle.util.encoders.Base64;
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;

import org.votingsystem.model.ActorVS
import org.votingsystem.model.ResponseVS;
import org.apache.log4j.Logger;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.StringUtils;

import org.votingsystem.simulation.ApplicationContextHolder as ACH;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeRequestor implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(RepresentativeRequestor.class);

    private String representativeNIF;
        
    public RepresentativeRequestor (String representativeNIF) 
            throws Exception {
        this.representativeNIF = representativeNIF;
    }
    
    @Override  public ResponseVS call() throws Exception {
        KeyStore mockDnie = SimulationContext.INSTANCE.crearMockDNIe(representativeNIF);
        File selectedImage = File.createTempFile("representativeImage", ".png");
        log.info(" - selectedImage.getAbsolutePath(): " + selectedImage.getAbsolutePath());
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

        ActorVS controlAcceso = Contexto.INSTANCE.getAccessControl();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                mockDnie, SimulationContext.DEFAULTS.END_ENTITY_ALIAS, 
                SimulationContext.PASSWORD.toCharArray(),
                SimulationContext.DNIe_SIGN_MECHANISM);
        
        String subject = SimulationContext.INSTANCE.
                getString("representativeRequestMsgSubject");

        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(
                representativeNIF, toUser, representativeDataJSON, subject , null);
        
        String urlService = SimulationContext.INSTANCE.getUrlRepresentativeService();
        
        RepresentativeDataSender worker = new RepresentativeDataSender( 
                smimeMessage, selectedImage, urlService, SimulationContext.INSTANCE.
                getAccessControl().getCertificate());
        ResponseVS respuesta = worker.call();
        if (ResponseVS.SC_OK == respuesta.getStatusCode()) {
            respuesta.setMensaje(representativeNIF);
        } else respuesta.appendErrorMessage(" - From nif: " + representativeNIF);
        return respuesta;
    }
    
    public static String getRepresentativeDataJSON(String representativeNIF,
            String imageDigestStr) {
        log.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_DATA");
        map.put("representativeInfo", " --- contenido del representante -" + representativeNIF);
        map.put("base64ImageHash", imageDigestStr);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }

}