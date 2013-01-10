/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author jgzornoza
 */
public class DigestTest {
    
    private static Logger logger = LoggerFactory.getLogger(DigestTest.class);    
    
    public DigestTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void bouncycastleHashAlgos() throws Exception {
        byte[] message = "hello world".getBytes();
        MessageDigest messageDigest = MessageDigest.getInstance("RIPEMD160",
                        new BouncyCastleProvider());
        byte[] digest = messageDigest.digest(message);
        logger.debug("RIPEMD160 size: " + digest.length);

        messageDigest = MessageDigest.getInstance("RIPEMD128",
                        new BouncyCastleProvider());
        digest = messageDigest.digest(message);
        logger.debug("RIPEMD128 size: " + digest.length);

        messageDigest = MessageDigest.getInstance("RIPEMD256",
                        new BouncyCastleProvider());
        digest = messageDigest.digest(message);
        logger.debug("RIPEMD256 size: " + digest.length);

        messageDigest = MessageDigest.getInstance("RIPEMD320",
                        new BouncyCastleProvider());
        digest = messageDigest.digest(message);
        logger.debug("RIPEMD320 size: " + digest.length);

        messageDigest = MessageDigest.getInstance("SHA-224",
                        new BouncyCastleProvider());
        digest = messageDigest.digest(message);
        logger.debug("SHA-224 size: " + digest.length);
    }

    @Test
    public void digestInfo() throws Exception {
        byte[] message = "hello world".getBytes();
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        byte[] digest = messageDigest.digest(message);
        logger.debug("Digest: " + new String(Hex.encodeHex(digest)));
        DERObjectIdentifier hashAlgoId = OIWObjectIdentifiers.idSHA1;
        DigestInfo digestInfo = new DigestInfo(new AlgorithmIdentifier(
                        hashAlgoId), digest);
        byte[] encodedDigestInfo = digestInfo.getEncoded();
        logger.debug("Digest Info: "
                        + new String(Hex.encodeHex(encodedDigestInfo)));
    }
}
