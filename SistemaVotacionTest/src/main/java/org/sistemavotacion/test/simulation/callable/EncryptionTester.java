package org.sistemavotacion.test.simulation.callable;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.test.simulation.EncryptionSimulator;
import org.sistemavotacion.util.VotingSystemKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class EncryptionTester implements Callable<Respuesta> {

    private static Logger logger = LoggerFactory.getLogger(EncryptionSimulator.class);

    private String requestNIF;
    private Respuesta respuesta;
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
    @Override public Respuesta call() throws Exception {
                File encryptedFile = File.createTempFile("csrEncryptedFile", ".p7m");
        encryptedFile.deleteOnExit();
        String testJSONstr = getTestJSON(requestNIF, publicKey);
        Encryptor.encryptMessage(testJSONstr.getBytes(), encryptedFile, serverCert);
        
        respuesta = Contexto.INSTANCE.getHttpHelper().sendFile(encryptedFile, 
                Contexto.ENCRYPTED_CONTENT_TYPE, serverURL);

        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            byte[] encryptedData = respuesta.getBytesArchivo();
            byte[] decryptedData = Encryptor.decryptFile(encryptedData, 
                    publicKey, privateKey);
            //logger.debug(" >>>>>> decryptedData: " + new String(decryptedData));
        }
        return respuesta;
    }

    public String getTestJSON(String from, PublicKey publicKey) {
        String result = null;
        try {
            logger.debug("getTestJSON");
            String publicKeyStr = new String(Base64.encode(publicKey.getEncoded()));
            
            //logger.debug("publicKeyStr: " + publicKeyStr);

            //byte[] decodedPK = Base64.decode(publicKeyStr);
            //PublicKey pk =  KeyFactory.getInstance("RSA").
            //        generatePublic(new X509EncodedKeySpec(decodedPK));
            //logger.debug("pk.toString(): " + pk.toString());
            
            Map map = new HashMap();
            map.put("from", from);
            map.put("publicKey", publicKeyStr);
            map.put("UUID", UUID.randomUUID().toString());
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
            result = jsonObject.toString();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return result;
    }

}
