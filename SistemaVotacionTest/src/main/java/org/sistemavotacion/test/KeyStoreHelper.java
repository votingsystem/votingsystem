package org.sistemavotacion.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500PrivateCredential;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class KeyStoreHelper {

    private static Logger logger = LoggerFactory.getLogger(KeyStoreHelper.class);
    
    private static X500PrivateCredential mockRaizDNIe;
    
    public static void main(String args[]) throws Exception {
        Contexto.inicializar();
        
        crearMockServidoresVotacion();
        crearMockRaizDNIe();
        File file = File.createTempFile("mockDnie", ".jks");
        crearMockDNIe("1234567A", new File("/home/jgzornoza/github/recursos/mockDnie.jks"));
    }
    
    public static void crearMockServidoresVotacion() throws Exception {
        File rootFileDir = new File("/home/jgzornoza/github/recursos/SistemaVotacion/");
        rootFileDir.mkdirs();
        File rootFile = new File("/home/jgzornoza/github/recursos/SistemaVotacion/root.jks");
        X500PrivateCredential rootCredential = crearMockUsuarioVotacion("1234567B", rootFile);
        File controlAccesoFile = new File("/home/jgzornoza/github/recursos/SistemaVotacion/ControlAcceso.jks");
        crearMockServidorVotacion(controlAccesoFile, rootCredential, 
                "CN=Control Acceso, SERIALNUMBER=1111111111A", "ClavesControlAcceso");
        File centroControlFile = new File("/home/jgzornoza/github/recursos/SistemaVotacion/CentroControl.jks");
        crearMockServidorVotacion(centroControlFile, rootCredential, 
                "CN=Centro Control, SERIALNUMBER=2222222222B", "ClavesCentroControl");
    }
    
    public static KeyStore crearMockRaizDNIe() throws Exception {
        return KeyStoreUtil.createRootKeyStore(ContextoPruebas.COMIEZO_VALIDEZ_CERT, 
                ContextoPruebas.PERIODO_VALIDEZ_ALMACEN_RAIZ, 
                ContextoPruebas.PASSWORD.toCharArray(), ContextoPruebas.ROOT_ALIAS, 
                File.createTempFile("mockRaiz", ".jks").getAbsolutePath(), 
                "CN=Autoridad Certificadora Sistema de Votación, OU=DNIE_CA");
    }
    
    public static X500PrivateCredential obtenerMockRaizDNIePrivateCredential() throws Exception {
        if (mockRaizDNIe != null) return mockRaizDNIe;
        //logger.debug("Inicializando mock raiz dnie en ->" + RUTA_MOCK_RAIZ_DNIE);
        //File mockRaizDNIeFile = new File(RUTA_MOCK_RAIZ_DNIE);
        File mockRaizDNIeFile = File.createTempFile("mockRaiz", ".jks");
        KeyStore rootKeyStore = crearMockRaizDNIe();
        X509Certificate rootCertificate = (X509Certificate)rootKeyStore.getCertificate(ContextoPruebas.ROOT_ALIAS);
        PrivateKey rootPK = (PrivateKey) rootKeyStore.getKey(ContextoPruebas.ROOT_ALIAS, 
                ContextoPruebas.PASSWORD.toCharArray());
        mockRaizDNIe = new X500PrivateCredential(
            rootCertificate,rootPK,ContextoPruebas.ROOT_ALIAS);
        //X509Certificate rootCert = (X509Certificate)rootKeyStore.getCertificate(ContextoPruebas.ROOT_ALIAS);
        return mockRaizDNIe;
    }

    public static KeyStore crearMockDNIe(String userID, File file) throws Exception {
        X500PrivateCredential rootCredential = obtenerMockRaizDNIePrivateCredential();
        KeyStore keyStore = KeyStoreUtil.createActorKeyStore(ContextoPruebas.COMIEZO_VALIDEZ_CERT,
                ContextoPruebas.PERIODO_VALIDEZ_CERT, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.END_ENTITY_ALIAS, rootCredential, "CN=Usuario Sistema de Votación, SERIALNUMBER=" + userID); 
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, ContextoPruebas.PASSWORD.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
        return keyStore;
    }
    
    public static KeyStore crearMockDNIe(String userID, File file, 
            X500PrivateCredential rootCredential) throws Exception {
        KeyStore keyStore = KeyStoreUtil.createActorKeyStore(ContextoPruebas.COMIEZO_VALIDEZ_CERT,
                ContextoPruebas.PERIODO_VALIDEZ_CERT, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.END_ENTITY_ALIAS, rootCredential, 
                "CN=Usuario " + userID + " Sistema de Votación, SERIALNUMBER=" + userID); 
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, ContextoPruebas.PASSWORD.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
        return keyStore;
    }
    
    public static X500PrivateCredential crearMockUsuarioVotacion(String userID, File file) throws Exception {
        X500PrivateCredential rootCredential = obtenerMockRaizDNIePrivateCredential();
        KeyStore keyStore = KeyStoreUtil.createActorKeyStore(ContextoPruebas.COMIEZO_VALIDEZ_CERT,
                ContextoPruebas.PERIODO_VALIDEZ_CERT, ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.END_ENTITY_ALIAS, rootCredential, "CN=Usuario Sistema de Votación, SERIALNUMBER=" + userID); 
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