package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.signature.util.VotingSystemKeyGenerator

import org.votingsystem.util.HttpHelper
import org.votingsystem.util.ApplicationContextHolder

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.concurrent.Callable
/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EncryptionTestSender implements Callable<ResponseVS> {

    private static Logger logger = Logger.getLogger(EncryptionTestSender.class);

    private String requestNIF;
    private ResponseVS responseVS;
    private String serverURL = null;
    private X509Certificate serverCert = null;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public EncryptionTestSender(String requestNIF, String serverURL, X509Certificate serverCert)  throws Exception {
        this.requestNIF = requestNIF;
        this.serverURL = serverURL;
        KeyPair keyPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        this.serverCert = serverCert;
    }
    @Override public ResponseVS call() throws Exception {
        byte[] encryptedRequestBytes = Encryptor.encryptMessage(
                getTestJSON(requestNIF, publicKey).toString().getBytes(), serverCert);
        responseVS = HttpHelper.getInstance().sendData(encryptedRequestBytes, ContentTypeVS.ENCRYPTED, serverURL);
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] encryptedData = responseVS.getMessageBytes();
            byte[] decryptedData = Encryptor.decryptFile(encryptedData, publicKey, privateKey);
            logger.debug(" >>>>>> decryptedData: " + new String(decryptedData));
        }
        return responseVS;
    }

    public JSONObject getTestJSON(String from, PublicKey publicKey) {
        logger.debug("getTestJSON");
        String publicKeyStr = new String(Base64.encode(publicKey.getEncoded()));
        //logger.debug("publicKeyStr: " + publicKeyStr);
        //byte[] decodedPK = Base64.decode(publicKeyStr);
        //PublicKey pk =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
        //logger.debug("pk.toString(): " + pk.toString());
        Map map = new HashMap();
        map.put("from", from);
        map.put("publicKey", publicKeyStr);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject;
    }

}