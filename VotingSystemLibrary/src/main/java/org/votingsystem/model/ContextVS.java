package org.votingsystem.model;

import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;
import org.votingsystem.util.FileUtils;

import javax.mail.Session;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.votingsystem.util.OSValidator;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class ContextVS {

    private static Logger logger = Logger.getLogger(ContextVS.class);

    public static Session MAIL_SESSION = Session.getDefaultInstance(System.getProperties(), null);

    static { Security.addProvider(new BouncyCastleProvider()); }

    public static String BASEDIR =  System.getProperty("user.home");
    public static String APPDIR =  BASEDIR + File.separator +  ".VotingSystem"  + File.separator;
    public static String APPTEMPDIR =  APPDIR + File.separator + "temp" + File.separator;
    public static String TEMPDIR =  System.getProperty("java.io.tmpdir");
    
    public static String CANCEL_VOTE_FILE = "Anulador_";
    public static final String PREFIJO_USER_JKS = "userVS_";
    public static final String SUFIJO_USER_JKS = ".jks";

    public static String SOLICITUD_FILE = "SolicitudAcceso_";
    public static String VOTE_FILE = "Vote_";

    public static final int MAXIMALONGITUDCAMPO = 255;

    public static final String CSR_FILE_NAME   = "csr";
    public static final String IMAGE_FILE_NAME   = "image";
    public static final String REPRESENTATIVE_DATA_FILE_NAME = "representativeData";

    public static final String ACCESS_REQUEST_FILE_NAME   = "accessRequest";

    public static final String CERT_RAIZ_PATH = "AC_RAIZ_DNIE_SHA1.pem";
    public static final int KEY_SIZE = 1024;
    public static final String SIG_NAME = "RSA";
    /** Random Number Generator algorithm. */
    private static final String ALGORITHM_RNG = "SHA1PRNG";
    public static final String CERT_GENERATION_SIG_ALGORITHM = "SHA1WithRSAEncryption";
    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    public static final String ALIAS_CLAVES = "certificadovoto";
    public static final String PASSWORD_CLAVES = "certificadovoto";

    public static final String CERT_AUTENTICATION = "CertAutenticacion";
    public static final String CERT_SIGN = "CertFirmaDigital";
    public static final String CERT_CA = "CertCAIntermediaDGP";
    public static final String DNIe_AUTH_PRIVATE_KEY_LABEL = "KprivAutenticacion";
    public static final String DNIe_SIGN_PRIVATE_KEY_LABEL = "KprivFirmaDigital";

    // public static final Mechanism DNIe_SESSION_MECHANISM = Mechanism.RSA_X_509;
    public static final String DNIe_SIGN_MECHANISM = "SHA1withRSA";
    public static final String TIMESTAMP_DNIe_HASH = TSPAlgorithms.SHA1;

    public static final String SIGN_MECHANISM = "SHA256withRSA";

    public static final String TIMESTAMP_VOTE_HASH = TSPAlgorithms.SHA512;
    public static final String VOTE_SIGN_MECHANISM = "SHA512withRSA";
    public static final String VOTING_DATA_DIGEST = "SHA256";

    public static final String PDF_SIGNATURE_DIGEST = "SHA1";
    public static final String PDF_SIGNATURE_MECHANISM = "SHA1withRSA";
    public static final String TIMESTAMP_PDF_HASH = TSPAlgorithms.SHA1;
    public static final String PDF_DIGEST_OID = CMSSignedDataGenerator.DIGEST_SHA1;


    public static final String DEFAULT_SIGNED_FILE_NAME = "smimeMessage.p7m";
    public static String CERT_STORE_TYPE = "Collection";

    public static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";

    public static final int IMAGE_MAX_FILE_SIZE_KB = 512;
    public static final int IMAGE_MAX_FILE_SIZE = IMAGE_MAX_FILE_SIZE_KB * 1024;
    public static final int SIGNED_MAX_FILE_SIZE_KB = 512;
    public static final int SIGNED_MAX_FILE_SIZE = SIGNED_MAX_FILE_SIZE_KB * 1024;

    public static final String MULTISIGNED_FILE_NAME = "MultiSign";

    //smime.p7m -> Email message encrypted
    //smime.p7s -> Email message that includes a digital signature
    public static final String SIGNED_PART_EXTENSION = ".p7s";

    private Map<String, VoteVS> receiptMap;

    private AppHostVS appHost;
    private static ResourceBundle resourceBundle;
    private UserVS userVS;
    private ActorVS accessControl;
    private ActorVS controlCenter;
    private static ContextVS INSTANCE;

    private ContextVS(){}

    private ContextVS(AppHostVS appHost, String localizatedMessagesFileName, String locale) {
        this.appHost = appHost;
        resourceBundle = ResourceBundle.getBundle(
                localizatedMessagesFileName + locale);
    }

    public static void init (AppHostVS appHost, String logPropertiesFile,
                             String localizatedMessagesFileName, String locale) throws Exception {
        Properties props = new Properties();
        props.load(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(logPropertiesFile));
        PropertyConfigurator.configure(props);
        VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
        INSTANCE = new ContextVS(appHost, localizatedMessagesFileName, locale);
    }

    public static void init () throws Exception {
        INSTANCE = new ContextVS();
        VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
    }


    public static void initSignatureApplet (AppHostVS appHost, String logPropertiesFile, String localizatedMessagesFileName, String locale){
        try {
            logger.debug("------------- initSignatureApplet ----------------- ");
            new File(APPDIR).mkdir();
            new File(APPTEMPDIR).mkdir();
            init(appHost, logPropertiesFile,localizatedMessagesFileName, locale);
            File copiaRaizDNI = new File(APPDIR + CERT_RAIZ_PATH);
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(CERT_RAIZ_PATH), copiaRaizDNI);
            OSValidator.initClassPath();
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ContextVS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static ContextVS getInstance() { 
        if(INSTANCE == null) logger.error("### ContextVS isn't initialized ###");
        return INSTANCE; 
    }
    
    public void shutdown() {
        try {
            logger.debug("------------- shutdown ----------------- ");
            FileUtils.deleteRecursively(new File(APPTEMPDIR));
        } catch (IOException ex) {
           logger.error(ex.getMessage(), ex);
        }
    }

    public void sendMessageToHost(OperationVS operacion) { appHost.sendMessageToHost(operacion); }

    public void setSessionUser(UserVS userVS) {
        logger.debug("setSessionUser - nif: " + userVS.getNif());
        this.userVS = userVS;
    }

    public UserVS getSessionUser() { return userVS; }

    public AppHostVS getAppHost() { return appHost;  }

    public void setAppHost(AppHostVS appHost) { this.appHost = appHost;  }


    public X509Certificate getTimeStampServerCert() {
        if(accessControl == null) return null;
        return accessControl.getTimeStampCert();
    }
    
    public VoteVS getSessionVote(String hashCertVoteBase64) {
        logger.debug("getSessionVote");
        if(receiptMap == null || hashCertVoteBase64 == null) {
            logger.debug("getSessionVote - receiptMap: " + receiptMap + " - hashCertVoteBase64: " + hashCertVoteBase64);
            return null;
        }
        return receiptMap.get(hashCertVoteBase64);
    }
    
    public void setSessionVote(String hashCertVoteBase64, VoteVS receipt) {
        if(receiptMap == null) receiptMap = new HashMap<String, VoteVS>();
        receiptMap.put(hashCertVoteBase64, receipt);
    }

    public ActorVS getAccessControl() { return accessControl; }

    public void setAccessControl(ActorVS accessControl) { this.accessControl = accessControl; }

    public ActorVS getControlCenter() { return controlCenter; }

    public void setControlCenter(ActorVS controlCenter) { this.controlCenter = controlCenter; }

    public static String getMessage(String key, Object... arguments) {
        try {
            String pattern = resourceBundle.getString(key);
            if(arguments.length > 0) return MessageFormat.format(pattern, arguments);
            else return resourceBundle.getString(key);
        } catch(Exception ex) {
            logger.error("### Value not found for key: " + key);
            return "---" + key + "---";
        }
    }

    public static Icon getIcon(Object baseObject, String key) {
        String iconPath = null;
        String iconName = null;
        Icon icon = null;
        if(key.endsWith("_16")) {
            iconName = key.substring(0, key.indexOf("_16"));
            iconPath = "/resources/fatcow_16/" + iconName + ".png";
        } else if(key.endsWith("_32")) {
            iconName = key.substring(0, key.indexOf("_32"));
            iconPath = "/resources/fatcow_32/" + iconName + ".png";
        } else {//defaults to 16x16 icons
             iconPath = "/resources/fatcow_16/" + key + ".png";
        }
        try {
            icon = new ImageIcon(baseObject.getClass().getResource(iconPath));
        } catch(Exception ex) {
            logger.error(" ### iconPath: " + iconPath + " not found");
            icon = new ImageIcon(baseObject.getClass().getResource(
                    "/resources/fatcow_32/button_default.png"));
        }
        return icon;
    }


}