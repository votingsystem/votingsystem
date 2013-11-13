package org.votingsystem.simulation.callable;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.bouncycastle.util.encoders.Base64;
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.Encryptor;
import org.apache.log4j.Logger;
import org.votingsystem.signature.util.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EncryptionTester implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(EncryptionTester.class);

    private String requestNIF;
    private ResponseVS respuesta;
    private String serverURL = null;
    private X509Certificate serverCert = null;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public EncryptionTester (String requestNIF, String serverURL) 
            throws Exception {
        this.requestNIF = requestNIF;
        this.serverURL = serverURL;
        KeyPair keyPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        serverCert = Contexto.INSTANCE.getAccessControl().getCertificate();
    }
    @Override public ResponseVS call() throws Exception {
        String testJSONstr = getTestJSON(requestNIF, publicKey);
        byte[] encryptredRequestBytes = Encryptor.encryptMessage(
                testJSONstr.getBytes(), serverCert);
        respuesta = Contexto.INSTANCE.getHttpHelper().sendByteArray(
                encryptredRequestBytes, 
                Contexto.ENCRYPTED_CONTENT_TYPE, serverURL);
        if (ResponseVS.SC_OK == respuesta.getStatusCode()) {
            byte[] encryptedData = respuesta.getMessageBytes();
            byte[] decryptedData = Encryptor.decryptFile(encryptedData, 
                    publicKey, privateKey);
            //log.debug(" >>>>>> decryptedData: " + new String(decryptedData));
        }
        return respuesta;
    }

    public String getTestJSON(String from, PublicKey publicKey) {
        String result = null;
        try {
            log.debug("getTestJSON");
            String publicKeyStr = new String(Base64.encode(publicKey.getEncoded()));
            
            //log.debug("publicKeyStr: " + publicKeyStr);

            //byte[] decodedPK = Base64.decode(publicKeyStr);
            //PublicKey pk =  KeyFactory.getInstance("RSA").
            //        generatePublic(new X509EncodedKeySpec(decodedPK));
            //log.debug("pk.toString(): " + pk.toString());
            
            Map map = new HashMap();
            map.put("from", from);
            map.put("publicKey", publicKeyStr);
            map.put("UUID", UUID.randomUUID().toString());
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
            result = jsonObject.toString();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return result;
    }

}
