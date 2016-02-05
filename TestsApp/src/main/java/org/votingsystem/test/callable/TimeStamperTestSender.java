package org.votingsystem.test.callable;

import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.StringUtils;

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
        SMIMEMessage smimeMessage = signatureService.getSMIME(nif,
                StringUtils.getNormalized(serverURL), getRequestJSON(nif).toString(), subject);
        MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, ActorVS.getTimeStampServiceURL(serverURL));
        return ResponseVS.OK(null).setSMIME(timeStamper.call());
    }
        
    private Map getRequestJSON(String nif) {
        Map map = new HashMap();
        map.put("operation", "TIMESTAMP_TEST");
        map.put("nif", nif);
        map.put("UUID", UUID.randomUUID().toString());
        return map;
    }

}