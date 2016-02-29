package org.votingsystem.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.model.voting.ControlCenterVS;
import org.votingsystem.signature.util.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.KeyStoreExceptionVS;

import javax.mail.Session;
import java.io.*;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContextVS {

    private static Logger log = Logger.getLogger(ContextVS.class.getName());

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

    private String appDir;
    private String tempDir;

    public static String SETTINGS_FILE_NAME = "settings.properties";
    public static String USER_KEYSTORE_FILE_NAME = "userks.jks";
    public static String WALLET_FILE_NAME = "wallet";
    public static String WALLET_FILE_EXTENSION = ".wvs";
    public static String SERIALIZED_OBJECT_EXTENSION = ".servs";
    public static String PLAIN_WALLET_FILE_NAME = "plain_wallet.wvs";
    public static String BROWSER_SESSION_FILE = "browser.bvs";
    public static String INBOX_FILE = "inbox.mvs";
    public static String REPRESENTATIVE_STATE_FILE = "representative.bvs";
    public static String RECEIPT_FILE_NAME = "receipt";
    public static String CANCEL_DATA_FILE_NAME = "cancellationDataVS";
    public static String CANCEL_BUNDLE_FILE_NAME = "cancellationBundleVS";

    public static final String CSR_FILE_NAME                 = "csr" + ":" + ContentTypeVS.TEXT.getName();
    public static final String IMAGE_FILE_NAME               = "image";
    public static final String REPRESENTATIVE_DATA_FILE_NAME = "representativeData";
    public static final String CURRENCY_REQUEST_DATA_FILE_NAME = "currencyRequestData" + ":" + MediaTypeVS.JSON_SIGNED;
    public static final String ACCESS_REQUEST_FILE_NAME   = "accessRequest" + ":" + MediaTypeVS.JSON_SIGNED;
    public static final String SMIME_FILE_NAME   = "smime" + ":" + MediaTypeVS.JSON_SIGNED;
    public static final String SMIME_ANONYMOUS_FILE_NAME   = "smimeAnonymous" + ":" + MediaTypeVS.JSON_SIGNED;

    public static final String CERT_RAIZ_PATH = "AC_RAIZ_DNIE_SHA1.pem";
    public static final int KEY_SIZE = 1024;
    public static final String SIG_NAME = "RSA";
    /** Random Number Generator algorithm. */
    public static final String ALGORITHM_RNG = "SHA1PRNG";
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

    public static final int MAX_MSG_LENGTH = 300;

    public static final String MULTISIGNED_FILE_NAME = "MultiSign";
    public static final String BASE64_ENCODED_CONTENT_TYPE = "Base64Encoded";
    public static final String KEYSTORE_USER_CERT_ALIAS = "UserTestKeysStore";

    //For tests environments
    public static final String END_ENTITY_ALIAS = "endEntityAlias";
    public static final String PASSWORD = "PemPass";

    private X509Certificate timeStampCACert;
    private Locale locale = new Locale("es");


    private static final Map<String, WebSocketSession> sessionMap = new HashMap<>();

    private Map<String, ResponseVS> hashCertVSDataMap;
    private Collection<X509Certificate> votingSystemSSLCerts;
    private Set<TrustAnchor> votingSystemSSLTrustAnchors;
    private ResourceBundle resBundle;
    private ResourceBundle parentBundle;
    private Properties settings;
    private UserVS userVS;
    private CurrencyServer currencyServer;
    private AccessControlVS accessControl;
    private ControlCenterVS controlCenter;
    private ActorVS defaultServer;
    private DeviceVSDto connectedDevice;
    private static ContextVS INSTANCE;

    public ContextVS(String localizatedMessagesFileName, String localeParam) {
        log.info("localizatedMessagesFileName: " + localizatedMessagesFileName + " - locale: " + locale);
        try {
            KeyGeneratorVS.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    shutdown();
                }
            });
            if(localeParam != null) locale = new Locale(localeParam);
            try {
                parentBundle = ResourceBundle.getBundle("votingSystemAPI", locale);
            } catch (Exception ex) {
                log.info("loading default parent bundle - locale: " + locale);
                parentBundle = ResourceBundle.getBundle("votingSystemAPI");
            }
            if(localizatedMessagesFileName != null) {
                resBundle = ResourceBundle.getBundle(localizatedMessagesFileName, locale);
            }
            INSTANCE = this;
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

    public void initDirs(String baseDir) throws Exception {
        appDir = baseDir + File.separator +  ".VotingSystem";
        tempDir = appDir + File.separator + "temp";
        new File(appDir).mkdir();
        new File(tempDir).mkdir();
        FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(CERT_RAIZ_PATH),  new File(INSTANCE.appDir + "/" + CERT_RAIZ_PATH));
        FileHandler fileHandler = new FileHandler(new File(appDir + "/app.log").getAbsolutePath());
        fileHandler.setFormatter(new SimpleFormatter());
        Logger.getLogger("").addHandler(fileHandler);
    }

    public String getAppDir() {
        return appDir;
    }

    public String getTempDir() {
        return tempDir;
    }

    public Locale getLocale() {
        return locale;
    }

    public static ContextVS getInstance() {
        if(INSTANCE == null) INSTANCE = new ContextVS(null, null);
        return INSTANCE; 
    }

    public void shutdown() {
        try {
            log.info("------------------ shutdown --------------------");
            FileUtils.deleteRecursively(new File(tempDir));
            log.info("------------------------------------------------");
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void initTestEnvironment(InputStream configPropertiesStream, String appDir) throws Exception {
        log.info("initTestEnvironment - appDir: " + appDir);
        initDirs(appDir);
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
                File settingsFile = new File(appDir + File.separator + SETTINGS_FILE_NAME);
                if(!settingsFile.exists()) {
                    settingsFile.getParentFile().mkdirs();
                    settingsFile.createNewFile();
                }
                input = new FileInputStream(settingsFile);
                settings.load(input);
                log.info("loading settings: " + settingsFile.getAbsolutePath());
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

    public ResponseVS<ActorVS> checkServer(String serverURL) throws Exception {
        log.info(" - checkServer: " + serverURL);
        ActorVS actorVS = null;
        if(currencyServer != null && currencyServer.getServerURL().equals(serverURL)) actorVS = currencyServer;
        else if(accessControl != null && accessControl.getServerURL().equals(serverURL)) actorVS = accessControl;
        else if(controlCenter != null && controlCenter.getServerURL().equals(serverURL)) actorVS = controlCenter;
        if (actorVS == null) {
            ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(serverURL), ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                actorVS = ((ActorVSDto) responseVS.getMessage(ActorVSDto.class)).getActorVS();
                responseVS.setData(actorVS);
                log.log(Level.INFO,"checkServer - adding " + serverURL.trim() + " to sever map");
                switch (actorVS.getType()) {
                    case ACCESS_CONTROL:
                        setAccessControl((AccessControlVS) actorVS);
                        break;
                    case CURRENCY:
                        setCurrencyServer((CurrencyServer) actorVS);
                        setTimeStampServerCert(actorVS.getTimeStampCert());
                        break;
                    case CONTROL_CENTER:
                        setControlCenter((ControlCenterVS) actorVS);
                        break;
                    default:
                        log.info("Unprocessed actor:" + actorVS.getType());
                }
            } else if (ResponseVS.SC_NOT_FOUND == responseVS.getStatusCode()) {
                responseVS.setMessage(ContextVS.getMessage("serverNotFoundMsg", serverURL.trim()));
            }
            return responseVS;
        } else {
            ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
            responseVS.setData(actorVS);
            return responseVS;
        }
    }

    public void putWSSession(String UUID, WebSocketSession session) {
        sessionMap.put(UUID, session.setUUID(UUID));
    }

    public WebSocketSession getWSSession(String UUID) {
        return sessionMap.get(UUID);
    }
    public WebSocketSession getWSSession(Long deviceId) {
        List<WebSocketSession> result = sessionMap.entrySet().stream().filter(k ->  k.getValue().getDeviceVS() != null &&
                k.getValue().getDeviceVS().getId() == deviceId).map(k -> k.getValue()).collect(toList());
        return result.isEmpty()? null : result.get(0);
    }

    public void setProperty(String propertyName, String propertyValue) {
        Properties settings = getSettings();
        OutputStream output = null;
        try {
            output = new FileOutputStream(appDir + File.separator + SETTINGS_FILE_NAME);
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

    public UserVS saveUserKeyStore(KeyStore keyStore, char[] password) throws Exception{
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password);
        File mainKeyStoreFile = new File(appDir + File.separator + USER_KEYSTORE_FILE_NAME);
        if(mainKeyStoreFile.exists()) {
            File oldFile = new File(appDir + File.separator + ".old_" + USER_KEYSTORE_FILE_NAME);
            mainKeyStoreFile.renameTo(oldFile);
            mainKeyStoreFile.createNewFile();
        } else mainKeyStoreFile.createNewFile();
        Certificate[] chain = keyStore.getCertificateChain(ContextVS.KEYSTORE_USER_CERT_ALIAS);
        UserVS userVS = UserVS.FROM_X509_CERT((X509Certificate)chain[0]);

        CertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CertExtensionDto.class,
                userVS.getCertificate(), ContextVS.DEVICEVS_OID);
        DeviceVSDto deviceVSDto = new DeviceVSDto(userVS, certExtensionDto);
        deviceVSDto.setDeviceName(userVS.getNif() + " - " + userVS.getName());

        File userVSKeyStoreFile = new File(appDir + File.separator + userVS.getNif() + "_" + USER_KEYSTORE_FILE_NAME);
        userVSKeyStoreFile.createNewFile();
        Map keyStoreMap = new HashMap<>();
        keyStoreMap.put("deviceVSDto", JSON.getMapper().writeValueAsString(deviceVSDto));
        keyStoreMap.put("certPEM", new String(CertUtils.getPEMEncoded(chain[0])));
        keyStoreMap.put("keyStore", Base64.getEncoder().encodeToString(keyStoreBytes));
        JSON.getMapper().writeValue(mainKeyStoreFile, keyStoreMap);
        JSON.getMapper().writeValue(userVSKeyStoreFile, keyStoreMap);
        return userVS;
    }

    public DeviceVSDto getKeyStoreDevice() {
        DeviceVSDto result = null;
        try {
            File keyStoreFile = new File(appDir + File.separator + USER_KEYSTORE_FILE_NAME);
            if(keyStoreFile.createNewFile()) return null;
            Map keyStoreMap = JSON.getMapper().readValue(keyStoreFile, new TypeReference<Map<String, String>>() { });
            result = JSON.getMapper().readValue((String) keyStoreMap.get("deviceVSDto"), DeviceVSDto.class);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            return result;
        }
    }

    public UserVS getKeyStoreUserVS() {
        try {
            File keyStoreFile = new File(appDir + File.separator + USER_KEYSTORE_FILE_NAME);
            if(keyStoreFile.createNewFile()) return null;
            Map keyStoreMap = JSON.getMapper().readValue(keyStoreFile, new TypeReference<Map<String, String>>() { });
            Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(
                    ((String) keyStoreMap.get("certPEM")).getBytes());
            return UserVS.FROM_X509_CERT(certChain.iterator().next());
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    public KeyStore getUserKeyStore(char[] password) throws KeyStoreExceptionVS {
        File keyStoreFile = null;
        KeyStore keyStore = null;
        try {
            keyStoreFile = new File(appDir + File.separator + USER_KEYSTORE_FILE_NAME);
            if(!keyStoreFile.exists()) throw new KeyStoreExceptionVS(getMessage("cryptoTokenNotFoundErrorMsg"));
        } catch(Exception ex) {
            throw new KeyStoreExceptionVS(getMessage("cryptoTokenNotFoundErrorMsg"), ex);
        }
        try {
            Map keyStoreMap = JSON.getMapper().readValue(keyStoreFile, new TypeReference<Map<String, String>>() {});
            byte[] keyStoreBytes = Base64.getDecoder().decode((String) keyStoreMap.get("keyStore"));
            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new ByteArrayInputStream(keyStoreBytes), password);
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
            keyStoreFile = new File(appDir + File.separator + nif + "_" + USER_KEYSTORE_FILE_NAME);
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
        if(getDefaultServer() != null) {
            return getDefaultServer().getTimeStampCert();
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
        File newFileDir = new File(appDir + subPath);
        newFileDir.mkdirs();
        File newFile = new File(newFileDir.getAbsolutePath() + "/" + fileName);
        log.info("copyFile - path: " + newFile.getAbsolutePath());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(fileToCopy), newFile);
    }

    public AccessControlVS getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(AccessControlVS accessControl) {
        this.accessControl = accessControl;
        this.controlCenter = accessControl.getControlCenter();
        if(this.defaultServer == null) this.defaultServer = accessControl;
    }

    public ControlCenterVS getControlCenter() { return controlCenter; }

    public void setControlCenter(ControlCenterVS controlCenter) { this.controlCenter = controlCenter; }

    public static String getMessage(String key, Object... arguments) {
        String pattern = null;
        try {
            if(getInstance().resBundle != null) {
                pattern = getInstance().resBundle.getString(key);
            }
        } catch(Exception ex) {
        } finally {
            try {
                if(pattern == null) pattern = getInstance().parentBundle.getString(key);
                pattern = new String(pattern.getBytes(ISO_8859_1), UTF_8);
                if(arguments.length > 0) return MessageFormat.format(pattern, arguments);
                else return pattern;
            } catch (Exception ex) {
                log.log(Level.SEVERE, "### Value not found for key: " + key);
                return "---" + key + "---";
            }

        }
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

    public DeviceVSDto getConnectedDevice() {
        return connectedDevice;
    }

    public void setConnectedDevice(DeviceVSDto connectedDevice) {
        this.connectedDevice = connectedDevice;
    }
}