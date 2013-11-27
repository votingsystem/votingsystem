package org.votingsystem.simulation

import com.lowagie.text.pdf.PdfName
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.tsp.TSPAlgorithms
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.signature.util.VotingSystemKeyGenerator
import org.votingsystem.simulation.util.SimulationUtils
import org.votingsystem.util.FileUtils
import org.votingsystem.util.NifUtils
import org.votingsystem.util.StringUtils

import javax.security.auth.x500.X500PrivateCredential
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.PKIXParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.text.SimpleDateFormat

class ContextService {

	public static String BASEDIR =  System.getProperty("user.home");
	public static String APPDIR =  BASEDIR + File.separator +
			"VotingSystemSimulationContext"  + File.separator;
	public static String ERROR_DIR =  APPDIR + File.separator + "Error";
	public static String APPTEMPDIR =  APPDIR + File.separator + "temp" + File.separator;
	
	private static final String ROOT_ALIAS = "rootAlias";
	public static final String END_ENTITY_ALIAS = "endEntityAlias";
	public static final String PASSWORD = "PemPass";
	private static final long CERT_VALID_FROM = System.currentTimeMillis();
	private static final long ROOT_KEYSTORE_PERIOD = 20000000000L;
	private static final long USER_KEYSTORE_PERIOD = 20000000000L;

	public static String TEMPDIR =  System.getProperty("java.io.tmpdir");
	
	
	public static String CANCEL_VOTE_FILE = "Anulador_";
	public static final String PREFIJO_USER_JKS = "userVS_";
	public static final String SUFIJO_USER_JKS = ".jks";
	
	public static String SOLICITUD_FILE = "SolicitudAcceso_";
	public static String VOTE_FILE = "Vote_";
	
	public static final int MAXIMALONGITUDCAMPO = 255;

	public static final String CSR_FILE_NAME   = "csr";
	
	public static final String ACCESS_REQUEST_FILE_NAME   = "accessRequest";

	public static final String CERT_RAIZ_PATH = "AC_RAIZ_DNIE_SHA1.pem";
	public static final int KEY_SIZE = 1024;
	public static final String SIG_NAME = "RSA";
	public static final String ALIAS_CLAVES = "certificatevoto";
	public static final String PASSWORD_CLAVES = "certificadovoto";
		
	public static final String CERT_AUTENTICATION = "CertAutenticacion";
	public static final String CERT_SIGN = "CertFirmaDigital";
	public static final String CERT_CA = "CertCAIntermediaDGP";
	public static final String LABEL_CLAVE_PRIVADA_AUTENTICACION = "KprivAutenticacion";
	public static final String LABEL_CLAVE_PRIVADA_FIRMA = "KprivFirmaDigital";
	
	//public static final String DNIe_SIGN_MECHANISM = "SHA1withRSA";
	public static final String DNIe_SIGN_MECHANISM = "SHA256withRSA";
	
	public static final String TIMESTAMP_DNIe_HASH = TSPAlgorithms.SHA1;
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
	
	public static final int IMAGE_MAX_FILE_SIZE_KB = 512;
	public static final int IMAGE_MAX_FILE_SIZE = IMAGE_MAX_FILE_SIZE_KB * 1024;
	public static final int SIGNED_MAX_FILE_SIZE_KB = 512;
	public static final int SIGNED_MAX_FILE_SIZE = SIGNED_MAX_FILE_SIZE_KB * 1024;
	
	
	private UserVS userTest;
	private Map<String, VoteVS> receiptMap;
	private AccessControlVS accessControl;
	private ControlCenterVS controlCenter;
	private X509Certificate rootCACert;
	private KeyStore rootCAKeyStore;
	private PrivateKey rootCAPrivateKey;
	private X500PrivateCredential rootCAPrivateCredential;
	private Locale locale = new Locale("es")
	
	def messageSource
	def grailsApplication


    public void init() throws Exception {
		log.debug("-------------  init ----------------- ");
		try {
			new File(ERROR_DIR).mkdirs();
			Properties props = new Properties();

			DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd' 'HH:mm:ss");
			String dateStr = formatter.format(new Date(CERT_VALID_FROM));

			String strSubjectDN = messageSource.getMessage('rootCASubjectDN', [dateStr].toArray(), locale)
			
			rootCAKeyStore = KeyStoreUtil.createRootKeyStore (CERT_VALID_FROM, ROOT_KEYSTORE_PERIOD,
					 PASSWORD.toCharArray(), ROOT_ALIAS, strSubjectDN);
			rootCACert = (X509Certificate)rootCAKeyStore.getCertificate(ROOT_ALIAS);
			rootCAPrivateKey = (PrivateKey)rootCAKeyStore.getKey(ROOT_ALIAS,PASSWORD.toCharArray());
			rootCAPrivateCredential = new X500PrivateCredential(rootCACert, rootCAPrivateKey,  ROOT_ALIAS);
			
			userTest = new UserVS();
			userTest.setNif(NifUtils.getNif(1234567));
			userTest.setName("Test Publisher User");
			userTest.setEmail("testPublisherUser@votingsystem.com");
			KeyStore userKeySTore = KeyStoreUtil.createUserKeyStore(CERT_VALID_FROM, USER_KEYSTORE_PERIOD,
                    PASSWORD.toCharArray(), END_ENTITY_ALIAS, rootCAPrivateCredential,
					"GIVENNAME=NameTestPublisherUser, SURNAME=SurnameTestPublisherUser , SERIALNUMBER=" + userTest.getNif());
			userTest.setKeyStore(userKeySTore);
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex);
		}
		
	}
	
	public String getMessage(String key, String... args) {
		return messageSource.getMessage(key, args, locale)
	}

    /**
	 * @return the rootCACert
	 */
	public X509Certificate getRootCACert() {
		return rootCACert;
	}
	
	public setLocale(Locale locale) {
		this.locale = locale;
		ACH.setLocale(locale)
	}
	
	public void addReceipt(String hashCertVoteBase64, VoteVS receipt) {
		if(receiptMap == null) receiptMap = new HashMap<String, VoteVS>();
		receiptMap.put(hashCertVoteBase64, receipt);
	}
	
	public VoteVS getReceipt(String hashCertVoteBase64) {
		if(receiptMap == null || hashCertVoteBase64 == null) return null;
		return receiptMap.get(hashCertVoteBase64);
	}
	
	public UserVS getUserTest() {
		return userTest;
	}

	/**
	 * @return the accessControl
	 */
	public AccessControlVS getAccessControl() {
		return accessControl;
	}

	/**
	 * @param accessControl the accessControl to set
	 */
	public void setAccessControl(AccessControlVS accessControl) {
		this.accessControl = accessControl;
	}

	/**
	 * @return the controlCenter
	 */
	public ControlCenterVS getControlCenter() {
		return controlCenter;
	}
	
	public PKIXParameters getSessionPKIXParameters() throws InvalidAlgorithmParameterException, Exception {
			log.debug("getSessionPKIXParameters");
		Set<TrustAnchor> anchors = accessControl.getTrustAnchors()
		TrustAnchor rootCACertSessionAnchor = new TrustAnchor(rootCACert, null);
		anchors.add(rootCACertSessionAnchor);
		PKIXParameters sessionPKIXParams = new PKIXParameters(anchors);
		sessionPKIXParams.setRevocationEnabled(false); // tell system do not check CRL's
		return sessionPKIXParams;
	}

	/**
	 * @param controlCenter the controlCenter to set
	 */
	public void setControlCenter(ControlCenterVS controlCenter) {
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

    public SMIMEMessageWrapper getUserTestSMIMEMessage (String toUser, String textToSign, String subject) {
        KeyStore keyStore = userTest.getKeyStore();
        PrivateKey privateKey = (PrivateKey)keyStore.getKey(END_ENTITY_ALIAS, PASSWORD.toCharArray());
        Certificate[] chain = keyStore.getCertificateChain(END_ENTITY_ALIAS);
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(privateKey, chain, DNIe_SIGN_MECHANISM);
        return signedMailGenerator.genMimeMessage(userTest.getEmail(),toUser, textToSign, subject, null);
    }

	public KeyStore generateTestDNIe(String userNIF) throws Exception {
		//logger.info("crearMockDNIe - userNIF: " + userNIF);
		KeyStore keyStore = KeyStoreUtil.createUserKeyStore( CERT_VALID_FROM, USER_KEYSTORE_PERIOD,
				PASSWORD.toCharArray(), END_ENTITY_ALIAS, rootCAPrivateCredential,
				"GIVENNAME=NameDe" + userNIF + " ,SURNAME=ApellidoDe" + userNIF
				+ ", SERIALNUMBER=" + userNIF);
		byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, PASSWORD.toCharArray());
		String userSubPath = SimulationUtils.getUserDirPath(userNIF)
		copyFileToSimDir(keyStoreBytes, userSubPath,  PREFIJO_USER_JKS + userNIF + SUFIJO_USER_JKS)
		return keyStore;
	}
	
	private copyFileToSimDir(byte[] fileToCopy, String subPath, String fileName) {
		String baseDirPath = "${grailsApplication.config.VotingSystem.simulationFilesBaseDir}"
		File newFileDir = new File(baseDirPath + subPath)
		newFileDir.mkdirs()
		File newFile = new File(newFileDir.absolutePath + "/" + fileName);
		log.debug("newFile.path: ${newFile.path}")
		// newFile << new ByteArrayInputStream(fileToCopy) -> append
        newFile.setBytes(fileToCopy)
	}

}
