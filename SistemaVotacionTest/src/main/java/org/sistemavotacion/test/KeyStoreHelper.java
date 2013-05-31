package org.sistemavotacion.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500PrivateCredential;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class KeyStoreHelper {

    private static Logger logger = LoggerFactory.getLogger(KeyStoreHelper.class);
    
    public static void main(String args[]) throws Exception { }

    
    public static KeyStore crearMockRaizDNIe() throws Exception {
        KeyStore keyStore = KeyStoreUtil.createRootKeyStore(ContextoPruebas.COMIEZO_VALIDEZ_CERT, 
                ContextoPruebas.PERIODO_VALIDEZ_ALMACEN_RAIZ, 
                ContextoPruebas.PASSWORD.toCharArray(), ContextoPruebas.ROOT_ALIAS, 
                "CN=Autoridad Certificadora Sistema de Votación, OU=DNIE_CA");
        return keyStore;
    }
    
    public static X500PrivateCredential obtenerMockRaizDNIePrivateCredential() throws Exception {
        KeyStore rootKeyStore = crearMockRaizDNIe();
        X509Certificate rootCertificate = (X509Certificate)rootKeyStore.getCertificate(ContextoPruebas.ROOT_ALIAS);
        PrivateKey rootPK = (PrivateKey) rootKeyStore.getKey(ContextoPruebas.ROOT_ALIAS, 
                ContextoPruebas.PASSWORD.toCharArray());
        X500PrivateCredential mockRaizDNIe = new X500PrivateCredential(
            rootCertificate,rootPK,ContextoPruebas.ROOT_ALIAS);
        return mockRaizDNIe;
    }

    public static KeyStore crearMockDNIe(String userID, File file) throws Exception {
        X500PrivateCredential rootCredential = obtenerMockRaizDNIePrivateCredential();
        KeyStore keyStore = KeyStoreUtil.createActorKeyStore(ContextoPruebas.COMIEZO_VALIDEZ_CERT,
                ContextoPruebas.PERIODO_VALIDEZ_CERT, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.END_ENTITY_ALIAS, rootCredential, 
                "GIVENNAME=NombreDe" + userID + " ,SURNAME=ApellidoDe" + userID + ", SERIALNUMBER=" + userID); 
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, ContextoPruebas.PASSWORD.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
        return keyStore;
    }
    
    public static KeyStore crearMockDNIe(String userID, File file, 
            X500PrivateCredential rootCredential) throws Exception {
        KeyStore keyStore = KeyStoreUtil.createActorKeyStore(ContextoPruebas.COMIEZO_VALIDEZ_CERT,
                ContextoPruebas.PERIODO_VALIDEZ_CERT, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.END_ENTITY_ALIAS, rootCredential, 
                "GIVENNAME=NombreDe" + userID + " ,SURNAME=ApellidoDe" + userID + ", SERIALNUMBER=" + userID); 
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, ContextoPruebas.PASSWORD.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
        return keyStore;
    }
    
    public static X500PrivateCredential crearMockUsuarioVotacion(String userID, File file) throws Exception {
        X500PrivateCredential rootCredential = obtenerMockRaizDNIePrivateCredential();
        KeyStore keyStore = KeyStoreUtil.createActorKeyStore(ContextoPruebas.COMIEZO_VALIDEZ_CERT,
                ContextoPruebas.PERIODO_VALIDEZ_CERT, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.END_ENTITY_ALIAS, rootCredential, 
                "GIVENNAME=NombreDe" + userID + " ,SURNAME=ApellidoDe" + userID + ", SERIALNUMBER=" + userID); 
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, ContextoPruebas.PASSWORD.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
        return rootCredential;
    }
    
   public static KeyStore crearMockServidorVotacion(File file, 
           X500PrivateCredential rootCredential, String subjectDN, String alias) throws Exception {
        KeyStore keyStore = KeyStoreUtil.createActorKeyStore(ContextoPruebas.COMIEZO_VALIDEZ_CERT,
                ContextoPruebas.PERIODO_VALIDEZ_CERT, ContextoPruebas.PASSWORD.toCharArray(),
                alias, rootCredential, subjectDN); 
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, ContextoPruebas.PASSWORD.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
        return keyStore;
    }

}