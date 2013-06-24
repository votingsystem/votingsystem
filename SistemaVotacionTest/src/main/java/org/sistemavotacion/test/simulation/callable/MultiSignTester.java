package org.sistemavotacion.test.simulation.callable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class MultiSignTester implements Callable<Respuesta> {

    private static Logger logger = LoggerFactory.getLogger(MultiSignTester.class);

    private String requestNIF;
    private String serverURL = null;

    public MultiSignTester (String requestNIF, String serverURL) 
            throws Exception {
        this.requestNIF = requestNIF;
        this.serverURL = serverURL;
    }
    
    @Override public Respuesta call() throws Exception {
        KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(requestNIF);
        String msgSubject = "Message from MultiSignTester";
        
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
            mockDnie, ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
            ContextoPruebas.PASSWORD.toCharArray(),
            ContextoPruebas.VOTE_SIGN_MECHANISM);
        
        File encryptedFile = File.createTempFile(
                "signeTestFile_from" + requestNIF, ".p7m");
        //encryptedFile.deleteOnExit();
        String testJSONstr = getTestJSON(requestNIF);
        

        String toUser = "MultiSignController";
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.
            genMimeMessage(requestNIF, toUser, testJSONstr, msgSubject, null);

        SMIMESignedSender sender= new SMIMESignedSender(
                        null, smimeMessage, serverURL, null, null);
        Respuesta senderResponse = sender.call();
        
        logger.debug("Response status: " + senderResponse.getCodigoEstado());
        if(Respuesta.SC_OK == senderResponse.getCodigoEstado()) {
            byte[] multiSigendResponseBytes = senderResponse.getMessageBytes();
            SMIMEMessageWrapper smimeResponse = new SMIMEMessageWrapper(null, 
                    new ByteArrayInputStream(multiSigendResponseBytes), null);
            logger.debug("- smimeResponse.isValidSignature(): " + 
                    smimeResponse.isValidSignature());
        } else senderResponse.appendErrorMessage(" - from: " + requestNIF);
        return senderResponse;
    }

    public String getTestJSON(String from) {
        String result = null;
        try {
            logger.debug("getTestJSON");
            
            Map map = new HashMap();
            map.put("from", from);
            map.put("UUID", UUID.randomUUID().toString());
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
            result = jsonObject.toString();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return result;
    }

}
