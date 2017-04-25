package org.votingsystem.crypto;

import javax.security.auth.x500.X500PrivateCredential;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class KeyStoreUtils {
    
    public static KeyStore getKeyStore(byte[] keyStore, char[] password) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(new ByteArrayInputStream(keyStore), password);
        return store;
    }
    
    public static byte[] toByteArray(KeyStore keyStore, char[] password) throws Exception {
    	ByteArrayOutputStream baos  = new ByteArrayOutputStream();
    	keyStore.store(baos, password);
    	return baos.toByteArray();
    }

    public static KeyStore generateRootKeyStore(Date dateBegin, Date dateFinish, char[] password,
                                                String rootAlias, String strSubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        KeyPair rootPair = KeyGenerator.INSTANCE.genKeyPair();
        X509Certificate rootCert = CertificateUtils.generateV3RootCert(rootPair, dateBegin, dateFinish, strSubjectDN);
        X500PrivateCredential rootCredential = new X500PrivateCredential(rootCert, rootPair.getPrivate(), rootAlias);
        store.setKeyEntry(rootCredential.getAlias(), rootCredential.getPrivateKey(), password,
                new Certificate[] {rootCredential.getCertificate()});
        return store;
    }

    public static KeyStore generateSystemEntityKeyStore(Date dateBegin, Date dateFinish, char[] password,
                            String endEntityAlias, X500PrivateCredential rootCredential,
                            String endEntitySubjectDN, String ocspServer) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        KeyPair endPair = KeyGenerator.INSTANCE.genKeyPair();
        X509Certificate endCert = CertificateUtils.generateUserCert(endPair.getPublic(),
                rootCredential.getPrivateKey(), rootCredential.getCertificate(),
                dateBegin, dateFinish, endEntitySubjectDN, ocspServer);
        X500PrivateCredential endCredential = new X500PrivateCredential(
                endCert, endPair.getPrivate(), endEntityAlias);
        store.setKeyEntry(endCredential.getAlias(), endCredential.getPrivateKey(), password,
                new Certificate[] {endCredential.getCertificate(), rootCredential.getCertificate()});
        return store;
    }

    public static KeyStore generateUserKeyStore(Date dateBegin, Date dateFinish, char[] password,
                    String endEntityAlias, X500PrivateCredential rootCredential,
                    String endEntitySubjectDN, String ocspServerURL) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        KeyPair endPair = KeyGenerator.INSTANCE.genKeyPair();
        X509Certificate endCert = CertificateUtils.generateUserCert(endPair.getPublic(),
                rootCredential.getPrivateKey(), rootCredential.getCertificate(),
                dateBegin, dateFinish, endEntitySubjectDN, ocspServerURL);
        X500PrivateCredential endCredential = new X500PrivateCredential(
                endCert, endPair.getPrivate(), endEntityAlias);
        store.setKeyEntry(endCredential.getAlias(), endCredential.getPrivateKey(), password, 
                new Certificate[] {endCredential.getCertificate(), rootCredential.getCertificate()});
        return store;
    }

    public static KeyStore generateTimeStampServerKeyStore(Date dateBegin, Date dateFinish, char[] password,
            String endEntityAlias, X500PrivateCredential rootCredential,
           String endEntitySubjectDN, String ocspServerURL) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        KeyPair endPair = KeyGenerator.INSTANCE.genKeyPair();
        X509Certificate endCert = CertificateUtils.generateTimeStampServerCert(endPair.getPublic(),
                rootCredential.getPrivateKey(), rootCredential.getCertificate(),
                dateBegin, dateFinish, endEntitySubjectDN, ocspServerURL);
        X500PrivateCredential endCredential = new X500PrivateCredential(
                endCert, endPair.getPrivate(), endEntityAlias);
        store.setKeyEntry(endCredential.getAlias(), endCredential.getPrivateKey(), password, 
                new Certificate[] { 
        		endCredential.getCertificate(), rootCredential.getCertificate()});
        return store;
    }
    
}
