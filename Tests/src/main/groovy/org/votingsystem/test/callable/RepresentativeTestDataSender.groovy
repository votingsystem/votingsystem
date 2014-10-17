package org.votingsystem.test.callable

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.callable.RepresentativeDataSender
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureService
import org.votingsystem.util.FileUtils

import java.security.MessageDigest
import java.util.concurrent.Callable

import static org.votingsystem.model.ContextVS.VOTING_DATA_DIGEST

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class RepresentativeTestDataSender implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(RepresentativeTestDataSender.class);

    public static final String IMAGE_FILE_NAME   = "image";
    public static final String REPRESENTATIVE_DATA_FILE_NAME = "representativeData";

    private String representativeNIF;
    private byte[] imageBytes;

    public RepresentativeTestDataSender(String representativeNIF, byte[] imageBytes) throws Exception {
        this.representativeNIF = representativeNIF;
        this.imageBytes = imageBytes;
    }
    
    @Override  public ResponseVS call() throws Exception {
        String subject = "Message from RepresentativeTestDataSender";
        MessageDigest messageDigest = MessageDigest.getInstance(VOTING_DATA_DIGEST);
        byte[] resultDigest =  messageDigest.digest(imageBytes);
        SignatureService signatureService = SignatureService.genUserVSSignatureService(representativeNIF)
        SMIMEMessage smimeMessage = signatureService.getTimestampedSignedMimeMessage(representativeNIF,
                ContextVS.getInstance().getAccessControl().getNameNormalized(),
                getRequestJSON(representativeNIF, Base64.getEncoder().encodeToString(resultDigest)).toString(), subject)
        RepresentativeDataSender representativeDataSender = new RepresentativeDataSender(smimeMessage,
                FileUtils.getFileFromBytes(imageBytes), ContextVS.getInstance().getAccessControl().getRepresentativeServiceURL());
        ResponseVS responseVS = representativeDataSender.call();
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) responseVS.setMessage(representativeNIF);
        return responseVS;
    }
    
    private JSONObject getRequestJSON(String representativeNIF, String imageDigestStr) {
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_DATA");
        map.put("representativeInfo", " --- data about the representative -" + representativeNIF);
        map.put("base64ImageHash", imageDigestStr);
        map.put("UUID", UUID.randomUUID().toString());
        return JSONSerializer.toJSON(map);
    }

}