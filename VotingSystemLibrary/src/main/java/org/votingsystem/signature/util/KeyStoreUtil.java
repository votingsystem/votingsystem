package org.votingsystem.signature.util;

import javax.security.auth.x500.X500PrivateCredential;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class KeyStoreUtil {
    
    public static KeyStore getKeyStoreFromFile(String filePath, char[] password) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(new FileInputStream(filePath), password);
        return store;
    }
    
    public static KeyStore getKeyStoreFromBytes(byte[] keyStore, char[] password) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(new ByteArrayInputStream(keyStore), password);
        return store;
    }
    
    public static byte[] getBytes (KeyStore keyStore, char[] password) throws Exception {
    	ByteArrayOutputStream baos  = new ByteArrayOutputStream();
    	keyStore.store(baos, password);
    	return baos.toByteArray();
    }
    
    public static KeyStore createRootKeyStore(long begin, long period, 
            char[] password, String rootAlias, String strSubjectDN) throws Exception {
        Date dateBegin = new Date(begin);
        Date dateFinish = new Date(begin + period);
        return createRootKeyStore(dateBegin, dateFinish, password, rootAlias, strSubjectDN);
    }

    public static KeyStore createRootKeyStore(Date dateBegin, Date dateFinish, char[] password,
              String rootAlias, String strSubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        KeyPair rootPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        X509Certificate rootCert = CertUtils.generateV3RootCert(rootPair, dateBegin, dateFinish, strSubjectDN);
        X500PrivateCredential rootCredential = new X500PrivateCredential(rootCert, rootPair.getPrivate(), rootAlias);
        store.setCertificateEntry(rootCredential.getAlias(), rootCredential.getCertificate());
        store.setKeyEntry(rootCredential.getAlias(), rootCredential.getPrivateKey(), password,
                new Certificate[] {rootCredential.getCertificate()});
        return store;
    }

    /**
     * Create user KeyStore
     */
    public static KeyStore createUserKeyStore(long begin, long period, char[] password, 
            String endEntityAlias, X500PrivateCredential rootCredential, 
            String endEntitySubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        KeyPair endPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        Date dateBegin = new Date(begin);
        Date dateFinish = new Date(begin + period);
        X509Certificate endCert = CertUtils.generateEndEntityCert(endPair.getPublic(),
                rootCredential.getPrivateKey(), rootCredential.getCertificate(),
                dateBegin, dateFinish, endEntitySubjectDN);
        X500PrivateCredential endCredential = new X500PrivateCredential(
                endCert, endPair.getPrivate(), endEntityAlias);
        store.setCertificateEntry(rootCredential.getAlias(), rootCredential.getCertificate());
        store.setKeyEntry(endCredential.getAlias(), endCredential.getPrivateKey(), password, 
                new Certificate[] {endCredential.getCertificate(), rootCredential.getCertificate()});
        return store;
    }
    
    /**
     * Create user TimeStampingKeyStore
     */
    public static KeyStore createTimeStampingKeyStore(long begin, long period, 
            char[] password,  String endEntityAlias, X500PrivateCredential rootCredential, 
            String endEntitySubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        KeyPair endPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        X509Certificate endCert = CertUtils.generateTimeStampingCert(endPair.getPublic(),
                rootCredential.getPrivateKey(), rootCredential.getCertificate(),
                begin, period, endEntitySubjectDN);
        X500PrivateCredential endCredential = new X500PrivateCredential(
                endCert, endPair.getPrivate(), endEntityAlias);
        store.setCertificateEntry(rootCredential.getAlias(), rootCredential.getCertificate());
        store.setKeyEntry(endCredential.getAlias(), endCredential.getPrivateKey(), password, 
                new Certificate[] { 
        		endCredential.getCertificate(), rootCredential.getCertificate()});
        return store;
    }
    
}
