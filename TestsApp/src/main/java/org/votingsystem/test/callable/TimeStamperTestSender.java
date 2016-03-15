package org.votingsystem.test.callable;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.cms.CMSSignedMessage;
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

    public TimeStamperTestSender(String nif) throws Exception {
        this.nif = nif;
    }
        
    @Override public ResponseVS call() throws Exception {
        SignatureService signatureService = SignatureService.load(this.nif);
        CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(getRequest(nif).getBytes());
        return ResponseVS.OK(null).setCMS(cmsMessage);
    }
        
    private String getRequest(String nif) throws JsonProcessingException {
        Map map = new HashMap();
        map.put("operation", "TIMESTAMP_TEST");
        map.put("nif", nif);
        map.put("UUID", UUID.randomUUID().toString());
        return JSON.getMapper().writeValueAsString(map);
    }

}