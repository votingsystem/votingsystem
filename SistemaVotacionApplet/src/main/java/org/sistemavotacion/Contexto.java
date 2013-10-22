package org.sistemavotacion;

import com.itextpdf.text.pdf.PdfName;
import iaik.pkcs.pkcs11.Mechanism;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.sf.json.JSONObject;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.red.HttpHelper;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.OSValidator;
import org.sistemavotacion.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public enum Contexto {
    
    INSTANCE;

    private Logger logger = LoggerFactory.getLogger(Contexto.class);
    
    private static ExecutorService executor;
    private static CompletionService<Respuesta> completionService;
    
    public static class DEFAULTS {
        public static String BASEDIR =  System.getProperty("user.home");
        public static String APPDIR =  BASEDIR + File.separator + 
                ".VotingSystem"  + File.separator;
        public static String APPTEMPDIR =  APPDIR + File.separator + "temp"
             + File.separator;
    }

    public static String TEMPDIR =  System.getProperty("java.io.tmpdir");
    
    
    public static String CANCEL_VOTE_FILE = "Anulador_";
    public static final String PREFIJO_USER_JKS = "usuario_"; 
    public static final String SUFIJO_USER_JKS = ".jks"; 
    
    public static String SOLICITUD_FILE = "SolicitudAcceso_";
    public static String VOTE_FILE = "Vote_";
    
    public static String APPVOTODIR =  DEFAULTS.APPDIR + File.separator + 
            "votes" + File.separator;
    
    //An smime.p7m file is an encrypted or signed email.
    public static final String SIGNED_PART_EXTENSION = ".p7m";
    
    public static final int MAXIMALONGITUDCAMPO = 255;

    public static final String CSR_FILE_NAME   = "csr";
    public static final String IMAGE_FILE_NAME   = "image";
    public static final String REPRESENTATIVE_DATA_FILE_NAME = "representativeData";
    
    public static final String ACCESS_REQUEST_FILE_NAME   = "accessRequest";    
    
    public static final String CERT_RAIZ_PATH = "AC_RAIZ_DNIE_SHA1.pem";
    public static final int KEY_SIZE = 1024;
    public static final String SIG_NAME = "RSA";
    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    public static final String ALIAS_CLAVES = "certificadovoto";
    public static final String PASSWORD_CLAVES = "certificadovoto";
        
    public static final String CERT_AUTENTICATION = "CertAutenticacion";
    public static final String CERT_SIGN = "CertFirmaDigital";
    public static final String CERT_CA = "CertCAIntermediaDGP";
    public static final String LABEL_CLAVE_PRIVADA_AUTENTICACION = "KprivAutenticacion";
    public static final String LABEL_CLAVE_PRIVADA_FIRMA = "KprivFirmaDigital";
        
    public static final Mechanism DNIe_SESSION_MECHANISM = Mechanism.SHA1_RSA_PKCS;
   // public static final Mechanism DNIe_SESSION_MECHANISM = Mechanism.RSA_X_509;
    public static final String DNIe_SIGN_MECHANISM = "SHA1withRSA";
    public static final String TIMESTAMP_DNIe_HASH = TSPAlgorithms.SHA1;
    ///public static final String DNIe_SIGN_MECHANISM = "SHA256withRSA";
    //public static final String DNIe_SIGN_MECHANISM = "SHA512withRSA";
    
    public static final String TIMESTAMP_VOTE_HASH = TSPAlgorithms.SHA512;
    public static final String VOTE_SIGN_MECHANISM = "SHA512withRSA";
    public static final String VOTING_DATA_DIGEST = "SHA256";
    
    public static final String PDF_SIGNATURE_DIGEST = "SHA1";
    public static final String PDF_SIGNATURE_MECHANISM = "SHA1withRSA";
    public static final String TIMESTAMP_PDF_HASH = TSPAlgorithms.SHA1;
    public static final PdfName PDF_SIGNATURE_NAME = PdfName.ADBE_PKCS7_SHA1;
    public static final String PDF_DIGEST_OID = CMSSignedDataGenerator.DIGEST_SHA1;


    public static final String DEFAULT_SIGNED_FILE_NAME = "smimeMessage.p7m";
    public static String CERT_STORE_TYPE = "Collection";
    
    
    public static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";
    public static final String JSON_CONTENT_TYPE    = "application/json";
    
    public static final String PDF_CONTENT_TYPE    = "application/pdf";
    public static final String SIGNED_CONTENT_TYPE = "application/x-pkcs7-signature";
    public static final String X509_CONTENT_TYPE = "application/x-x509-ca-cert";
    public static final String ENCRYPTED_CONTENT_TYPE = "application/x-pkcs7-mime";
    public static final String SIGNED_AND_ENCRYPTED_CONTENT_TYPE = 
            SIGNED_CONTENT_TYPE + "," + ENCRYPTED_CONTENT_TYPE;
    public static final String PDF_SIGNED_AND_ENCRYPTED_CONTENT_TYPE = 
            PDF_CONTENT_TYPE + "," +  SIGNED_CONTENT_TYPE + ";" + ENCRYPTED_CONTENT_TYPE;    
    public static final String PDF_SIGNED_CONTENT_TYPE = 
    		PDF_CONTENT_TYPE + "," + SIGNED_CONTENT_TYPE;     
    public static final String PDF_ENCRYPTED_CONTENT_TYPE = 
    		PDF_CONTENT_TYPE + "," + ENCRYPTED_CONTENT_TYPE; 
    
    public static final int IMAGE_MAX_FILE_SIZE_KB = 512;
    public static final int IMAGE_MAX_FILE_SIZE = IMAGE_MAX_FILE_SIZE_KB * 1024;
    public static final int SIGNED_MAX_FILE_SIZE_KB = 512;
    public static final int SIGNED_MAX_FILE_SIZE = SIGNED_MAX_FILE_SIZE_KB * 1024;
    
    
    private Usuario usuario;
    private HttpHelper httpHelper;
    private ResourceBundle resourceBundle;
    private Map<String, ReciboVoto> receiptMap;
    private ActorConIP accessControl;
    private ActorConIP controlCenter;
    private AppHost appHost;
    
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private Contexto () { 
        logger.debug("------------- Contexto ----------------- ");
    }

    public void init(AppHost appHost){
        logger.debug("------------- init ----------------- ");
        this.appHost = appHost;
        httpHelper = new HttpHelper();
        try {
            new File(DEFAULTS.APPDIR).mkdir();
            new File(DEFAULTS.APPTEMPDIR).mkdir();
            File copiaRaizDNI = new File(DEFAULTS.APPDIR + CERT_RAIZ_PATH);
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(CERT_RAIZ_PATH), copiaRaizDNI);
            OSValidator.initClassPath();
            resourceBundle = ResourceBundle.getBundle(
                    "messages_" + AppletFirma.locale);
        } catch (Exception ex) {
            LoggerFactory.getLogger(Contexto.class).error(ex.getMessage(), ex);
        } 
    }
    
    public void sendMessageToHost(Operacion operacion) {
        appHost.sendMessageToHost(operacion);
    }
    
    public Operacion getPendingOperation() {
        return appHost.getPendingOperation();
    }
    
    public void shutdown() {
        try {
            logger.debug("------------- shutdown ----------------- ");
            if(executor != null) executor.shutdown();
            if(httpHelper != null) httpHelper.shutdown();
            FileUtils.deleteRecursively(new File(DEFAULTS.APPTEMPDIR));
        } catch (IOException ex) {
           logger.error(ex.getMessage(), ex);
        }
    }
    
    public void submit(Runnable runnable) {
        if(executor == null || executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(10);
            completionService = 
                    new ExecutorCompletionService<Respuesta>(executor);
        }
        executor.submit(runnable);
    }
    
    public Future<Respuesta> submit(Callable<Respuesta> callable) {
        return completionService.submit(callable);
    }
    
    public void initMultiThreadedHttp() {
        httpHelper.initMultiThreadedMode();
    }
    
    public String getURLTimeStampServer() {
        if(accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "timeStamp";
    }
    
    public X509Certificate getTimeStampServerCert() {
        if(accessControl == null) return null;
        return accessControl.getTimeStampCert();
    }
    
    
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
   
    public String getString(String key, Object... arguments) {
        String pattern = resourceBundle.getString(key);
        if(arguments.length > 0) 
            return MessageFormat.format(pattern, arguments);
        else return pattern;
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

    public void setUsuario(Usuario usuario) {
    	logger.debug("setUsuario - nif: " + usuario.getNif());
        this.usuario = usuario;
    }

    public static String getRutaArchivosVoto(Evento evento, String nif) {
        String ruta = APPVOTODIR + StringUtils.getCadenaNormalizada(
                evento.getControlAcceso().getServerURL()) +
                "-" + nif + "-" + evento.getEventoId() + File.separator;
        return ruta;
    }
    
    public void addReceipt(String hashCertVoteBase64, ReciboVoto receipt) {
        if(receiptMap == null) receiptMap = new HashMap<String, ReciboVoto>();
        receiptMap.put(hashCertVoteBase64, receipt);
    }
    
    public ReciboVoto getReceipt(String hashCertVoteBase64) {
        if(receiptMap == null || hashCertVoteBase64 == null) return null;
        return receiptMap.get(hashCertVoteBase64);
    }
       
        
    public JSONObject getVoteCancelationInSession(String hashCertVoteBase64) {
        logger.debug("getVoteCancelationInSession");
        if(receiptMap == null) return null;
        ReciboVoto recibo = receiptMap.get(hashCertVoteBase64);
        if(recibo == null) return null;
        Evento voto = recibo.getVoto();
        return voto.getCancelVoteJSON();
    } 
    
    public Usuario getUsuario() {
        return usuario;
    }

    public HttpHelper getHttpHelper() {
        return httpHelper;
    }

    /**
     * @return the accessControl
     */
    public ActorConIP getAccessControl() {
        return accessControl;
    }

    /**
     * @param accessControl the accessControl to set
     */
    public void setAccessControl(ActorConIP accessControl) {
        this.accessControl = accessControl;
    }

    /**
     * @return the controlCenter
     */
    public ActorConIP getControlCenter() {
        return controlCenter;
    }

    /**
     * @param controlCenter the controlCenter to set
     */
    public void setControlCenter(ActorConIP controlCenter) {
        this.controlCenter = controlCenter;
    }
    
            
    public static String getUserKeyStorePath (String userNIF, String basePath) {
        String userDirPath = getUserDirPath(userNIF, basePath);
        new File(userDirPath).mkdirs();
        return  userDirPath + PREFIJO_USER_JKS + userNIF + SUFIJO_USER_JKS;
    }
    
    public static File copyKeyStoreToUserDir(byte[] keyStoreBytes, String userNIF,
            String basePath) throws Exception {
        String userDirPath = getUserDirPath(userNIF, basePath);
        File userDir = new File(userDirPath);
        userDir.mkdirs();
        File keyStroreFile = new File(userDir.getAbsolutePath() + 
                PREFIJO_USER_JKS + userNIF + SUFIJO_USER_JKS);
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes), keyStroreFile);
        return keyStroreFile;
    }
    
    public static String getUserDirPath (String userNIF, String basePath) {
        int subPathLength = 3;
        while (userNIF.length() > 0) {
            if(userNIF.length() <= subPathLength) subPathLength = userNIF.length();
            String subPath = userNIF.substring(0, subPathLength);
            userNIF = userNIF.substring(subPathLength);
            basePath = basePath + subPath + File.separator;
        }
        return basePath;
    }

}