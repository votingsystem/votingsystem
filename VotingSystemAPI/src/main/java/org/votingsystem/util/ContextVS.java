package org.votingsystem.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.model.Actor;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.voting.AccessControl;
import org.votingsystem.model.voting.ControlCenter;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.KeyStoreExceptionVS;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.KeyGeneratorVS;
import org.votingsystem.util.crypto.KeyStoreUtil;
import org.votingsystem.util.crypto.PEMUtils;

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

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;

    public static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";

    public static final String VOTING_SYSTEM_BASE_OID                  = "0.0.0.0.0.0.0.0.0.";
    public static final String VOTE_OID                                = VOTING_SYSTEM_BASE_OID + 0;
    public static final String REPRESENTATIVE_VOTE_OID                 = VOTING_SYSTEM_BASE_OID + 1;
    public static final String ANONYMOUS_REPRESENTATIVE_DELEGATION_OID = VOTING_SYSTEM_BASE_OID + 2;
    public static final String CURRENCY_OID                            = VOTING_SYSTEM_BASE_OID + 3;
    public static final String DEVICE_OID                              = VOTING_SYSTEM_BASE_OID + 4;
    public static final String ANONYMOUS_CERT_OID                      = VOTING_SYSTEM_BASE_OID + 5;

    public static final int KEY_SIZE = 2048;
    public static final String SIG_NAME = "RSA";
    public static final String ALGORITHM_RNG = "SHA1PRNG";
    public static final String CERT_GENERATION_SIG_ALGORITHM = "SHA256WithRSAEncryption";
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final String DATA_DIGEST_ALGORITHM = "SHA256";
    //For tests environments
    public static final String END_ENTITY_ALIAS = "endEntityAlias";
    public static final String KEYSTORE_USER_CERT_ALIAS = "UserTestKeysStore";
    public static final String PASSWORD = "PemPass";


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
    public static final String CMS_FILE_NAME   = "cms" + ":" + MediaTypeVS.JSON_SIGNED;
    public static final String CMS_ANONYMOUS_FILE_NAME   = "cmsAnonymous" + ":" + MediaTypeVS.JSON_SIGNED;


    public static final int IMAGE_MAX_FILE_SIZE_KB = 1024;
    public static final int IMAGE_MAX_FILE_SIZE = IMAGE_MAX_FILE_SIZE_KB * 1024;
    public static final int SIGNED_MAX_FILE_SIZE_KB = 512;
    public static final int SIGNED_MAX_FILE_SIZE = SIGNED_MAX_FILE_SIZE_KB * 1024;

    public static final int MAX_MSG_LENGTH = 300;

    private X509Certificate timeStampCACert;
    private Locale locale = new Locale("es");

    private static final Map<String, WebSocketSession> sessionMap = new HashMap<>();

    private Map<String, ResponseVS> hashCertVSDataMap;
    private Collection<X509Certificate> votingSystemSSLCerts;
    private Set<TrustAnchor> votingSystemSSLTrustAnchors;
    private ResourceBundle resBundle;
    private ResourceBundle parentBundle;
    private Properties settings;
    private User user;
    private CurrencyServer currencyServer;
    private AccessControl accessControl;
    private ControlCenter controlCenter;
    private Actor defaultServer;
    private DeviceDto connectedDevice;
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
            votingSystemSSLCerts =  PEMUtils.fromPEMToX509CertCollection(
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
        new File(appDir).mkdirs();
        new File(tempDir).mkdirs();
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

    public void setSessionUser(User user) {
        log.info("setSessionUser - nif: " + user.getNif());
        this.user = user;
    }

    public User getSessionUser() { return user; }

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

    public ResponseVS<Actor> checkServer(String serverURL) throws Exception {
        log.info(" - checkServer: " + serverURL);
        Actor actor = null;
        if(currencyServer != null && currencyServer.getServerURL().equals(serverURL)) actor = currencyServer;
        else if(accessControl != null && accessControl.getServerURL().equals(serverURL)) actor = accessControl;
        else if(controlCenter != null && controlCenter.getServerURL().equals(serverURL)) actor = controlCenter;
        if (actor == null) {
            ResponseVS responseVS = HttpHelper.getInstance().getData(Actor.getServerInfoURL(serverURL), ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                actor = ((ActorDto) responseVS.getMessage(ActorDto.class)).getActor();
                responseVS.setData(actor);
                log.log(Level.INFO,"checkServer - adding " + serverURL.trim() + " to sever map");
                switch (actor.getType()) {
                    case ACCESS_CONTROL:
                        setAccessControl((AccessControl) actor);
                        break;
                    case CURRENCY:
                        setCurrencyServer((CurrencyServer) actor);
                        setTimeStampServerCert(actor.getTimeStampCert());
                        break;
                    case CONTROL_CENTER:
                        setControlCenter((ControlCenter) actor);
                        break;
                    default:
                        log.info("Unprocessed actor:" + actor.getType());
                }
            } else if (ResponseVS.SC_NOT_FOUND == responseVS.getStatusCode()) {
                responseVS.setMessage(ContextVS.getMessage("serverNotFoundMsg", serverURL.trim()));
            }
            return responseVS;
        } else {
            ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
            responseVS.setData(actor);
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
        List<WebSocketSession> result = sessionMap.entrySet().stream().filter(k ->  k.getValue().getDevice() != null &&
                k.getValue().getDevice().getId() == deviceId).map(k -> k.getValue()).collect(toList());
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

    public User saveUserKeyStore(KeyStore keyStore, char[] password) throws Exception{
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password);
        File mainKeyStoreFile = new File(appDir + File.separator + USER_KEYSTORE_FILE_NAME);
        if(mainKeyStoreFile.exists()) {
            File oldFile = new File(appDir + File.separator + ".old_" + USER_KEYSTORE_FILE_NAME);
            mainKeyStoreFile.renameTo(oldFile);
            mainKeyStoreFile.createNewFile();
        } else mainKeyStoreFile.createNewFile();
        Certificate[] chain = keyStore.getCertificateChain(ContextVS.KEYSTORE_USER_CERT_ALIAS);
        User user = User.FROM_X509_CERT((X509Certificate)chain[0]);

        CertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CertExtensionDto.class,
                user.getCertificate(), ContextVS.DEVICE_OID);
        DeviceDto deviceDto = new DeviceDto(user, certExtensionDto);
        deviceDto.setDeviceName(user.getNif() + " - " + user.getName());

        File userKeyStoreFile = new File(appDir + File.separator + user.getNif() + "_" + USER_KEYSTORE_FILE_NAME);
        userKeyStoreFile.createNewFile();
        Map keyStoreMap = new HashMap<>();
        keyStoreMap.put("deviceDto", JSON.getMapper().writeValueAsString(deviceDto));
        keyStoreMap.put("certPEM", new String(PEMUtils.getPEMEncoded(chain[0])));
        keyStoreMap.put("keyStore", Base64.getEncoder().encodeToString(keyStoreBytes));
        JSON.getMapper().writeValue(mainKeyStoreFile, keyStoreMap);
        JSON.getMapper().writeValue(userKeyStoreFile, keyStoreMap);
        return user;
    }

    public DeviceDto getKeyStoreDevice() {
        DeviceDto result = null;
        try {
            File keyStoreFile = new File(appDir + File.separator + USER_KEYSTORE_FILE_NAME);
            if(keyStoreFile.createNewFile()) return null;
            Map keyStoreMap = JSON.getMapper().readValue(keyStoreFile, new TypeReference<Map<String, String>>() { });
            result = JSON.getMapper().readValue((String) keyStoreMap.get("deviceDto"), DeviceDto.class);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            return result;
        }
    }

    public User getKeyStoreUser() {
        try {
            File keyStoreFile = new File(appDir + File.separator + USER_KEYSTORE_FILE_NAME);
            if(keyStoreFile.createNewFile()) return null;
            Map keyStoreMap = JSON.getMapper().readValue(keyStoreFile, new TypeReference<Map<String, String>>() { });
            Collection<X509Certificate> certChain = PEMUtils.fromPEMToX509CertCollection(
                    ((String) keyStoreMap.get("certPEM")).getBytes());
            return User.FROM_X509_CERT(certChain.iterator().next());
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

    public AccessControl getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(AccessControl accessControl) {
        this.accessControl = accessControl;
        this.controlCenter = accessControl.getControlCenter();
        if(this.defaultServer == null) this.defaultServer = accessControl;
    }

    public ControlCenter getControlCenter() { return controlCenter; }

    public void setControlCenter(ControlCenter controlCenter) { this.controlCenter = controlCenter; }

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

    public void setServer(Actor server) {
        if(server instanceof CurrencyServer) setCurrencyServer((CurrencyServer) server);
        else if(server instanceof AccessControl) setAccessControl((AccessControl) server);
        else if(server instanceof ControlCenter) setControlCenter((ControlCenter) server);
        else log.log(Level.SEVERE, "setServer - unknown server type: " + server.getType() + " - class: " + server.getClass().getSimpleName());
    }

    public void setDefaultServer(Actor server) {
        log.info("setDefaultServer - serverURL: " + server.getServerURL());
        this.defaultServer = server;
    }

    public Actor getDefaultServer() throws ExceptionVS {
        if(defaultServer != null) return defaultServer;
        if(accessControl != null) return accessControl;
        if(currencyServer != null) return currencyServer;
        throw new ExceptionVS("Missing default server");
    }

    public DeviceDto getConnectedDevice() {
        return connectedDevice;
    }

    public void setConnectedDevice(DeviceDto connectedDevice) {
        this.connectedDevice = connectedDevice;
    }
}