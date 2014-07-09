package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.callable.MessageTimeStamper
import org.votingsystem.callable.RepresentativeDataSender
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.FileUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.StringUtils

import java.security.KeyStore
import java.security.MessageDigest
import java.util.concurrent.Callable

import static org.votingsystem.model.ContextVS.*

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class RepresentativeTestDataSender implements Callable<ResponseVS> {

    private static Logger logger = Logger.getLogger(RepresentativeTestDataSender.class);

    public static final String IMAGE_FILE_NAME   = "image";
    public static final String REPRESENTATIVE_DATA_FILE_NAME = "representativeData";

    private String representativeNIF;
    private File imageFile;

    public RepresentativeTestDataSender(String representativeNIF, File imageFile) throws Exception {
        this.representativeNIF = representativeNIF;
        this.imageFile = imageFile;
    }
    
    @Override  public ResponseVS call() throws Exception {
        KeyStore mockDnie = ContextVS.getInstance().generateKeyStore(representativeNIF);
        MessageDigest messageDigest = MessageDigest.getInstance(VOTING_DATA_DIGEST);
        byte[] resultDigest =  messageDigest.digest(FileUtils.getBytesFromFile(imageFile));
        String base64ResultDigestStr = new String(Base64.encode(resultDigest));
        String representativeDataStr = getRepresentativeDataJSON(representativeNIF, base64ResultDigestStr).toString();
        String toUser = StringUtils.getNormalized(ContextVS.getInstance().getAccessControl().getName());
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie,
                END_ENTITY_ALIAS, PASSWORD.toCharArray(), DNIe_SIGN_MECHANISM);
        String subject = ApplicationContextHolder.getInstance().getMessage("representativeRequestMsgSubject", null);
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(
                representativeNIF, toUser, representativeDataStr, subject , null);
        RepresentativeDataSender representativeDataSender = new RepresentativeDataSender(smimeMessage, imageFile,
                ContextVS.getInstance().getAccessControl().getRepresentativeServiceURL());
        ResponseVS responseVS = representativeDataSender.call();
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