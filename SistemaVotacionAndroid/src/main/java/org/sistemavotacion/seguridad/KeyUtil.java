package org.sistemavotacion.seguridad;

import org.bouncycastle2.openssl.PEMReader;
import org.bouncycastle2.openssl.PEMWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
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
		
	    /**
	     * Crea un par de claves RSA aleatorio de 1024 bits
	     */
	    public static KeyPair generateRSAKeyPair() throws Exception {
	        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
	        kpGen.initialize(1024, new SecureRandom());
	        return kpGen.generateKeyPair();
	    }
	    
	    public static byte[] fromX509CertToPEM (X509Certificate certificate) throws IOException {
	        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
	        PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(bOut));
	        pemWrt.writeObject(certificate);
	        pemWrt.close();
	        bOut.close();
	        return bOut.toByteArray();
	    }

	    public static X509Certificate fromPEMToX509Cert (byte[] pemFileBytes) throws Exception {
	        InputStream in = new ByteArrayInputStream(pemFileBytes);
	        CertificateFactory fact = CertificateFactory.getInstance("X.509","BC");
	        X509Certificate x509Cert = (X509Certificate)fact.generateCertificate(in);
	        return x509Cert;
	    }
		
	    	
	    public static X509Certificate loadCertificateFromStream (InputStream inputStream) throws Exception {
	        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
	        Collection<X509Certificate> certificateChain =
	                (Collection<X509Certificate>) certificateFactory.generateCertificates(inputStream);
	        X509Certificate cert = certificateChain.iterator().next();
	        return cert;
	    }
	    
	    public static String getPEMStringFromKey (Key key) throws IOException {
		    StringWriter sw = new StringWriter();
		    PEMWriter pw = new PEMWriter(sw);
		    pw.writeObject(key);
		    pw.flush();
		    return sw.toString();
	    }

	    public static KeyPair readPEMKeyPair(String pemPrivateKey, char[] keyPassword) throws IOException {
	    	StringReader stringReader = new StringReader(pemPrivateKey);
	        PEMReader r = new PEMReader(stringReader, new DefaultPasswordFinder(keyPassword));
	        return (KeyPair) r.readObject();
	    }
}
