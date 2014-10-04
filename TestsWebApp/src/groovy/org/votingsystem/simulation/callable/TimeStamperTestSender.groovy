package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.simulation.SignatureVSService
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.StringUtils

import java.security.KeyStore
import java.util.concurrent.Callable
/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class TimeStamperTestSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(TimeStamperTestSender.class);
    
    private SMIMEMessage documentSMIME;
    private String requestNIF;
    private String timestampServerURL;

    public TimeStamperTestSender(String requestNIF, String timestampServerURL) throws Exception {
        this.requestNIF = requestNIF;
        this.timestampServerURL = timestampServerURL;
    }
        
    @Override public ResponseVS call() throws Exception {
        SignatureVSService signatureVSService = (SignatureVSService)ApplicationContextHolder.getBean("signatureVSService")
        KeyStore mockDnie = signatureVSService.generateKeyStore(requestNIF)
        ActorVS accessControl = ContextVS.getInstance().getAccessControl();
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie,
                ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD.toCharArray(),
                ContextVS.VOTE_SIGN_MECHANISM);
        String subject = ApplicationContextHolder.getInstance().getMessage("timeStampMsgSubject");
        String toUser = StringUtils.getNormalized(timestampServerURL);
        documentSMIME = signedMailGenerator.genMimeMessage(requestNIF, toUser, getRequestDataJSON(), subject , null);
        String timeStamptServiceURL = timestampServerURL + "/timeStamp"
        String urlTimeStampTestService = timestampServerURL + "/timeStamp/validateTestMessage"
        SMIMESignedSender signedSender = new SMIMESignedSender(documentSMIME, urlTimeStampTestService,
                timeStamptServiceURL, ContentTypeVS.JSON_SIGNED, null, null);
        return signedSender.call();
    }
        
    public String getRequestDataJSON() {
        Map map = new HashMap();
        map.put("operation", "TIMESTAMP_TEST");
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }

}