package org.votingsystem.test.callable

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.SignatureService
import org.votingsystem.util.StringUtils

import java.util.concurrent.Callable

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class TimeStamperTestSender implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(TimeStamperTestSender.class);

    private String nif;
    private String serverURL;

    public TimeStamperTestSender(String nif, String timestampServerURL) throws Exception {
        this.nif = nif;
        this.serverURL = timestampServerURL;
    }
        
    @Override public ResponseVS call() throws Exception {
        String subject = "Message from MultiSignTestSender";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(this.nif)
        SMIMEMessage smimeMessage = signatureService.getTimestampedSignedMimeMessage(nif,
                StringUtils.getNormalized(serverURL), getRequestJSON(nif).toString(), subject)
        String timeStampTestServiceURL = serverURL + "/timeStamp/validateTestMessage"
        SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage, timeStampTestServiceURL,
                ActorVS.getTimeStampServiceURL(serverURL), ContentTypeVS.JSON_SIGNED, null, null);
        return signedSender.call();
    }
        
    private JSONObject getRequestJSON(String nif) {
        Map map = new HashMap();
        map.put("operation", "TIMESTAMP_TEST");
        map.put("nif", nif);
        map.put("UUID", UUID.randomUUID().toString());
        return JSONSerializer.toJSON(map);
    }

}