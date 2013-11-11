package org.votingsystem.model;

import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.mail.Session;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ContextVS {
    
    private static Logger logger = Logger.getLogger(ContextVS.class);

    public static final Session MAIL_SESSION = Session.getDefaultInstance(
    		System.getProperties(), null);
    
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
    
    
    public static String CANCEL_VOTE_FILE = "Anulador_";
    public static final String PREFIJO_USER_JKS = "usuario_"; 
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
    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    public static final String ALIAS_CLAVES = "certificadovoto";
    public static final String PASSWORD_CLAVES = "certificadovoto";
        
    public static final String CERT_AUTENTICATION = "CertAutenticacion";
    public static final String CERT_SIGN = "CertFirmaDigital";
    public static final String CERT_CA = "CertCAIntermediaDGP";
    public static final String LABEL_CLAVE_PRIVADA_AUTENTICACION = "KprivAutenticacion";
    public static final String LABEL_CLAVE_PRIVADA_FIRMA = "KprivFirmaDigital";
        
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
    public static final String JSON_CONTENT_TYPE    = "application/json";
    
    public static final String PDF_CONTENT_TYPE    = "application/pdf";
    public static final String SIGNED_CONTENT_TYPE = "application/x-pkcs7-signature";
    public static final String X509_CONTENT_TYPE = "application/x-x509-ca-cert";
    public static final String ENCRYPTED_CONTENT_TYPE = "application/x-pkcs7-mime";
    public static final String SIGNED_AND_ENCRYPTED_CONTENT_TYPE = 
            SIGNED_CONTENT_TYPE + ";" + ENCRYPTED_CONTENT_TYPE;
    public static final String PDF_SIGNED_AND_ENCRYPTED_CONTENT_TYPE = 
            PDF_CONTENT_TYPE + ";" +  SIGNED_CONTENT_TYPE + ";" + ENCRYPTED_CONTENT_TYPE;    
    public static final String PDF_SIGNED_CONTENT_TYPE = 
    		PDF_CONTENT_TYPE + ";" + SIGNED_CONTENT_TYPE;     
    public static final String PDF_ENCRYPTED_CONTENT_TYPE = 
    		PDF_CONTENT_TYPE + ";" + ENCRYPTED_CONTENT_TYPE; 
    
    public static final int IMAGE_MAX_FILE_SIZE_KB = 512;
    public static final int IMAGE_MAX_FILE_SIZE = IMAGE_MAX_FILE_SIZE_KB * 1024;
    public static final int SIGNED_MAX_FILE_SIZE_KB = 512;
    public static final int SIGNED_MAX_FILE_SIZE = SIGNED_MAX_FILE_SIZE_KB * 1024;
    
    public static final String MULTISIGNED_FILE_NAME = "MultiSign";

    //smime.p7m -> Email message encrypted
    //smime.p7s -> Email message that includes a digital signature
    public static final String SIGNED_PART_EXTENSION = ".p7s";
    
    private static String userVSClassName = "org.votingsystem.model.UserVSBase";
    
    private AppHostVS appHost;
    private ResourceBundle resourceBundle;
    private UserVS userVS;
    private ActorVS accessControl;
    private ActorVS controlCenter;
    public static ContextVS INSTANCE;
    
    private ContextVS(){}
    
    private ContextVS(AppHostVS appHost, 
            String localizatedMessagesFileName, String locale) {
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
        VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE);
        INSTANCE = new ContextVS(appHost, localizatedMessagesFileName, locale);
    }
    
    public static void init (String userVSClassName) throws Exception {
    	INSTANCE = new ContextVS();
    	INSTANCE.setUserVSClassName(userVSClassName);
    	VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE);
    }
    
    public void setUserVSClassName(String className) {
    	userVSClassName = className;
    }
    
    public UserVS getUserVS(X509Certificate certificate) throws 
    	ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class userVSClass = Class.forName(userVSClassName);
        UserVS userVS = (UserVS)userVSClass.newInstance();
    	userVS.setCertificate(certificate);
    	String subjectDN = certificate.getSubjectDN().getName();
    	if (subjectDN.contains("C="))
    		userVS.setPais(subjectDN.split("C=")[1].split(",")[0]);
    	if (subjectDN.contains("SERIALNUMBER="))
    		userVS.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
    	if (subjectDN.contains("SURNAME="))
    		userVS.setPrimerApellido(subjectDN.split("SURNAME=")[1].split(",")[0]);
    	if (subjectDN.contains("GIVENNAME="))
    		userVS.setNombre(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
    	if (subjectDN.contains("CN="))
    		userVS.setCn(subjectDN.split("CN=")[1]);
		if(subjectDN.split("OU=email:").length > 1) {
			userVS.setEmail(subjectDN.split("OU=email:")[1].split(",")[0]);
		}
		if(subjectDN.split("CN=nif:").length > 1) {
			String nif = subjectDN.split("CN=nif:")[1];
			if (nif.split(",").length > 1) {
				nif = nif.split(",")[0];
			}
			userVS.setNif(nif);
		}
		if (subjectDN.split("OU=telefono:").length > 1) {
			userVS.setTelefono(subjectDN.split("OU=telefono:")[1].split(",")[0]);
		}
    	return userVS;    
    }
    
    
    public void sendMessageToHost(OperationVS operacion) {
        appHost.sendMessageToHost(operacion);
    }
    
    public UserVS getUserVS() throws 
        ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class userVSClass = Class.forName(userVSClassName);
        UserVS userVS = (UserVS)userVSClass.newInstance();
        return userVS;    
    }
    
        
    public void setSessionUser(UserVS userVS) {
    	logger.debug("setUsuario - nif: " + userVS.getNif());
        this.userVS = userVS;
    }
    
    public UserVS getSessionUser() {
        return userVS;
    }

    /**
     * @return the appHost
     */
    public AppHostVS getAppHost() {
        return appHost;
    }

    /**
     * @param appHost the appHost to set
     */
    public void setAppHost(AppHostVS appHost) {
        this.appHost = appHost;
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
    
    /**
     * @return the accessControl
     */
    public ActorVS getAccessControl() {
        return accessControl;
    }

    /**
     * @param accessControl the accessControl to set
     */
    public void setAccessControl(ActorVS accessControl) {
        this.accessControl = accessControl;
    }
    
    /**
     * @return the controlCenter
     */
    public ActorVS getControlCenter() {
        return controlCenter;
    }

    /**
     * @param controlCenter the controlCenter to set
     */
    public void setControlCenter(ActorVS controlCenter) {
        this.controlCenter = controlCenter;
    }
    
    public String getString(String key, Object... arguments) {
        String pattern = resourceBundle.getString(key);
        if(arguments.length > 0)
            return MessageFormat.format(pattern, arguments);
        else return resourceBundle.getString(key);
    }

}
