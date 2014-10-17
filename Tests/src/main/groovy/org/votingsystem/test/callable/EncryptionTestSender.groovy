package org.votingsystem.test.callable

import net.sf.json.JSONObject
import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.signature.util.KeyGeneratorVS
import org.votingsystem.util.HttpHelper

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

    private static Logger log = Logger.getLogger(EncryptionTestSender.class);

    private String requestNIF;
    private String serverURL = null;
    private X509Certificate serverCert = null;

    public EncryptionTestSender(String requestNIF, String serverURL, X509Certificate serverCert)  throws Exception {
        this.requestNIF = requestNIF;
        this.serverURL = serverURL;
        this.serverCert = serverCert;
    }
    @Override public ResponseVS call() throws Exception {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        byte[] encryptedRequestBytes = Encryptor.encryptMessage(
                getRequestJSON(requestNIF, publicKey).toString().getBytes(), serverCert);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(encryptedRequestBytes, ContentTypeVS.ENCRYPTED, serverURL);
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] encryptedData = responseVS.getMessageBytes();
            byte[] decryptedData = Encryptor.decryptFile(encryptedData, publicKey, privateKey);
            log.debug("decryptedData: " + new String(decryptedData));
        }
        return responseVS;
    }

    private JSONObject getRequestJSON(String from, PublicKey publicKey) {
        //PublicKey pk =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
        Map map = new HashMap();
        map.put("from", from);
        map.put("publicKey", Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        map.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(map);
    }

}