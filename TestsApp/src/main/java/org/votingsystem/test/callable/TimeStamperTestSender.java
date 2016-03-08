package org.votingsystem.test.callable;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class TimeStamperTestSender implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(TimeStamperTestSender.class.getName());

    private String nif;
    private String serverURL;

    public TimeStamperTestSender(String nif, String timestampServerURL) throws Exception {
        this.nif = nif;
        this.serverURL = timestampServerURL;
    }
        
    @Override public ResponseVS call() throws Exception {
        String subject = "Message from MultiSignTestSender";
        SignatureService signatureService = SignatureService.genUserVSSignatureService(this.nif);
        CMSSignedMessage cmsMessage = signatureService.signData(getRequest(nif));
        MessageTimeStamper timeStamper = new MessageTimeStamper(cmsMessage, ActorVS.getTimeStampServiceURL(serverURL));
        return ResponseVS.OK(null).setCMS(timeStamper.call());
    }
        
    private String getRequest(String nif) throws JsonProcessingException {
        Map map = new HashMap();
        map.put("operation", "TIMESTAMP_TEST");
        map.put("nif", nif);
        map.put("UUID", UUID.randomUUID().toString());
        return JSON.getMapper().writeValueAsString(map);
    }

}