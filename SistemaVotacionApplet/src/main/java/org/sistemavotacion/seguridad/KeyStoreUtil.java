package org.sistemavotacion.seguridad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import javax.security.auth.x500.X500PrivateCredential;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class KeyStoreUtil {
    
    public static KeyStore getKeyStoreFromFile(String filePath, char[] password) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(new FileInputStream(filePath), password);
        return store;
    }
    
    public static KeyStore getKeyStoreFromFile(File file, char[] password) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(new FileInputStream(file), password);
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
    public static KeyStore createKeyStore(long comienzo, int periodoValidez, 
    		char[] password, String rootAlias, String endEntityAlias, String principal, 
                String endEntitySubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        X500PrivateCredential rootCredential = KeyUtil.createRootCredential(
        		comienzo, periodoValidez, rootAlias, principal);
        X500PrivateCredential endCredential = KeyUtil.createEndEntityCredential(
        		rootCredential.getPrivateKey(), rootCredential.getCertificate(), 
        		comienzo, periodoValidez, endEntityAlias, endEntitySubjectDN);
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
    public static KeyStore createRootKeyStore(long comienzo, int periodoValidez, 
    		char[] password, String rootAlias, String filePath, String strSubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        X500PrivateCredential rootCredential = KeyUtil.createRootCredential(
        		comienzo, periodoValidez, rootAlias, strSubjectDN);
        store.setCertificateEntry(
        		rootCredential.getAlias(), rootCredential.getCertificate());
        store.setKeyEntry(rootCredential.getAlias(), rootCredential.getPrivateKey(), password, 
                new Certificate[] {rootCredential.getCertificate()});
        if (filePath != null) store.store(new FileOutputStream(new File(filePath)), password);
        return store;
    } 
    
    /**
     * Crea un almacén de claves para un Actor
     */
    public static KeyStore createActorKeyStore(long comienzo, int periodoValidez, char[] password, 
    		String endEntityAlias, X500PrivateCredential rootCredential, 
                String endEntitySubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        X500PrivateCredential endCredential = KeyUtil.createEndEntityCredential(
        		rootCredential.getPrivateKey(), rootCredential.getCertificate(), 
        		comienzo, periodoValidez, endEntityAlias, endEntitySubjectDN);
        store.setCertificateEntry(rootCredential.getAlias(), rootCredential.getCertificate());
        store.setKeyEntry(endCredential.getAlias(), endCredential.getPrivateKey(), password, 
                new Certificate[] { 
        		endCredential.getCertificate(), rootCredential.getCertificate()});
        return store;
    }
    
    /**
     * Crea un almacén de claves para un emisora de sellos de tiempo
     */
    public static KeyStore createTimeStampingKeyStore(
            long comienzo, int periodoValidez, char[] password, 
            String endEntityAlias, X500PrivateCredential rootCredential, 
            String endEntitySubjectDN) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        X500PrivateCredential endCredential = KeyUtil.createTimeStampingCredential(
        		rootCredential.getPrivateKey(), rootCredential.getCertificate(), 
        		comienzo, periodoValidez, endEntityAlias, endEntitySubjectDN);
        store.setCertificateEntry(rootCredential.getAlias(), rootCredential.getCertificate());
        store.setKeyEntry(endCredential.getAlias(), endCredential.getPrivateKey(), password, 
                new Certificate[] { 
        		endCredential.getCertificate(), rootCredential.getCertificate()});
        return store;
    }

}
