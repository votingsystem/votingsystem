package org.votingsystem.signature.util;

import org.bouncycastle2.openssl.PEMReader;
import org.bouncycastle2.openssl.PEMWriter;

import java.io.*;
import java.security.*;
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
	    
	    public static byte[] getPEMEncoded (X509Certificate certificate) throws IOException {
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