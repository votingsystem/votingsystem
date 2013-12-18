package org.votingsystem.model;

import com.itextpdf.text.pdf.PdfName;
import iaik.pkcs.pkcs11.Mechanism;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;
import org.votingsystem.util.FileUtils;

import javax.mail.Session;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import javax.security.auth.x500.X500PrivateCredential;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.votingsystem.util.NifUtils;
import org.votingsystem.util.OSValidator;
import org.votingsystem.util.StringUtils;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class ContextVS {

    private static Logger logger = Logger.getLogger(ContextVS.class);

    public static Session MAIL_SESSION = Session.getDefaultInstance(System.getProperties(), null);

    static { Security.addProvider(new BouncyCastleProvider()); }

    public static final int ACCESS_CONTROL_URL_TAG                  = 0;
    public static final int EVENT_ID_TAG                            = 1;
    public static final int HASH_CERT_VOTE_TAG                      = 2;
    public static final int REPRESENTATIVE_URL_TAG                  = 3;
    public static final int ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG = 4;

    public static final String VOTING_SYSTEM_BASE_OID = "0.0.0.0.0.0.0.0.0.";
    public static final String ACCESS_CONTROL_OID     = VOTING_SYSTEM_BASE_OID + ACCESS_CONTROL_URL_TAG;
    public static final String EVENT_ID_OID           = VOTING_SYSTEM_BASE_OID + EVENT_ID_TAG;
    public static final String HASH_CERT_VOTE_OID     = VOTING_SYSTEM_BASE_OID + HASH_CERT_VOTE_TAG;
    public static final String REPRESENTATIVE_URL_OID = VOTING_SYSTEM_BASE_OID + REPRESENTATIVE_URL_TAG;
    public static final String ANONYMOUS_REPRESENTATIVE_DELEGATION_OID = VOTING_SYSTEM_BASE_OID +
            ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG;

    public static final Mechanism DNIe_SESSION_MECHANISM = Mechanism.SHA1_RSA_PKCS;
    public static final PdfName PDF_SIGNATURE_NAME = PdfName.ADBE_PKCS7_SHA1;

    public static String APPDIR;
    public static String APPTEMPDIR;
    public static String ERROR_DIR;

    public static String VOTE_FILE = "Vote_";
    public static String CANCEL_VOTE_FILE = "Anulador_";

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

    //For tests environments
    private static final String ROOT_ALIAS = "rootAlias";
    public static final String END_ENTITY_ALIAS = "endEntityAlias";
    public static final String PASSWORD = "PemPass";
    private static final long CERT_VALID_FROM = System.currentTimeMillis();
    private static final long ROOT_KEYSTORE_PERIOD = 20000000000L;
    private static final long USER_KEYSTORE_PERIOD = 20000000000L;


    private UserVS userTest;
    private X509Certificate rootCACert;
    private KeyStore rootCAKeyStore;
    private PrivateKey rootCAPrivateKey;
    private X500PrivateCredential rootCAPrivateCredential;
    private Locale locale = new Locale("es");


    private Map<String, ResponseVS> hashCertVSDataMap;
    private AppHostVS appHost;
    private static Properties appProperties;
    private UserVS userVS;
    private AccessControlVS accessControl;
    private ControlCenterVS controlCenter;
    private static ContextVS INSTANCE;

    private ContextVS(){
        try {
            initDirs(System.getProperty("user.home"));
            appProperties = new Properties();
            appProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    "votingSystemLibraryMessages" + "_" + locale +  ".properties"));
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private ContextVS(AppHostVS appHost, String logPropertiesFile, String localizatedMessagesFileName, String locale) {
        this.appHost = appHost;
        try {
            initDirs(System.getProperty("user.home"));
            if(logPropertiesFile != null) {
                Properties props = new Properties();
                props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(logPropertiesFile));
                PropertyConfigurator.configure(props);
            }
            appProperties = new Properties();
            String messagesFileName = null;
            if(locale == null) messagesFileName = "votingSystemLibraryMessages" + ".properties";
            else messagesFileName = "votingSystemLibraryMessages" + "_" + locale +  ".properties";
            appProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(messagesFileName));
            if(localizatedMessagesFileName != null) {
                appProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        localizatedMessagesFileName.split("\\.")[0] + "_" + locale + ".properties"));
            }
        } catch (Exception ex) {
            logger.error("localizatedMessagesFileName: " + localizatedMessagesFileName);
            logger.error(ex.getMessage(), ex);
        }
    }

    private void initDirs(String baseDir) {
        APPDIR =  baseDir + File.separator +  ".VotingSystem";
        APPTEMPDIR =  APPDIR + File.separator + "temp";
        ERROR_DIR =  APPDIR + File.separator + "error";
        new File(APPDIR).mkdir();
        new File(APPTEMPDIR).mkdir();
        new File(ERROR_DIR).mkdirs();
    }

    public static void init (AppHostVS appHost, String logPropertiesFile,
                     String localizatedMessagesFileName, String locale) throws Exception {
        VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
        INSTANCE = new ContextVS(appHost, logPropertiesFile, localizatedMessagesFileName, locale);
    }

    public static void init () throws Exception {
        INSTANCE = new ContextVS();
        VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
    }

    public static void initSignatureApplet (AppHostVS appHost, String logPropertiesFile,
            String localizatedMessagesFileName, String locale){
        try {
            logger.debug("------------- initSignatureApplet ----------------- ");
            init(appHost, logPropertiesFile,localizatedMessagesFileName, locale);
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(CERT_RAIZ_PATH),  new File(APPDIR + CERT_RAIZ_PATH));
            OSValidator.initClassPath();
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ContextVS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static ContextVS getInstance() { 
        if(INSTANCE == null)  INSTANCE = new ContextVS();
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

    public void initTestEnvironment(String appDir) throws Exception {
        logger.debug("--------  initTestEnvironment - appDir: " + appDir + "-------- ");
        if(appDir != null) initDirs(appDir);
        try {
            DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd' 'HH:mm:ss");
            String dateStr = formatter.format(new Date(CERT_VALID_FROM));
            String strSubjectDN = getMessage("rootCASubjectDN", dateStr);
            rootCAKeyStore = KeyStoreUtil.createRootKeyStore (CERT_VALID_FROM, ROOT_KEYSTORE_PERIOD,
                    PASSWORD.toCharArray(), ROOT_ALIAS, strSubjectDN);
            rootCACert = (X509Certificate)rootCAKeyStore.getCertificate(ROOT_ALIAS);
            rootCAPrivateKey = (PrivateKey)rootCAKeyStore.getKey(ROOT_ALIAS,PASSWORD.toCharArray());
            rootCAPrivateCredential = new X500PrivateCredential(rootCACert, rootCAPrivateKey,  ROOT_ALIAS);
            userTest = new UserVS();
            userTest.setNif(NifUtils.getNif(Integer.valueOf(getMessage("testUserNifNumber"))));
            userTest.setFirstName(getMessage("testUserFirstName"));
            userTest.setLastName(getMessage("testUserLastName"));
            userTest.setEmail(getMessage("testUserEmail"));
            String testUserDN = getMessage("userDN", userTest.getFirstName(),userTest.getLastName(), userTest.getNif());
            KeyStore userKeySTore = KeyStoreUtil.createUserKeyStore(CERT_VALID_FROM, USER_KEYSTORE_PERIOD,
                    PASSWORD.toCharArray(), END_ENTITY_ALIAS, rootCAPrivateCredential, testUserDN);
            userTest.setKeyStore(userKeySTore);
        } catch (Exception ex) {
            logger.error (ex.getMessage(), ex);
        }
    }

    public X509Certificate getRootCACert() {
        return rootCACert;
    }

    public UserVS getUserTest() {
        return userTest;
    }

    public KeyStore generateKeyStore(String userNIF) throws Exception {
        KeyStore keyStore = KeyStoreUtil.createUserKeyStore(CERT_VALID_FROM, USER_KEYSTORE_PERIOD,
                PASSWORD.toCharArray(), END_ENTITY_ALIAS, rootCAPrivateCredential,
                "GIVENNAME=FirstName_" + userNIF + " ,SURNAME=lastName_" + userNIF + ", SERIALNUMBER=" + userNIF);
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, PASSWORD.toCharArray());
        String userSubPath = StringUtils.getUserDirPath(userNIF);
        copyFile(keyStoreBytes, userSubPath,  "userVS_" + userNIF + ".jks");
        return keyStore;
    }

    public SMIMEMessageWrapper genTestSMIMEMessage (String toUser, String textToSign,
            String subject) throws Exception {
        KeyStore keyStore = userTest.getKeyStore();
        PrivateKey privateKey = (PrivateKey)keyStore.getKey(END_ENTITY_ALIAS, PASSWORD.toCharArray());
        Certificate[] chain = keyStore.getCertificateChain(END_ENTITY_ALIAS);
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(privateKey, chain, DNIe_SIGN_MECHANISM);
        return signedMailGenerator.genMimeMessage(userTest.getEmail(),toUser, textToSign, subject);
    }

    public void sendMessageToHost(OperationVS operation) { appHost.sendMessageToHost(operation); }

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
    
    public ResponseVS getHashCertVSData(String hashCertVSBase64) {
        logger.debug("getHashCertVSData");
        if(hashCertVSDataMap == null || hashCertVSBase64 == null) {
            logger.debug("getHashCertVSData - hashCertVSDataMap: " + hashCertVSDataMap +
                    " - hashCertVSBase64: " + hashCertVSBase64);
            return null;
        }
        return hashCertVSDataMap.get(hashCertVSBase64);
    }
    
    public void addHashCertVSData(String hashCertVSBase64, ResponseVS hashCertVSData) {
        if(hashCertVSDataMap == null) hashCertVSDataMap = new HashMap<String, ResponseVS>();
        hashCertVSDataMap.put(hashCertVSBase64, hashCertVSData);
    }

    public void copyFile(byte[] fileToCopy, String subPath, String fileName) throws Exception {
        File newFileDir = new File(APPDIR + subPath);
        newFileDir.mkdirs();
        File newFile = new File(newFileDir.getAbsolutePath() + "/" + fileName);
        logger.debug("newFile.path: " + newFile.getAbsolutePath());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(fileToCopy), newFile);
    }

    public AccessControlVS getAccessControl() { return accessControl; }

    public void setAccessControl(AccessControlVS accessControl) { this.accessControl = accessControl; }

    public PKIXParameters getSessionPKIXParameters() throws InvalidAlgorithmParameterException, Exception {
        logger.debug("getSessionPKIXParameters");
        Set<TrustAnchor> anchors = accessControl.getTrustAnchors();
        TrustAnchor rootCACertSessionAnchor = new TrustAnchor(rootCACert, null);
        anchors.add(rootCACertSessionAnchor);
        PKIXParameters sessionPKIXParams = new PKIXParameters(anchors);
        sessionPKIXParams.setRevocationEnabled(false); // tell system do not check CRL's
        return sessionPKIXParams;
    }

    public ControlCenterVS getControlCenter() { return controlCenter; }

    public void setControlCenter(ControlCenterVS controlCenter) { this.controlCenter = controlCenter; }

    public static String getMessage(String key, Object... arguments) {
        try {
            String pattern = appProperties.getProperty(key);
            if(arguments.length > 0) return MessageFormat.format(pattern, arguments);
            else return pattern;
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