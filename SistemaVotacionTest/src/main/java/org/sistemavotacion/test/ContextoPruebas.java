package org.sistemavotacion.test;

import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import javax.security.auth.x500.X500PrivateCredential;
import org.apache.log4j.PropertyConfigurator;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.test.modelo.UserBaseSimulationData;
import org.sistemavotacion.test.panel.VotacionesPanel;
import org.sistemavotacion.util.NifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public enum ContextoPruebas {
     
    INSTANCE;
    
    private static Logger logger = LoggerFactory.getLogger(ContextoPruebas.class);

    public static class DEFAULTS {
        public static final String APPDIR =  Contexto.DEFAULTS.APPDIR + 
                File.separator + "ContextoPruebas"  + File.separator;  
        private static final String locale =  "es";
        private static final String ROOT_ALIAS = "rootAlias";
        public static final String END_ENTITY_ALIAS = "endEntityAlias";        
        public static final String PASSWORD = "PemPass"; 
        private static final long CERT_VALID_FROM = System.currentTimeMillis();
        private static final long ROOT_KEYSTORE_PERIOD = 20000000000L;
        private static final long USER_KEYSTORE_PERIOD = 20000000000L;  
    } 

    public static String locale = "es";
    public static final String SIG_NAME = "RSA";
    public static final String PROVIDER = "BC";

    private KeyStore rootCAKeyStore;
    private Usuario userTest;
    private X500PrivateCredential rootCAPrivateCredential;
    private PrivateKey rootCAPrivateKey;
    private X509Certificate rootCACert;
    private ActorConIP controlCenter;
    private ActorConIP accessControl;
    
    private Evento evento = null;
    private UserBaseSimulationData userBaseData = null;
    private VotacionesPanel votacionesPanel = null;
    
    public static final String DNIe_SIGN_MECHANISM = "SHA1withRSA";
    public static final String VOTE_SIGN_MECHANISM = "SHA512withRSA";
    public static final String DIGEST_ALG = "SHA256";
    

    public static String ROOT_ALIAS = "rootAlias";     
    public static String PASSWORD = "PemPass"; 

    private ResourceBundle resourceBundle;

    private PKIXParameters sessionPKIXParams = null;

    private ContextoPruebas () {
        try {
            new File(DEFAULTS.APPDIR).mkdirs();
            Properties props = new Properties();
            props.load(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("log4jSistemaVotacionTest.properties")); 
            Contexto.INSTANCE.init();
            resourceBundle = ResourceBundle.getBundle("messagesTest_" + DEFAULTS.locale);
            PropertyConfigurator.configure(props);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
            String dateStr = formatter.format(new Date(DEFAULTS.CERT_VALID_FROM));
    
            String strSubjectDN = getString("rootCASubjectDN", dateStr);
            rootCAKeyStore = KeyStoreUtil.createRootKeyStore(
                     DEFAULTS.CERT_VALID_FROM, DEFAULTS.ROOT_KEYSTORE_PERIOD, 
                     DEFAULTS.PASSWORD.toCharArray(), DEFAULTS.ROOT_ALIAS, 
                     strSubjectDN);
            rootCACert = (X509Certificate)rootCAKeyStore.
                    getCertificate(DEFAULTS.ROOT_ALIAS);
            rootCAPrivateKey = (PrivateKey)rootCAKeyStore.
                    getKey(DEFAULTS.ROOT_ALIAS, DEFAULTS.PASSWORD.toCharArray());
            rootCAPrivateCredential = new X500PrivateCredential(
                     rootCACert, rootCAPrivateKey,  DEFAULTS.ROOT_ALIAS);
            
            userTest = new Usuario();
            userTest.setNif(NifUtils.getNif(1234567));
            userTest.setNombre("Test Publisher User");
            userTest.setEmail("testPublisherUser@votingsystem.com");
            KeyStore userKeySTore = KeyStoreUtil.createUserKeyStore(
                    DEFAULTS.CERT_VALID_FROM, DEFAULTS.USER_KEYSTORE_PERIOD, 
                    DEFAULTS.PASSWORD.toCharArray(),
                    DEFAULTS.END_ENTITY_ALIAS, rootCAPrivateCredential, 
                    "GIVENNAME=NameTestPublisherUser, SURNAME=SurnameTestPublisherUser" + 
                    ", SERIALNUMBER=" + userTest.getNif()); 
            userTest.setKeyStore(userKeySTore);    
            Contexto.INSTANCE.initMultiThreadedHttp();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override public void run() {
                    logger.debug("------- ContextoPruebas Shutdown Hook -------");
                    shutdown();
                }
            });
        } catch (Exception ex) {
            LoggerFactory.getLogger(ContextoPruebas.class).error(ex.getMessage(), ex);
        }

    }
    
    public void init() { }
    
    public void shutdown() {
        logger.debug("shutdown");
        Contexto.INSTANCE.shutdown();
    }
    
    public void setVotingPanel(VotacionesPanel panel) {
        this.votacionesPanel = panel;
    }
    
    public PrivateKey getUserTestPrivateKey() throws Exception {
        PrivateKey key = (PrivateKey)userTest.getKeyStore().getKey(
                DEFAULTS.END_ENTITY_ALIAS, PASSWORD.toCharArray());
        return key;
    }
    
    public Certificate[] getUserTestCertificateChain() throws Exception {
        return userTest.getKeyStore().getCertificateChain(DEFAULTS.END_ENTITY_ALIAS);
    }
    
    /*
     * ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.PASSWORD.toCharArray()
     */
    
    public KeyStore crearMockDNIe(String userNIF) throws Exception {
        //logger.info("crearMockDNIe - userNIF: " + userNIF);
        KeyStore keyStore = KeyStoreUtil.createUserKeyStore(
                DEFAULTS.CERT_VALID_FROM, DEFAULTS.USER_KEYSTORE_PERIOD,
                PASSWORD.toCharArray(), DEFAULTS.END_ENTITY_ALIAS, rootCAPrivateCredential, 
                "GIVENNAME=NombreDe" + userNIF + " ,SURNAME=ApellidoDe" + userNIF 
                + ", SERIALNUMBER=" + userNIF); 
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, PASSWORD.toCharArray());
        Contexto.copyKeyStoreToUserDir(keyStoreBytes, userNIF, 
                ContextoPruebas.DEFAULTS.APPDIR);
        return keyStore;
    }
    
    public String getString(String key) {
        return resourceBundle.getString(key);
    }    
    
    public String getString(String key, Object... arguments) {
        String pattern = getString(key);
        return MessageFormat.format(pattern, arguments);
    }

    /**
     * @return the accessControl
     */
    public ActorConIP getAccessControl() {
        return accessControl;
    }
    
    public String getUrlControlAccesoCertChain() {
        logger.debug(" --- getUrlControlAccesoCertChain --- ");
        return accessControl.getServerURL() + "/certificado/cadenaCertificacion";
    }
    
    private PKIXParameters getSessionPKIXParametersFromWeb() 
            throws InvalidAlgorithmParameterException, Exception {
        logger.debug(" --- getSessionPKIXParameters --- ");
        if(sessionPKIXParams == null) {
            PKIXParameters params = Contexto.INSTANCE.getHttpHelper()
                    .obtenerPKIXParametersDeServidor(getUrlControlAccesoCertChain());
            TrustAnchor anchorUserCert = new TrustAnchor(rootCACert, null);
            Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
            anchors.add(anchorUserCert);
            anchors.addAll(params.getTrustAnchors());
            sessionPKIXParams = new PKIXParameters(anchors);
            sessionPKIXParams.setRevocationEnabled(false);
        }
        return sessionPKIXParams;
    }
    
    public PKIXParameters getSessionPKIXParameters() throws Exception {
            logger.debug(" --- getAccessControlPKIXParameters --- ");
        if(accessControl == null) return null;
        if(sessionPKIXParams == null) {
            TrustAnchor anchorTestSession = new TrustAnchor(rootCACert, null);
            Set<TrustAnchor> anchors = accessControl.getTrustAnchors();
            anchors.add(anchorTestSession);
            sessionPKIXParams = new PKIXParameters(anchors);
            sessionPKIXParams.setRevocationEnabled(false);
        }
        return sessionPKIXParams;
    }
    

    /**
     * @param aActorConIP the accessControl to set
     */
    public void setControlAcceso(ActorConIP aActorConIP) {
        accessControl = aActorConIP;
        Contexto.INSTANCE.setAccessControl(accessControl);
    }
           
    /**
     * @return the controlCenter
     */
    public ActorConIP getControlCenter() {
        return controlCenter;
    }

    /**
     * @param aControlCenter the controlCenter to set
     */
    public void setControlCenter(ActorConIP aControlCenter) {
        this.controlCenter = aControlCenter;
        if(accessControl == null) return;
        accessControl.getCentrosDeControl().add(controlCenter);
    }

    /**
     * @return the rootCAPrivateCredential
     */
    public X500PrivateCredential getPrivateCredentialRaizAutoridad() {
        return rootCAPrivateCredential;
    }

    /**
     * @param aPrivateCredentialRaizAutoridad the rootCAPrivateCredential to set
     */
    public void setPrivateCredentialRaizAutoridad(
            X500PrivateCredential aPrivateCredentialRaizAutoridad) {
        rootCAPrivateCredential = aPrivateCredentialRaizAutoridad;
    }

    /**
     * @return the rootCACert
     */
    public X509Certificate getRootCACert() {
        return rootCACert;
    }

    /**
     * @return the userTest
     */
    public Usuario getUserTest() {
        return userTest;
    }

    /**
     * @return the evento
     */
    public Evento getEvento() {
        return evento;
    }

    /**
     * @param aEvento the evento to set
     */
    public void setEvento(Evento aEvento) {
        evento = aEvento;
        votacionesPanel.cargarEvento(evento);
    }

    public static void crearNivelesDeDirectorios (
            int numDirsPorNivel, String baseDir, int nivel) {
        if (nivel == 0) return;
        for (int i = 0; i < numDirsPorNivel; i++) {
            File file = new File(baseDir + File.separator + i);
            file.mkdirs();
            crearNivelesDeDirectorios(numDirsPorNivel, file.getAbsolutePath(), nivel-1);
        }
    }

 


    public static String getURLInfoServidor(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "infoServidor";
    }
    
    public static String getURLEncryptor(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "encryptor";
    }

    public static String getVotingEventURL(String serverURL, Long eventoId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "eventoVotacion/" +eventoId;
    }
        
    public String getVotingEventURL(Long eventoId) {
        if(accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "eventoVotacion/" +eventoId;
    }

    public static String getRootCAServiceURL(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "certificado/addCertificateAuthority";
    }
    
    public String getAccessControlRootCAServiceURL() {
        if(accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "certificado/addCertificateAuthority";
    }
     
    public String getUserCertServiceURL() {
        if(accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "user";
    }
    

    public String getControlCenterRootCAServiceURL() {
        if(controlCenter == null) return null;
        String serverURL = controlCenter.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "certificado/addCertificateAuthority";
    }
    
    public static String getURLAsociarActorConIP (String serverURL) {
        while(serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0, serverURL.length() - 1);
        }
        return serverURL + "/subscripcion";
    }
    
    public String getURLAsociarActorConIP () {
        if(accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        while(serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0, serverURL.length() - 1);
        }
        return serverURL + "/subscripcion";
    }

    public static String getURLGuardarEventoParaVotar(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "eventoVotacion";
    }
    
    public String getURLGuardarEventoParaVotar() {
        if(accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "eventoVotacion";
    }
    
    public static String getManifestServiceURL(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "eventoFirma";
    }
    
    public static String getClaimServiceURL(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "eventoReclamacion";
    }
       
    public String getClaimServiceURL() {        
        if (accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "eventoReclamacion";
    }
    
    public String getCancelEventURL() {
        if (accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "evento/cancelled";
    }
    
    public static String getSignManifestURL(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "recolectorFirma";
    }

    public static String getURLAnulacionVoto(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "anuladorVoto";
    }
    
    public String getURLAnulacionVoto() {
        if (accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "anuladorVoto";
    }
    public static String getURLAccessRequest(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "solicitudAcceso";
    }
    
    public String getURLAccessRequest() {
        if(accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "solicitudAcceso";
    }

    public static String getURLVoto(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "voto";
    }

    public static String getUrlTimeStampServer(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "timeStamp";
    }
    
    
    public String getUrlRepresentativeService() {
        if (accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "representative";
    }
    
    public String getUrlTimeStampServer() {
        if (accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "timeStamp";
    }

    public static String getUrlSubmitClaims(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "recolectorReclamacion";
    }
    
    public String getUrlSubmitClaims() {
        if (accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "recolectorReclamacion";
    }
    
    public static String getUrlrepresentativeDelegation(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "representative/userSelection";
    }
    
    public String getUrlrepresentativeDelegation() {
        if (accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "representative/userSelection";
    }
    
    public String getUrlBackupEvents() {
        if (accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "solicitudCopia?sync=true";
    }
    
    /**
     * @return the userBaseData
     */
    public UserBaseSimulationData getUserBaseData() {
        return userBaseData;
    }

    /**
     * @param aUserBaseData the userBaseData to set
     */
    public void setUserBaseData(UserBaseSimulationData aUserBaseData) {
        userBaseData = aUserBaseData;
        votacionesPanel.enableSimulation(true);
    }
    
}