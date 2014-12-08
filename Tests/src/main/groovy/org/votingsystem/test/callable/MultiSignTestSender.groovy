package org.votingsystem.test.callable

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureService
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.StringUtils

import java.util.concurrent.Callable

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MultiSignTestSender implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(MultiSignTestSender.class);

    private String nif;
    private String serverURL = null;

    public MultiSignTestSender(String nif, String serverURL) throws Exception {
        this.nif = nif;
        this.serverURL = serverURL;
    }
    
    @Override public ResponseVS call() throws Exception {
        String subject = "Message from MultiSignTestSender";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(this.nif)
        SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(nif,
                StringUtils.getNormalized(serverURL), getRequestJSON(nif).toString(), subject)
        ResponseVS responseVS = HttpHelper.getInstance().sendData(
                smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED, serverURL);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] multiSigendResponseBytes = responseVS.getMessageBytes();
            SMIMEMessage smimeResponse = new SMIMEMessage(new ByteArrayInputStream(multiSigendResponseBytes));
            log.debug("- smimeResponse.isValidSignature(): " + smimeResponse.isValidSignature());
        } else throw new ExceptionVS(responseVS.getMessage())
        return responseVS;
    }

    private JSONObject getRequestJSON(String from) {
        Map map = new HashMap();
        map.put("from", from);
        map.put("UUID", UUID.randomUUID().toString());
        return JSONSerializer.toJSON(map);
    }

}
