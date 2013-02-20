package pruebas;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.seguridad.PKCS10WrapperServer;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author jgzornoza
 * Para simular un ciclo ejecutar en este orden : CsrTest, CreatedSignedCSR,
 * ValidateSignatureFromCSRSignedCert
 * 
 */
public class CsrTest {
    
    private static Logger logger = (Logger) LoggerFactory.getLogger(CsrTest.class);
    
    public static final String directorioBase = "/home/jgzornoza/git/recursos/csr/";
    
    public static String cadenaVerisign = directorioBase + "cadenaVerisign.pem";
    
    public static String cadenaCSR = directorioBase + "cadenaCSR.pem";
    
    //Datos del almacén que el Control de Acceso crea para firmar las CSR del usuario.
    //Se crea uno para cada votación
    public static final int PERIODO_VALIDEZ_ALMACEN_RAIZ = 1000000000;//En producción durará lo que dure una votación
    public static String keyStoreFirmaCSRPath = directorioBase + "keyStoreFirmaCSR.jks";
    public static String password = "PemPass";
    public static String firmaCSRKeyAlias = "aliasRoot";
    public static KeyStore keyStore;
    public static long comienzo = System.currentTimeMillis();
    
    //Datos del almacén con el que el Control de Acceso firma los votos Válidos
    public static String keyStoreFirmaRecibosServPath = directorioBase + "KeyStoreVerisign.jks";
    public static String serverKeyPassword = "PemPass";
    public static String firmaRecibosServerKeyAlias = "clavessistemavotacion";
    public static int periodoValidezCertFirmadoEnServ = 240000000;
    
    //Datos del almacén con el que el cliente firma sus votos
    public static String keyStoreFirmaVotosPath = directorioBase + "KeyStoreVerisign.jks";
    public static String keyStoreFirmaVotosPass = "PemPass";
    public static String keyStoreFirmaVotosAlias = "clavessistemavotacion";
    public static final String keyStoreFirmaVotosSignedFilePath = CsrTest.directorioBase + "votoFirmadoUsuarioConCSR";
    
    public static String strSubjectDNRoot = "CN=eventoUrl:sistemavotacion.cloudfundry.com, OU=Votaciones";
    
     public static String strSubjectDNEndEntity = "CN=hash_iufdhfiuhrf, OU=Votaciones";
    
    public static PKCS10WrapperClient wrapperClient;
    
    public static void main (String[] args) throws Exception {
        Date fecha = new Date();
        fecha.getTime();
        Contexto.inicializar();
        createKeyStore();
        //generar almacen de claves en servidor (cuando se publica el evento)
        KeyStore keyStore = KeyStoreUtil.createKeyStore(comienzo,
                PERIODO_VALIDEZ_ALMACEN_RAIZ, password.toCharArray(), 
                firmaCSRKeyAlias, keyStoreFirmaCSRPath, strSubjectDNRoot, strSubjectDNEndEntity);
        //generarAlmacenClaves y csr en cliente();
        byte[] csrBytes = testPKCS10WrapperClient();
        logger.debug("csrBytes: " + new String(csrBytes));
        //se envía la solicitud al servidor y se espera la cadena de certificación
        //que debe incluir el certificado firmado
        byte[] cadenaCertificada = testPKCS10WrapperServer(csrBytes);
        logger.debug("Cadena certif: " + new String(cadenaCertificada));
        generarAlmacenClavesFirmaVoto(cadenaCertificada);
        //Se genera en el cliente el almacén de claves con el certificado firmado por
        // el control de acceso (un SuperToken), se puede utilizar para mil cosas mas
    }
    
    
    public static byte[] testPKCS10WrapperClient () throws Exception { 
        wrapperClient = new PKCS10WrapperClient(1024, "RSA", "SHA1withRSA", 
                "BC", "eventoURL_sistemavotacioncontrolacceso.cloudfoundry.com", "1234",
                "hash_iufdvdv-nmfoipfdn-iouwxmpw-xieuphpgohrgpthb");
        byte[] pemEncoded = wrapperClient.getPEMEncodedRequestCSR();
        return pemEncoded;
    }
    
    public static byte[] testPKCS10WrapperServer (byte[] csrBytes) throws Exception { 
        //X509Name subject =csr.getCertificationRequestInfo().getSubject();
        //X509Principal principal = new X509Principal(subject);
        //logger.debug("Principal: " + principal.toString());
        
        File serverKeyStoreFile = new File(keyStoreFirmaCSRPath);   
        KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
            FileUtils.getBytesFromFile(serverKeyStoreFile), serverKeyPassword.toCharArray());
        PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(firmaCSRKeyAlias, serverKeyPassword.toCharArray());
        X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(firmaCSRKeyAlias);
        PKCS10WrapperServer wrapper = new PKCS10WrapperServer(privateKeySigner, certSigner);
        byte[] cadenaCertificacion = wrapper.firmarCsr(csrBytes, comienzo, periodoValidezCertFirmadoEnServ);
        return cadenaCertificacion;
    }
    
    public static void generarAlmacenClavesFirmaVoto (byte[] cadeCert) {
        try {  
            Collection<X509Certificate> certificados = 
            		CertUtil.fromPEMToX509CertCollection(cadeCert);
            logger.debug("Número certificados en cadena: " + certificados.size());
            X509Certificate[] arrayCerts = new X509Certificate[certificados.size()];
            certificados.toArray(arrayCerts);
            KeyStore store = KeyStore.getInstance("JKS");
            store.load(null, null);
            store.setKeyEntry("AliasCertFirmado", wrapperClient.getPrivateKey(), "PemPass".toCharArray(), arrayCerts);
            byte[] storeBytes = KeyStoreUtil.getBytes(store, "PemPass".toCharArray());
            FileUtils.copyStreamToFile(new ByteArrayInputStream(storeBytes), new File(directorioBase + "KeyStoreFirmaVoto.jks"));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } 
    }
    
    public static void createKeyStore () throws Exception {
        String password = "dnie";
        KeyStore keyStore = KeyStoreUtil.createKeyStore(comienzo,
                PERIODO_VALIDEZ_ALMACEN_RAIZ, password.toCharArray(), 
                "rootAlias", "endEntityAlias", "CN=dummy_Dnie, OU=DNIE_CA", "CN=Usuario dnie, SERIALNUMBER=07553172H"); 
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes), 
                new File("/home/jgzornoza/git/recursos/dni_keystore/dni.jks"));
    }

}
