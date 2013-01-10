package org.sistemavotacion;

import com.itextpdf.text.pdf.PdfName;
import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;
import java.util.ResourceBundle;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.red.HttpHelper;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.OSValidator;
import org.sistemavotacion.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class Contexto {

    private static Logger logger = LoggerFactory.getLogger(Contexto.class);

    public static boolean IS_TIME_STAMPED_SIGNATURE = true; 

    public static final String NOMBRE_ARCHIVO_ALMACEN_CLAVES = "AlmacenClaves.jks";
    public static final String NOMBRE_ARCHIVO_SOLICITUD_ACCESO = "AccessRequest";
    public static final String NOMBRE_ARCHIVO_SOLICITUD_ACCESO_TIMESTAMPED 
            = "AccessRequestTimeStamped";
    public static final String NOMBRE_ARCHIVO_ENVIADO_FIRMADO = "archivoFirmado";
    public static final String NOMBRE_ARCHIVO_CSR_ENVIADO = "csr";
    
    public static final String CERT_RAIZ_PATH = "AC_RAIZ_DNIE_SHA1.pem";
    public static final int KEY_SIZE = 1024;
    public static final String SIG_NAME = "RSA";
    public static final String PROVIDER = "BC";
    public static final String ALIAS_CLAVES = "certificadovoto";
    public static final String PASSWORD_CLAVES = "certificadovoto";
        
    public static final String CERT_AUTENTICATION = "CertAutenticacion";
    public static final String CERT_SIGN = "CertFirmaDigital";
    public static final String CERT_CA = "CertCAIntermediaDGP";
    public static final String LABEL_CLAVE_PRIVADA_AUTENTICACION = "KprivAutenticacion";
    public static final String LABEL_CLAVE_PRIVADA_FIRMA = "KprivFirmaDigital";
        
    public static String NOMBRE_ARCHIVO_FIRMADO = "EventoEnviado";
    public static final Mechanism DNIe_SESSION_MECHANISM = Mechanism.SHA1_RSA_PKCS;
   // public static final Mechanism DNIe_SESSION_MECHANISM = Mechanism.RSA_X_509;
    public static final String DNIe_SIGN_MECHANISM = "SHA1withRSA";
    public static final String TIMESTAMP_DNIe_HASH = TSPAlgorithms.SHA1;
    ///public static final String DNIe_SIGN_MECHANISM = "SHA256withRSA";
    //public static final String DNIe_SIGN_MECHANISM = "SHA512withRSA";
    
    public static final String TIMESTAMP_VOTE_HASH = TSPAlgorithms.SHA512;
    public static final String VOTE_SIGN_MECHANISM = "SHA512withRSA";
    
    public static final String PDF_SIGNATURE_DIGEST = "SHA1";
    public static final String TIMESTAMP_PDF_HASH = TSPAlgorithms.SHA1;
    public static final PdfName PDF_SIGNATURE_NAME = PdfName.ADBE_PKCS7_SHA1;
    public static final String PDF_DIGEST_OID = CMSSignedDataGenerator.DIGEST_SHA1;

    //hashing method of the voting data
    public static final String VOTING_DATA_DIGEST = "SHA512";
    
    public static final String SIGN_PROVIDER = "BC";
    public static final String DEFAULT_SIGNED_FILE_NAME = "smimeMessage";
    public static String CERT_STORE_TYPE = "Collection";
    
    public static final String SIGNED_PART_EXTENSION = ".p7s";
    
    public static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";
    
    private static Contexto instancia;
    private static Usuario usuario;
    private static String DNIePassword;
    private static KeyStore almacenClaves;
    private static final HttpHelper httpHelper = new HttpHelper();
    
    private static PKCS10WrapperClient pkcs10WrapperClient;
    private static Evento voto;
    private static ResourceBundle resourceBundle;
    
    private Contexto () { }

    public static Contexto inicializar () throws Exception {
        if (instancia == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            instancia = new Contexto();
            instancia.checkFiles();
            OSValidator.initClassPath();
            resourceBundle = ResourceBundle.getBundle("messages_" + AppletFirma.locale);
        }
        return instancia;
    }

    public static Contexto getInstancia () throws Exception {
        return inicializar();
    }
    
    public static ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
    
    public static String getString(String key) {
        return resourceBundle.getString(key);
    }

    
    private synchronized void checkFiles () throws Exception {
        logger.debug("checkFiles - checkFiles");
        new File(FileUtils.APPDIR).mkdir();
        new File(FileUtils.APPTEMPDIR).mkdir();
        //File directorioBaseDeAplicacion = getApplicactionBaseDirFile();
        File copiaRaizDNI = new File(FileUtils.APPDIR + CERT_RAIZ_PATH);
        FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(CERT_RAIZ_PATH), copiaRaizDNI);
    }

    public static String getApplicactionBaseDir () {
        File file = new File(Contexto.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath());
        return file.getAbsolutePath() + File.separator;
    }
    
    public static File getApplicactionBaseDirFile () {
        File file = new File(Contexto.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath());
        return file;
    }

    public static void setUsuario(Usuario usu) {
    	logger.debug("Poniendo usuario en contexto - usuario: " + usu.getNif());
        if (usuario == null && usu.getNif() != null 
                || !usuario.getNif().equals(usu.getNif())) {
            //getPropiedades().ponerUsuarioEnSesion(usu);
            //Se resetea contexto para nuevo usuario
            usuario = usu;
        }
    }
    
    /**
     * @return the pkcs10WrapperClient
     */
    public static PKCS10WrapperClient initPkcs10WrapperClient(Evento evento) throws Exception{
        pkcs10WrapperClient = new PKCS10WrapperClient(KEY_SIZE, SIG_NAME, 
                VOTE_SIGN_MECHANISM, PROVIDER, evento.getControlAcceso().getServerURL(), 
                evento.getEventoId().toString() , evento.getHashCertificadoVotoHex());
        return pkcs10WrapperClient;
    }

    public static KeyStore getKeyStoreVoto(Evento evento, String nif) throws Exception {
        logger.debug("getKeyStoreVoto - documento.getEventoId(): " + evento.getEventoId());
        File directorioAlmacen = new File (getRutaArchivosVoto(evento, nif)); 
        directorioAlmacen.mkdirs();
        File archivoAlmacen = new File (getRutaArchivosVoto(evento, nif) + 
                NOMBRE_ARCHIVO_ALMACEN_CLAVES);
        boolean archivoCreado = false;            
        try {
            archivoCreado = archivoAlmacen.createNewFile();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        if (!archivoCreado && archivoAlmacen.length()> 0) {
            logger.debug("El usuario '" + nif + "'"+ " ya había solicitado un almacén para el evento."
                    + "Obteniendo '" + archivoAlmacen.getAbsolutePath() + "' del sistema de ficheros");
            almacenClaves = KeyStoreUtil.getKeyStoreFromFile(
                    archivoAlmacen.getAbsolutePath(), PASSWORD_CLAVES.toCharArray());
            voto = evento;
        } else return null;
        return almacenClaves;
    }
    
    /**
     * @param voto the voto to set
     */
    public static void setVoto(Evento votoEvento, String nif) throws Exception {
        voto = votoEvento;
        File directorioArchivoVoto = new File (getRutaArchivosVoto(voto, nif));
        directorioArchivoVoto.mkdirs();
        File archivoVoto = new File (directorioArchivoVoto.getAbsolutePath() 
                + File.separator + Operacion.Tipo.ENVIO_VOTO_SMIME.getNombreArchivoEnDisco());
        InputStream is = new ByteArrayInputStream(
                votoEvento.obtenerVotoJSONStr().getBytes());
        FileUtils.copyStreamToFile(is, archivoVoto);
    }

    
    public static String getRutaArchivosVoto(Evento evento, String nif) {
        String ruta = FileUtils.APPVOTODIR + StringUtils.getCadenaNormalizada(
                evento.getControlAcceso().getServerURL()) +
                "-" + nif + "-" + evento.getEventoId() + File.separator;
        return ruta;
    }
    
    public static Usuario getUsuario() {
        //TODO
        //Usuario usuario = new Usuario();
        //usuario.setNif("7553172h");
        return usuario;
    }

    public static void setDNIePassword(String password) {
        DNIePassword = password;
    }

    public static String getDNIePassword() {
        return DNIePassword;
    }

    public static HttpHelper getHttpHelper() {
        return httpHelper;
    }

}