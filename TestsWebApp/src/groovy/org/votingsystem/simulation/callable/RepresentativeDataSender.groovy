package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.simulation.ContextService
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.StringUtils

import java.security.KeyStore
import java.security.MessageDigest
import java.util.concurrent.Callable

import static org.votingsystem.simulation.ContextService.*
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeDataSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(RepresentativeDataSender.class);

    public static final String IMAGE_FILE_NAME   = "image";
    public static final String REPRESENTATIVE_DATA_FILE_NAME = "representativeData";

    private String representativeNIF;
    private ContextService contextService;
    private byte[] imageBytes;
        
    public RepresentativeDataSender(String representativeNIF, byte[] imageBytes) throws Exception {
        this.representativeNIF = representativeNIF;
        this.imageBytes = imageBytes;
        this.contextService  = ApplicationContextHolder.getSimulationContext();
    }
    
    @Override  public ResponseVS call() throws Exception {
        KeyStore mockDnie = contextService.generateTestDNIe(representativeNIF);
        MessageDigest messageDigest = MessageDigest.getInstance(VOTING_DATA_DIGEST);
        byte[] resultDigest =  messageDigest.digest(imageBytes);
        String base64ResultDigestStr = new String(Base64.encode(resultDigest));
        String representativeDataStr = getRepresentativeDataJSON(representativeNIF, base64ResultDigestStr).toString();
        String toUser = StringUtils.getCadenaNormalizada(contextService.getAccessControl().getName());
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie,
                END_ENTITY_ALIAS, PASSWORD.toCharArray(), DNIe_SIGN_MECHANISM);
        String subject = contextService.getMessage("representativeRequestMsgSubject", null);
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(
                representativeNIF, toUser, representativeDataStr, subject , null);
        String serviceURL = contextService.getAccessControl().getServerURL() + "/representative";
        MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage);
        ResponseVS responseVS = timeStamper.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        smimeMessage = timeStamper.getSmimeMessage();
        byte[] representativeEncryptedDataBytes = Encryptor.encryptSMIME(smimeMessage,
                contextService.getAccessControl().getX509Certificate());
        Map<String, Object> fileMap = new HashMap<String, Object>();
        String representativeDataFileName = REPRESENTATIVE_DATA_FILE_NAME + ":" + ContentTypeVS.SIGNED_AND_ENCRYPTED;
        fileMap.put(representativeDataFileName, representativeEncryptedDataBytes);
        fileMap.put(IMAGE_FILE_NAME, imageBytes);
        responseVS = HttpHelper.getInstance().sendObjectMap(fileMap, serviceURL);
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            responseVS.setMessage(representativeNIF);
        } else responseVS.appendMessage("RepresentativeDataSender From nif: " + representativeNIF);
        return responseVS;
    }
    
    public static JSONObject getRepresentativeDataJSON(String representativeNIF, String imageDigestStr) {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_DATA");
        map.put("representativeInfo", " --- contenido del representante -" + representativeNIF);
        map.put("base64ImageHash", imageDigestStr);
        map.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(map);
    }

}