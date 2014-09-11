package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.simulation.SignatureVSService
import org.votingsystem.util.ApplicationContextHolder

import java.security.KeyStore
import java.util.concurrent.Callable
/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MultiSignTestSender implements Callable<ResponseVS> {

    private static Logger logger = Logger.getLogger(MultiSignTestSender.class);

    private String requestNIF;
    private String serverURL = null;

    public MultiSignTestSender(String requestNIF, String serverURL)
            throws Exception {
        this.requestNIF = requestNIF;
        this.serverURL = serverURL;
    }
    
    @Override public ResponseVS call() throws Exception {
        SignatureVSService signatureVSService = (SignatureVSService)ApplicationContextHolder.getBean("signatureVSService")
        KeyStore mockDnie = signatureVSService.generateKeyStore(requestNIF)
        String msgSubject = "Message from MultiSignTestSender";
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, ContextVS.END_ENTITY_ALIAS,
                ContextVS.PASSWORD.toCharArray(), ContextVS.VOTE_SIGN_MECHANISM);
        
        File encryptedFile = File.createTempFile("signTestFile_from" + requestNIF, ".p7m");
        encryptedFile.deleteOnExit();
        String testJSONstr = getTestJSON(requestNIF);

        String toUser = "MultiSignController";
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(
                requestNIF, toUser, testJSONstr, msgSubject, null);
        SMIMESignedSender sender= new SMIMESignedSender(smimeMessage, serverURL,
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED, null, null);
        ResponseVS senderResponse = sender.call();
        if(ResponseVS.SC_OK == senderResponse.getStatusCode()) {
            byte[] multiSigendResponseBytes = senderResponse.getMessageBytes();
            SMIMEMessageWrapper smimeResponse = new SMIMEMessageWrapper(new ByteArrayInputStream(multiSigendResponseBytes));
            logger.debug("- smimeResponse.isValidSignature(): " + smimeResponse.isValidSignature());
        } else senderResponse.appendMessage(" - from: " + requestNIF);
        return senderResponse;
    }

    public String getTestJSON(String from) {
        String result = null;
        try {
            logger.debug("getTestJSON");
            Map map = new HashMap();
            map.put("from", from);
            map.put("UUID", UUID.randomUUID().toString());
            JSONObject jsonObject = new JSONObject(map);
            result = jsonObject.toString();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return result;
    }

}
