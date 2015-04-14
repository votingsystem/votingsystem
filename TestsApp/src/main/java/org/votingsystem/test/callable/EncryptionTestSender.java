package org.votingsystem.test.callable;

import org.votingsystem.dto.EncryptedMsgDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyGeneratorVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EncryptionTestSender implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(EncryptionTestSender.class.getSimpleName());

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
        byte[] encryptedRequestBytes = Encryptor.encryptMessage(JSON.getMapper().writeValueAsBytes(
                EncryptedMsgDto.NEW(requestNIF, publicKey)), serverCert);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(encryptedRequestBytes, ContentTypeVS.ENCRYPTED, serverURL);
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] encryptedData = responseVS.getMessageBytes();
            byte[] decryptedData = Encryptor.decryptFile(encryptedData, publicKey, privateKey);
            log.info("decryptedData: " + new String(decryptedData));
        }
        return responseVS;
    }

}