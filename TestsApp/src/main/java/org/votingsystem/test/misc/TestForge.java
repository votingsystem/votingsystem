package org.votingsystem.test.misc;

import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.crypto.Encryptor;
import org.votingsystem.util.crypto.PEMUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;

public class TestForge {

    private static Logger log =  Logger.getLogger(TestForge.class.getName());

    public static String ivBase64 = "9UPwBv8VwyxBRfMKRLkP/g==";
    public static String keyBase64 = "PcEUN5PwLnMBq02Ik+juOZUW78/psYuLm0aXJnQYSGE=";
    public static String aesCipherText = "sGpaejhKJLbb7jnFNeNUDQ==";
    public static String rsaCipherText = "dszt3CKK0sxNYpyEz2WK2TkQOPRyFl2O5V61LbZbSLYssoj/Q+MVf3EQN88vfETTsMm4nXBodn" +
            "hg7kl3ok7QMPuQRqU5wAw8aj32efmrH6Y7L7/MqWDfUIb7bvt11rYY5aSI92F/4OQw6HGHGqDCApLcjz17HnHPU6zaDGz++h8=";

    public static String privateKeyPEM = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIICXAIBAAKBgQDpOlQkZQFoO6chEdngG7PmBX8j1X3WzXw4XTXSTuqmSKzfVVLD\n" +
            "2vdWl5EDhs9j6qzAzzubOIGuYat1+vRYPoYs/F3+3biyX0z050v4I2t8vuYFy0mJ\n" +
            "AXTbpkSgy0Ba13n2HNIRCUZ0i+LcDV0kQ50S/Kz3Ly6qZP5x5ACiU26rswIDAQAB\n" +
            "AoGBALMA7auxSk8x2ahsBwkWuhCFj3t1VEwVEo5AHBr6LubJSMVwu0FPQh7X3ZkT\n" +
            "UwGhEE47v8f2qrGojOzUW82Oz3Moci4IYl2n7Qwd4s/atv62OXEvFnNMZyy2ppCJ\n" +
            "AZQxe254aKFKj0TENVClBFeyyUbZDBMtA77NQ/eHrSjjDFXBAkEA92hjArL3xkr/\n" +
            "ELYzOQe3VKl9jNCMQpPU0StJZmC/QSgRa/y+w8Okh1ZknmXSRGRLXt69C4nNZkW6\n" +
            "T7RxEA0U6QJBAPFT35frvey5yp+54At6Ix2wrUB9acSL2AAEW0jZrXIgI5O0rdhj\n" +
            "2glXsdQJre/9OYby/gRWz+uM/AD8FNezyjsCQEmI7jzEIt7+NXerH+hogbYZNmbf\n" +
            "KYE0XUHaYtOaF95u4Va+ZZjcEo/jPr7RtsB7KufEvl2qlLE8Mlc0Y5KIwNkCQGly\n" +
            "WJStMUOqutqoATmCmK10cX9oTTrQUAVR4gEm/B6N5H25yOxwVOkYJF+eCx596xEI\n" +
            "Q+3pcNhftg1IGGNX79kCQBtxYsBthY5KymBFmZDtdCQq0M8V5TphARvQ6NS+XIin\n" +
            "5gfANs2deMH92o0xU9unufV8yITlKPg5YSJD6iykbxc=\n" +
            "-----END RSA PRIVATE KEY-----";
    public static String publicKeyPEM = "-----BEGIN PUBLIC KEY-----\n" +
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDpOlQkZQFoO6chEdngG7PmBX8j\n" +
            "1X3WzXw4XTXSTuqmSKzfVVLD2vdWl5EDhs9j6qzAzzubOIGuYat1+vRYPoYs/F3+\n" +
            "3biyX0z050v4I2t8vuYFy0mJAXTbpkSgy0Ba13n2HNIRCUZ0i+LcDV0kQ50S/Kz3\n" +
            "Ly6qZP5x5ACiU26rswIDAQAB\n" +
            "-----END PUBLIC KEY-----\n";
    public static String publicKeyBase64 = "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0NCk1JR2ZNQTBHQ1NxR1NJYjNEUUVCQVFVQUE0R05BRENCaVFLQmdRRHBPbFFrWlFGb082Y2hFZG5nRzdQbUJYOGoNCjFYM1d6WHc0WFRYU1R1cW1TS3pmVlZMRDJ2ZFdsNUVEaHM5ajZxekF6enViT0lHdVlhdDErdlJZUG9Zcy9GMysNCjNiaXlYMHowNTB2NEkydDh2dVlGeTBtSkFYVGJwa1NneTBCYTEzbjJITklSQ1VaMGkrTGNEVjBrUTUwUy9LejMNCkx5NnFaUDV4NUFDaVUyNnJzd0lEQVFBQg0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0tDQo=";


    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        testRSAForgeData();
    }

    private static PublicKey generatePublicKey() throws Exception {
        PublicKey publicKey = PEMUtils.fromPEMToRSAPublicKey(publicKeyPEM);
        log.info(publicKey.toString());
        return publicKey;
    }

    private static PublicKey generatePublicKeyFromBase64() throws Exception {
        String pemKey = new String(Base64.decode(publicKeyBase64.getBytes()));
        PublicKey publicKey = PEMUtils.fromPEMToRSAPublicKey(pemKey);
        log.info(publicKey.toString());
        return publicKey;
    }

    private static final void testRSAForgeData()  throws Exception {
        PrivateKey privateKey = PEMUtils.fromPEMToRSAPrivateKey(privateKeyPEM);
        String plainText = Encryptor.decryptRSA(rsaCipherText, privateKey);
        log.info("plainText: " + plainText);
    }

    private static final void testRSA()  throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024); // speedy generation, but not secure anymore
        KeyPair kp = kpg.generateKeyPair();
        RSAPublicKey pubkey = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey privkey = (RSAPrivateKey) kp.getPrivate();
        String cipherText = Encryptor.encryptRSA("{\"iv\":\"IQU55gJhucngQ9jAl9GOXw==\",\"key\":\"W1iMZ3XQ2wxDce4fCro2O0CwWrMq7i9gZrlqlMWZhlg=\"}", pubkey);
        log.info("cipherText: " + cipherText);
        String plainText = Encryptor.decryptRSA(cipherText, privkey);
        log.info("plainText: " + plainText);
    }

}
