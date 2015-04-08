package org.votingsystem.util;

import iaik.pkcs.pkcs11.Mechanism;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.votingsystem.model.*;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.KeyGeneratorVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.throwable.ExceptionVS;

import javax.mail.Session;
import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContextVS implements BundleActivator {

    private static Logger log = Logger.getLogger(ContextVS.class.getSimpleName());

    public static Session MAIL_SESSION = Session.getDefaultInstance(System.getProperties(), null);

    static { Security.addProvider(new BouncyCastleProvider()); }

    public static final int VOTE_TAG                                = 0;
    public static final int REPRESENTATIVE_VOTE_TAG                 = 1;
    public static final int ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG = 2;
    public static final int CURRENCY_TAG                               = 3;
    public static final int DEVICEVS_TAG                            = 4;

    public static final String VOTING_SYSTEM_BASE_OID = "0.0.0.0.0.0.0.0.0.";
    public static final String REPRESENTATIVE_VOTE_OID = VOTING_SYSTEM_BASE_OID + REPRESENTATIVE_VOTE_TAG;
    public static final String ANONYMOUS_REPRESENTATIVE_DELEGATION_OID = VOTING_SYSTEM_BASE_OID +
            ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG;
    public static final String VOTE_OID = VOTING_SYSTEM_BASE_OID + VOTE_TAG;
    public static final String CURRENCY_OID = VOTING_SYSTEM_BASE_OID + CURRENCY_TAG;
    public static final String DEVICEVS_OID = VOTING_SYSTEM_BASE_OID + DEVICEVS_TAG;

    public static final Mechanism DNIe_SESSION_MECHANISM = Mechanism.SHA1_RSA_PKCS;

    public static String APPDIR;
    public static String WEBVIEWDIR;
    public static String APPTEMPDIR;
    public static String ERROR_DIR;

    public static String SETTINGS_FILE_NAME = "settings.properties";
    public static String USER_KEYSTORE_FILE_NAME = "userks.jks";
    public static String WALLET_FILE_NAME = "wallet";
    public static String WALLET_FILE_EXTENSION = ".wvs";
    public static String SERIALIZED_OBJECT_EXTENSION = ".servs";
    public static String PLAIN_WALLET_FILE_NAME = "plain_wallet.wvs";
    public static String BROWSER_SESSION_FILE = "browser.bvs";
    public static String INBOX_FILE = "inbox.mvs";
    public static String REPRESENTATIVE_STATE_FILE = "representative.bvs";
    public static String USER_CSR_REQUEST_FILE_NAME = "user.csrvs";
    public static String RECEIPT_FILE_NAME = "receipt";
    public static String CANCEL_DATA_FILE_NAME = "cancellationDataVS";
    public static String CANCEL_BUNDLE_FILE_NAME = "cancellationBundleVS";

    public static final String CSR_FILE_NAME                 = "csr" + ":" + ContentTypeVS.TEXT.getName();
    public static final String IMAGE_FILE_NAME               = "image";
    public static final String REPRESENTATIVE_DATA_FILE_NAME = "representativeData";
    public static final String CURRENCY_REQUEST_DATA_FILE_NAME = "currencyRequestData" + ":" + ContentTypeVS.JSON_SIGNED.getName();
    public static final String ACCESS_REQUEST_FILE_NAME   = "accessRequest" + ":" + ContentTypeVS.JSON_SIGNED.getName();

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

    public static final int IMAGE_MAX_FILE_SIZE_KB = 1024;
    public static final int IMAGE_MAX_FILE_SIZE = IMAGE_MAX_FILE_SIZE_KB * 1024;
    public static final int SIGNED_MAX_FILE_SIZE_KB = 512;
    public static final int SIGNED_MAX_FILE_SIZE = SIGNED_MAX_FILE_SIZE_KB * 1024;

    public static final String MULTISIGNED_FILE_NAME = "MultiSign";
    public static final String HASH_CERTVS_KEY        = "hashCertVSBase64";
    public static final String ORIGIN_HASH_CERTVS_KEY = "originHashCertVS";
    public static final String CRYPTO_TOKEN = "CRYPTO_TOKEN";
    public static final String USER_NIF = "USER_NIF";
    public static final String BASE64_ENCODED_CONTENT_TYPE = "Base64Encoded";
    public static final String KEYSTORE_USER_CERT_ALIAS = "UserTestKeysStore";

    //For tests environments
    public static final String END_ENTITY_ALIAS = "endEntityAlias";
    public static final String PASSWORD = "PemPass";

    private X509Certificate timeStampCACert;
    private Locale locale = new Locale("es");

    private Map<String, ResponseVS> hashCertVSDataMap;
    private Collection<X509Certificate> votingSystemSSLCerts;
    private Set<TrustAnchor> votingSystemSSLTrustAnchors;
    private Properties appProperties;
    private Properties settings;
    private UserVS userVS;
    private CurrencyServer currencyServer;
    private AccessControlVS accessControl;
    private ControlCenterVS controlCenter;
    private ActorVS defaultServer;
    static ContextVS INSTANCE;

    public ContextVS() {
        try {
            initDirs(System.getProperty("user.home"));
            KeyGeneratorVS.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    public ContextVS(String localizatedMessagesFileName, String locale) {
        log.info("localizatedMessagesFileName: " + localizatedMessagesFileName + " - locale: " + locale);
        try {
            initDirs(System.getProperty("user.home"));
            appProperties = new Properties();
            String messagesFileName = null;
            if(locale == null) messagesFileName = "votingSystemAPI" + ".properties";
            else messagesFileName = "votingSystemAPI" + "_" + locale +  ".properties";
            URL res = Thread.currentThread().getContextClassLoader().getResource(messagesFileName);
            if (res == null) {
                messagesFileName = "votingSystemAPI.properties";;
                res = Thread.currentThread().getContextClassLoader().getResource(messagesFileName);
            }
            appProperties.load(res.openStream());
            if(localizatedMessagesFileName != null) {
                String providedProperties = localizatedMessagesFileName.split("\\.")[0] + "_" + locale + ".properties";
                log.info("provided localizatedMessagesFileName: " + providedProperties);
                appProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        providedProperties));
            }
            KeyGeneratorVS.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            Runtime.getRuntime().addShutdownHook(new Thread() { public void run() { shutdown(); } });
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage() + " - localizatedMessagesFileName: " +
                    localizatedMessagesFileName, ex);
        }
    }

    public Collection<X509Certificate> getVotingSystemSSLCerts() throws Exception {
        if(votingSystemSSLCerts == null) {
            votingSystemSSLCerts =  CertUtils.fromPEMToX509CertCollection(
                    FileUtils.getBytesFromStream(Thread.currentThread().
                            getContextClassLoader().getResourceAsStream("VotingSystemSSLCert.pem")));
            votingSystemSSLTrustAnchors = new HashSet<TrustAnchor>();
            for(X509Certificate certificate: votingSystemSSLCerts) {
                TrustAnchor anchor = new TrustAnchor(certificate, null);
                votingSystemSSLTrustAnchors.add(anchor);
            }
        }
        return votingSystemSSLCerts;
    }

    public Set<TrustAnchor> getVotingSystemSSLTrustAnchors() throws Exception {
        if (votingSystemSSLTrustAnchors == null) getVotingSystemSSLCerts();
        return votingSystemSSLTrustAnchors;
    }

    public void initDirs(String baseDir) {
        APPDIR =  baseDir + File.separator +  ".VotingSystem";
        WEBVIEWDIR =  APPDIR + File.separator + "webview";
        APPTEMPDIR =  APPDIR + File.separator + "temp";
        ERROR_DIR =  APPDIR + File.separator + "error";
        new File(APPDIR).mkdir();
        new File(APPTEMPDIR).mkdir();
        new File(ERROR_DIR).mkdirs();
    }

    public static void initSignatureClient (String localizatedMessagesFileName, String locale){
        try {
            log.info("------------- initSignatureClient ----------------- ");
            INSTANCE = new ContextVS(localizatedMessagesFileName, locale);
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(CERT_RAIZ_PATH),  new File(APPDIR + CERT_RAIZ_PATH));
            OSValidator.initClassPath();
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ContextVS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Locale getLocale() {
        return locale;
    }


    public static void init() {
        getInstance();
    }

    public static ContextVS getInstance() {
        if(INSTANCE == null)  INSTANCE = new ContextVS();
        return INSTANCE; 
    }
    
    public void shutdown() {
        try {
            log.info("------------------ shutdown --------------------");
            FileUtils.deleteRecursively(new File(APPTEMPDIR));
            log.info("------------------------------------------------");
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void initTestEnvironment(InputStream configPropertiesStream, String appDir) throws Exception {
        log.info("initTestEnvironment - appDir: " + appDir);
        if(appDir != null) initDirs(appDir);
        try {
            settings = getSettings();
            settings.load(configPropertiesStream);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public byte[] getResourceBytes(String name) throws IOException {
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        return FileUtils.getBytesFromStream(input);
    }

    public void setSessionUser(UserVS userVS) {
        log.info("setSessionUser - nif: " + userVS.getNif());
        this.userVS = userVS;
    }

    public UserVS getSessionUser() { return userVS; }

    public Properties getSettings() {
        FileInputStream input = null;
        try {
            if(settings == null) {
                settings = new Properties();
                File settingsFile = new File(APPDIR + File.separator + SETTINGS_FILE_NAME);
                if(!settingsFile.exists()) settingsFile.createNewFile();
                input = new FileInputStream(settingsFile);
                settings.load(input);
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            return settings;
        }
    }

    public String getProperty(String propertyName) {
        return getProperty(propertyName, null);
    }

    public String getProperty(String propertyName, String defaultValue) {
        String result = null;
        try {
            result = getSettings().getProperty(propertyName);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (result == null) return defaultValue;
            else return result;
        }
    }

    public Boolean getBoolProperty(String propertyName, Boolean defaultValue) {
        Boolean result = null;
        String propertyStr = getSettings().getProperty(propertyName);
        try {
            if(propertyStr != null) {
                result = Boolean.valueOf(propertyStr);
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (result == null) return defaultValue;
            else return result;
        }
    }

    public void setProperty(String propertyName, String propertyValue) {
        Properties settings = getSettings();
        OutputStream output = null;
        try {
            output = new FileOutputStream(APPDIR + File.separator + SETTINGS_FILE_NAME);
            settings.setProperty(propertyName, propertyValue);
            settings.store(output, null);
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
    }

    public static UserVS saveUserKeyStore(KeyStore keyStore, String password) throws Exception{
        byte[] resultBytes = KeyStoreUtil.getBytes(keyStore, password.toCharArray());
        File mainKeyStoreFile = new File(APPDIR + File.separator + USER_KEYSTORE_FILE_NAME);
        mainKeyStoreFile.createNewFile();
        Certificate[] chain = keyStore.getCertificateChain(ContextVS.KEYSTORE_USER_CERT_ALIAS);
        UserVS userVS = UserVS.getUserVS((X509Certificate)chain[0]);
        File userVSKeyStoreFile = new File(APPDIR + File.separator + userVS.getNif() + "_" + USER_KEYSTORE_FILE_NAME);
        userVSKeyStoreFile.createNewFile();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(resultBytes), userVSKeyStoreFile);
        FileUtils.copyStreamToFile(new ByteArrayInputStream(resultBytes), mainKeyStoreFile);
        return userVS;
    }

    public KeyStore getUserKeyStore(char[] password) throws KeyStoreExceptionVS{
        File keyStoreFile = null;
        KeyStore keyStore = null;
        try {
            keyStoreFile = new File(APPDIR + File.separator + USER_KEYSTORE_FILE_NAME);
        } catch(Exception ex) {
            throw new KeyStoreExceptionVS(getMessage("cryptoTokenNotFoundErrorMsg"), ex);
        }
        try {
            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(keyStoreFile), password);
        } catch(Exception ex) {
            throw new KeyStoreExceptionVS(getMessage("cryptoTokenPasswdErrorMsg"), ex);
        }
        return keyStore;
    }

    public KeyStore getUserKeyStore(String nif, String password) throws KeyStoreExceptionVS{
        if(nif == null) return getUserKeyStore(password.toCharArray());
        File keyStoreFile = null;
        KeyStore keyStore = null;
        try {
            keyStoreFile = new File(APPDIR + File.separator + nif + "_" + USER_KEYSTORE_FILE_NAME);
        } catch(Exception ex) {
            throw new KeyStoreExceptionVS(getMessage("cryptoTokenNotFoundErrorMsg"), ex);
        }
        try {
            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
        } catch(Exception ex) {
            throw new KeyStoreExceptionVS(getMessage("cryptoTokenPasswdErrorMsg"), ex);
        }
        return keyStore;
    }

    public X509Certificate getTimeStampServerCert() throws ExceptionVS {
        if(timeStampCACert != null) return timeStampCACert;
        if(defaultServer != null) {
            return defaultServer.getTimeStampCert();
        } else throw new ExceptionVS("TimeStampServerCert not initialized");
    }

    public void setTimeStampServerCert(X509Certificate timeStampCACert) {
        this.timeStampCACert = timeStampCACert;
    }
    
    public ResponseVS getHashCertVSData(String hashCertVSBase64) {
        log.info("getHashCertVSData");
        if(hashCertVSDataMap == null || hashCertVSBase64 == null) {
            log.info("getHashCertVSData - hashCertVSDataMap: " + hashCertVSDataMap +
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
        log.info("newFile.path: " + newFile.getAbsolutePath());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(fileToCopy), newFile);
    }

    public AccessControlVS getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(AccessControlVS accessControl) {
        this.accessControl = accessControl;
        this.controlCenter = accessControl.getControlCenters().iterator().next();
        if(this.defaultServer == null) this.defaultServer = accessControl;
    }

    public ControlCenterVS getControlCenter() { return controlCenter; }

    public void setControlCenter(ControlCenterVS controlCenter) { this.controlCenter = controlCenter; }

    public static String getMessage(String key, Object... arguments) {
        try {
            String pattern = getInstance().appProperties.getProperty(key);
            if(arguments.length > 0) return new String(MessageFormat.format(pattern, arguments).getBytes(ISO_8859_1), UTF_8);
            else return new String(pattern.getBytes(ISO_8859_1), UTF_8);
        } catch(Exception ex) {
            log.log(Level.SEVERE, "### Value not found for key: " + key);
            return "---" + key + "---";
        }
    }

    public static Icon getIcon(Object baseObject, String key) {
        String iconPath = null;
        String iconName = null;
        Icon icon = null;
        if(key.endsWith("_16")) {
            iconName = key.substring(0, key.indexOf("_16"));
            iconPath = "/resources/icon_16/" + iconName + ".png";
        } else if(key.endsWith("_32")) {
            iconName = key.substring(0, key.indexOf("_32"));
            iconPath = "/resources/icon_32/" + iconName + ".png";
        } else {//defaults to 16x16 icons
             iconPath = "/resources/icon_16/" + key + ".png";
        }
        try {
            icon = new ImageIcon(baseObject.getClass().getResource(iconPath));
        } catch(Exception ex) {
            log.log(Level.SEVERE, " ### iconPath: " + iconPath + " not found");
            icon = new ImageIcon(baseObject.getClass().getResource("/resources/icon_32/button_default.png"));
        }
        return icon;
    }


    public CurrencyServer getCurrencyServer() {
        return currencyServer;
    }

    public void setCurrencyServer(CurrencyServer currencyServer) {
        this.currencyServer = currencyServer;
    }

    public void setServer(ActorVS server) {
        if(server instanceof CurrencyServer) setCurrencyServer((CurrencyServer) server);
        else if(server instanceof AccessControlVS) setAccessControl((AccessControlVS) server);
        else if(server instanceof ControlCenterVS) setControlCenter((ControlCenterVS) server);
        else log.log(Level.SEVERE, "setServer - unknown server type: " + server.getType() + " - class: " + server.getClass().getSimpleName());
    }

    public ActorVS checkServer(String serverURL) {
        if(currencyServer != null && currencyServer.getServerURL().equals(serverURL)) return currencyServer;
        if(accessControl != null && accessControl.getServerURL().equals(serverURL)) return accessControl;
        if(controlCenter != null && controlCenter.getServerURL().equals(serverURL)) return controlCenter;
        log.info("checkServer - serverURL: '" + serverURL + "' not found");
        return null;
    }

    public void setDefaultServer(ActorVS server) {
        log.info("setDefaultServer - serverURL: " + server.getServerURL());
        this.defaultServer = server;
    }

    public ActorVS getDefaultServer() throws ExceptionVS {
        if(defaultServer != null) return defaultServer;
        if(accessControl != null) return accessControl;
        if(currencyServer != null) return currencyServer;
        throw new ExceptionVS("Missing default server");
    }

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("System.out ----> Activator start - org.votingsystem.util.ContextVS");
        log.info("Log4j ----> Activator - org.votingsystem.util.ContextVS");
        INSTANCE = this;
        try {
            initDirs(System.getProperty("user.home"));
            appProperties = new Properties();
            String providedProperties =  "votingSystemAPI" + "_" + locale +  ".properties";
            URL res = context.getBundle().getEntry(providedProperties);
            if (res == null) {
                providedProperties = "votingSystemAPI.properties";;
                res = context.getBundle().getEntry(providedProperties);
            }
            appProperties.load(res.openStream());
            res = context.getBundle().getEntry("VotingSystemSSLCert.pem");
            votingSystemSSLCerts =  CertUtils.fromPEMToX509CertCollection(
                    FileUtils.getBytesFromStream(res.openStream()));
            votingSystemSSLTrustAnchors = new HashSet<TrustAnchor>();
            for(X509Certificate certificate: votingSystemSSLCerts) {
                TrustAnchor anchor = new TrustAnchor(certificate, null);
                votingSystemSSLTrustAnchors.add(anchor);
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("System.out ----> Activator stop - org.votingsystem.util.ContextVS");
    }
}