package org.sistemavotacion.seguridad;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import javax.security.auth.x500.X500PrivateCredential;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class KeyUtil {
    
    public static final String PEM_FILE_EXTENSION = "pem";

    public static PublicKey getPublicKey(String filename) throws Exception {
        File f = new File(filename);
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int)f.length()];
        dis.readFully(keyBytes);
        dis.close();
        return getPublicKey(keyBytes);
    }

    public static PublicKey getPublicKey(File f) throws Exception {
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int)f.length()];
        dis.readFully(keyBytes);
        dis.close();
        return getPublicKey(keyBytes);
    }

    public static PublicKey getPublicKey(byte[] keyBytes) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static PrivateKey getPrivateKey(String filename) throws Exception {
        File f = new File(filename);
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int)f.length()];
        dis.readFully(keyBytes);
        dis.close();
        return getPrivateKey(keyBytes);
    }

    public static PrivateKey getPrivateKey(File f) throws Exception {
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int)f.length()];
        dis.readFully(keyBytes);
        dis.close();
        return getPrivateKey(keyBytes);
    }

    public static PrivateKey getPrivateKey(byte[] keyBytes) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public static X500PrivateCredential createRootCredential(long comienzo, 
            int periodoValidez, String rootAlias, String strSubjectDN) throws Exception {
    	KeyPair rootPair = generateRSAKeyPair();
    	X509Certificate rootCert = CertUtil.generateV3RootCert(
                rootPair, comienzo, periodoValidez, strSubjectDN);
    	return new X500PrivateCredential(rootCert, rootPair.getPrivate(), rootAlias);
    }
    
    public static X500PrivateCredential createEndEntityCredential(PrivateKey caKey, 
    		X509Certificate caCert, long comienzo, int periodoValidez, 
                String endEntityAlias, String endEntitySubjectDN) throws Exception {
        KeyPair endPair = generateRSAKeyPair();
        X509Certificate endCert = CertUtil.generateEndEntityCert(endPair.getPublic(), 
        		caKey, caCert, comienzo, periodoValidez, endEntitySubjectDN);
        return new X500PrivateCredential(endCert, endPair.getPrivate(), endEntityAlias);
    }
    
    public static X500PrivateCredential createTimeStampingCredential(PrivateKey caKey, 
    		X509Certificate caCert, long comienzo, int periodoValidez, 
                String endEntityAlias, String endEntitySubjectDN) throws Exception {
        KeyPair endPair = generateRSAKeyPair();
        X509Certificate endCert = CertUtil.generateTimeStampingCert(endPair.getPublic(), 
        		caKey, caCert, comienzo, periodoValidez, endEntitySubjectDN);
        return new X500PrivateCredential(endCert, endPair.getPrivate(), endEntityAlias);
    }
    
    /**
     * Crea un par de claves RSA aleatorio de 1024 bits
     */
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(1024, new SecureRandom());
        return kpGen.generateKeyPair();
}
    
}
