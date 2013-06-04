package org.sistemavotacion.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.PrivateKey;
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
import org.sistemavotacion.util.FileUtils;
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
        private static final String APPDIR =  FileUtils.BASEDIR + File.separator 
            + "DatosSimulacionVotacion"  + File.separator;  
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
    private ActorConIP centroControl;
    private ActorConIP accessControl;
    
    private Evento evento = null;
    private UserBaseSimulationData userBaseData = null;
    
    public static final String DNIe_SIGN_MECHANISM = "SHA1withRSA";
    public static final String VOTE_SIGN_MECHANISM = "SHA512withRSA";
    public static final String DIGEST_ALG = "SHA256";
    
    public static final String PREFIJO_USER_JKS = "usuario_"; 
    public static final String SUFIJO_USER_JKS = ".jks"; 
    public static String ROOT_ALIAS = "rootAlias"; 
    
    public static String PASSWORD = "PemPass"; 
    
    public static String SOLICITUD_FILE = "SolicitudAcceso_";
    public static String DATOS_VOTO = "DatosVoto_";
    public static String ANULACION_FILE = "Anulador_";
    public static String ANULACION_FIRMADA_FILE = "AnuladorFirmado_";
    public static String RECIBO_FILE = "ReciboVoto_";
    public static String VOTE_FILE = "Vote_";
    
    public static final long PERIODO_VALIDEZ_ALMACEN_RAIZ = 20000000000L;//En producción durará lo que dure una votación
    

    public static final int MAXIMALONGITUDCAMPO = 255;
    
    public static String BASEDIR =  System.getProperty("user.home");
    public static String APPDIR =  FileUtils.BASEDIR + File.separator 
            + "DatosSimulacionVotacion"  + File.separator;    

    private ResourceBundle resourceBundle;

    private PKIXParameters sessionPKIXParams = null;

    private ContextoPruebas () {
        try {
            new File(DEFAULTS.APPDIR).mkdirs();
            Properties props = new Properties();
            props.load(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("log4jSistemaVotacionTest.properties"));

            Contexto.INSTANCE.init();
            PropertyConfigurator.configure(props);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
            String dateStr = formatter.format(new Date(DEFAULTS.CERT_VALID_FROM));
    
            rootCAKeyStore = KeyStoreUtil.createRootKeyStore(
                     DEFAULTS.CERT_VALID_FROM, DEFAULTS.ROOT_KEYSTORE_PERIOD, 
                     DEFAULTS.PASSWORD.toCharArray(), DEFAULTS.ROOT_ALIAS, 
                     "CN=Autoridad Certificadora Pruebas - " + dateStr +
                     ", OU=DNIE_CA");
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
            
            resourceBundle = ResourceBundle.getBundle("messagesTest_" + DEFAULTS.locale);            
        } catch (Exception ex) {
            LoggerFactory.getLogger(ContextoPruebas.class).error(ex.getMessage(), ex);
        }

    }
        
    public KeyStore crearMockDNIe(String userNIF) throws Exception {
        File file = new File(ContextoPruebas.getUserKeyStorePath(userNIF));
        //logger.info("crearMockDNIe - userNIF: " + userNIF + " mockDnie:" + 
        //        file.getAbsolutePath());
        KeyStore keyStore = KeyStoreUtil.createUserKeyStore(
                DEFAULTS.CERT_VALID_FROM, DEFAULTS.USER_KEYSTORE_PERIOD,
                PASSWORD.toCharArray(), DEFAULTS.END_ENTITY_ALIAS, rootCAPrivateCredential, 
                "GIVENNAME=NombreDe" + userNIF + " ,SURNAME=ApellidoDe" + userNIF 
                + ", SERIALNUMBER=" + userNIF); 
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, PASSWORD.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
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
     * @return the centroControl
     */
    public ActorConIP getCentroControl() {
        return centroControl;
    }

    /**
     * @param aCentroControl the centroControl to set
     */
    public void setCentroControl(ActorConIP aCentroControl) {
        this.centroControl = aCentroControl;
        if(accessControl == null) return;
        accessControl.getCentrosDeControl().add(centroControl);
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

    public static String getUserDirPath (String userNIF) {
        String resultPath = APPDIR;
        int subPathLength = 3;
        while (userNIF.length() > 0) {
            if(userNIF.length() <= subPathLength) subPathLength = userNIF.length();
            String subPath = userNIF.substring(0, subPathLength);
            userNIF = userNIF.substring(subPathLength);
            resultPath = resultPath + subPath + File.separator;
        }
        return resultPath;
    }
        
    public static String getUserKeyStorePath (String userNIF) {
        String userDirPath = getUserDirPath(userNIF);
        new File(userDirPath).mkdirs();
        return  userDirPath + PREFIJO_USER_JKS + userNIF + SUFIJO_USER_JKS;
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
    
    public String getRootCAServiceURL() {
        if(accessControl == null) return null;
        String serverURL = accessControl.getServerURL();
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
    }
    
}