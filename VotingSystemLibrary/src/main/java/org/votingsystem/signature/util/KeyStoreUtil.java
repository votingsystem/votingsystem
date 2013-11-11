package org.votingsystem.signature.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Date;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500PrivateCredential;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
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

    /**
     * Crea un almacén de claves que contiene la credencial privada con la cadena de certificados.
     * Crea certificado raíz.
     */
    public static KeyStore createKeyStoreWithEndEntity(Date fechaInicio, Date fechaFin, 
    		char[] password, String rootAlias, String endEntityAlias, 
    		String principal, String endEntitySubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        X500PrivateCredential rootCredential = KeyUtil.createRootCredential(
        		fechaInicio, fechaFin, rootAlias, principal);
        X500PrivateCredential endCredential = KeyUtil.createEndEntityCredential(
        		rootCredential.getPrivateKey(), rootCredential.getCertificate(), 
        		fechaInicio, fechaFin, endEntityAlias, endEntitySubjectDN);
        store.setCertificateEntry(
        		rootCredential.getAlias(), rootCredential.getCertificate());
        store.setKeyEntry(endCredential.getAlias(), endCredential.getPrivateKey(), password, 
                new Certificate[] { 
        		endCredential.getCertificate(), rootCredential.getCertificate()});
        return store;
    }

    
    /**
     * Crea un almacén de claves para la Autoridad Certificadora
     */
    public static KeyStore createRootKeyStore(Date fechaInicio, Date fechaFin, 
    		char[] password, String rootAlias, String filePath, String strSubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        X500PrivateCredential rootCredential = KeyUtil.createRootCredential(
        		fechaInicio, fechaFin, rootAlias, strSubjectDN);
        store.setCertificateEntry(rootCredential.getAlias(), 
                rootCredential.getCertificate());
        store.setKeyEntry(rootCredential.getAlias(), rootCredential.getPrivateKey(), password, 
                new Certificate[] {rootCredential.getCertificate()});
        if (filePath != null) store.store(new FileOutputStream(new File(filePath)), password);
        return store;
    } 
    
    public static KeyStore createRootKeyStore(long begin, long period, 
            char[] password, String rootAlias, String strSubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        KeyPair rootPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        X509Certificate rootCert = CertUtil.generateV3RootCert(
                rootPair, begin, period, strSubjectDN);

        X500PrivateCredential rootCredential = new X500PrivateCredential(
                rootCert, rootPair.getPrivate(), rootAlias);
        store.setCertificateEntry(
        		rootCredential.getAlias(), rootCredential.getCertificate());
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
        KeyPair endPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        Date dateBegin = new Date(begin);
        Date dateFinish = new Date(begin + period);
        X509Certificate endCert = CertUtil.generateEndEntityCert(endPair.getPublic(), 
                rootCredential.getPrivateKey(), rootCredential.getCertificate(), 
                dateBegin, dateFinish, endEntitySubjectDN);
        X500PrivateCredential endCredential = new X500PrivateCredential(
                endCert, endPair.getPrivate(), endEntityAlias);
        store.setCertificateEntry(rootCredential.getAlias(), rootCredential.getCertificate());
        store.setKeyEntry(endCredential.getAlias(), endCredential.getPrivateKey(), password, 
                new Certificate[] { 
        		endCredential.getCertificate(), rootCredential.getCertificate()});
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
        KeyPair endPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        X509Certificate endCert = CertUtil.generateTimeStampingCert(endPair.getPublic(), 
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
