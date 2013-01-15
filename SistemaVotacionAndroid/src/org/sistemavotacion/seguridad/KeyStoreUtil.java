package org.sistemavotacion.seguridad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class KeyStoreUtil {
    
    public static KeyStore getKeyStoreFromFile(String filePath, char[] password) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(new FileInputStream(filePath), password);
        return store;
    }
    
    public static KeyStore getKeyStoreFromBytes(byte[] keyStore, char[] password) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
    	store.load(new ByteArrayInputStream(keyStore), password);
        return store;
    }
    
    public static KeyStore getKeyStoreFromStream(InputStream keyStoreInputStream, char[] password) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(keyStoreInputStream, password);
        return store;
    }
    
    public static byte[] getBytes (KeyStore keyStore, char[] password) throws Exception {
    	ByteArrayOutputStream baos  = new ByteArrayOutputStream();
    	keyStore.store(baos, password);
    	return baos.toByteArray();
    }

}