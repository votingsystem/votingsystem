package pruebas;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.VotingSystemKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class CMSEncryptionTest {

    private static Logger logger = (Logger) LoggerFactory.getLogger(CMSEncryptionTest.class);
    
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    
    private static X509Certificate rootCert = null;
    
    private static String textoPrueba = "Hooola";
    
    private static String strSubjectDNRoot = "CN=eventoUrl:sistemavotacion.cloudfundry.com, OU=Votaciones";
    
    public static void main(String[] args) throws Exception {
        File fileInput = File.createTempFile("pruebaCMS", ".json");
        fileInput.deleteOnExit();
        FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("testFiles/votingOperation.json"), fileInput); 
        
        
        KeyPair rootPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        int periodoValidez = new Long(System.currentTimeMillis()).intValue() + 100000000;
    	rootCert = CertUtil.generateV3RootCert(
                rootPair, System.currentTimeMillis() -1000, periodoValidez, strSubjectDNRoot);
        byte[] inputData = FileUtils.getBytesFromFile(fileInput);

        byte[] encryptedDataBytes = Encryptor.encryptCMS(inputData, rootCert);
        //logger.debug("--- encryptedDataBytes: " + new String(encryptedDataBytes));
        
        byte[] base64EncryptedDataBytes = Base64.encode(encryptedDataBytes);
        logger.debug("--- base64EncryptedDataBytes: " + new String(base64EncryptedDataBytes));


        byte[] encryptedDataBytes1 = Base64.decode(base64EncryptedDataBytes);

        byte[] decryptedDataBytes = Encryptor.decryptCMSStream(
                rootPair.getPrivate(), encryptedDataBytes1);
        logger.debug("--- decryptedDataBytes: " + new String(decryptedDataBytes));

    }

    public static SecretKey makeDesede128Key() throws Exception {
        SecureRandom rand = new SecureRandom();
        KeyGenerator     desede128kg = KeyGenerator.getInstance("DESEDE", "BC");
        desede128kg.init(112, rand);
        return desede128kg.generateKey();
    }
    
    public static SecretKey makeAESKey(int keySize) throws Exception {
        KeyGenerator aesKg = KeyGenerator.getInstance("AES", "BC");
        aesKg.init(keySize);
        return aesKg.generateKey();
    }

}
