package org.votingsystem.simulation.callable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.web.json.JSONObject

import org.votingsystem.simulation.ApplicationContextHolder as ACH;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class MultiSignTester implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(MultiSignTester.class);

    private String requestNIF;
    private String serverURL = null;

    public MultiSignTester (String requestNIF, String serverURL) 
            throws Exception {
        this.requestNIF = requestNIF;
        this.serverURL = serverURL;
    }
    
    @Override public ResponseVS call() throws Exception {
        KeyStore mockDnie = SimulationContext.INSTANCE.crearMockDNIe(requestNIF);
        String msgSubject = "Message from MultiSignTester";
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
            mockDnie, SimulationContext.DEFAULTS.END_ENTITY_ALIAS, 
            SimulationContext.PASSWORD.toCharArray(),
            SimulationContext.VOTE_SIGN_MECHANISM);
        
        File encryptedFile = File.createTempFile(
                "signeTestFile_from" + requestNIF, ".p7m");
        //encryptedFile.deleteOnExit();
        String testJSONstr = getTestJSON(requestNIF);
        

        String toUser = "MultiSignController";
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.
            genMimeMessage(requestNIF, toUser, testJSONstr, msgSubject, null);

        SMIMESignedSender sender= new SMIMESignedSender(
                        null, smimeMessage, serverURL, null, null);
        ResponseVS senderResponse = sender.call();
        
        log.debug("Response status: " + senderResponse.getStatusCode());
        if(ResponseVS.SC_OK == senderResponse.getStatusCode()) {
            byte[] multiSigendResponseBytes = senderResponse.getMessageBytes();
            SMIMEMessageWrapper smimeResponse = new SMIMEMessageWrapper(null, 
                    new ByteArrayInputStream(multiSigendResponseBytes), null);
            log.debug("- smimeResponse.isValidSignature(): " + 
                    smimeResponse.isValidSignature());
        } else senderResponse.appendErrorMessage(" - from: " + requestNIF);
        return senderResponse;
    }

    public String getTestJSON(String from) {
        String result = null;
        try {
            log.debug("getTestJSON");
            
            Map map = new HashMap();
            map.put("from", from);
            map.put("UUID", UUID.randomUUID().toString());
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
            result = jsonObject.toString();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return result;
    }

}
