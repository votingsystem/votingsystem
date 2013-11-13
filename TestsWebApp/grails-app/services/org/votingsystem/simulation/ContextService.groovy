package org.votingsystem.simulation

import java.security.cert.X509Certificate;
import java.util.Map;

import org.votingsystem.model.ActorVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.simulation.util.HttpHelper;

import com.itextpdf.text.pdf.PdfName;
import org.votingsystem.simulation.util.SimulationUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.PKIXParameters
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.apache.log4j.Logger;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;
import org.votingsystem.model.EventVS
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.UserVSBase
import org.votingsystem.model.VoteVS
import org.votingsystem.simulation.ApplicationContextHolder;
import org.votingsystem.simulation.util.HttpHelper;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.NifUtils;

import com.lowagie.text.pdf.PdfName;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.security.auth.x500.X500PrivateCredential
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.CertUtil;

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
	public static final String PREFIJO_USER_JKS = "usuario_";
	public static final String SUFIJO_USER_JKS = ".jks";
	
	public static String SOLICITUD_FILE = "SolicitudAcceso_";
	public static String VOTE_FILE = "Vote_";
	
	public static String APPVOTODIR =  APPDIR + File.separator +
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
	public static final String ALIAS_CLAVES = "certificadovoto";
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
	
	public static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";
	
	public static final int IMAGE_MAX_FILE_SIZE_KB = 512;
	public static final int IMAGE_MAX_FILE_SIZE = IMAGE_MAX_FILE_SIZE_KB * 1024;
	public static final int SIGNED_MAX_FILE_SIZE_KB = 512;
	public static final int SIGNED_MAX_FILE_SIZE = SIGNED_MAX_FILE_SIZE_KB * 1024;
	
	
	private UserVS userTest;
	private HttpHelper httpHelper;
	private Map<String, VoteVS> receiptMap;
	private ActorVS accessControl;
	private ActorVS controlCenter;
	private X509Certificate rootCACert;
	private KeyStore rootCAKeyStore;
	private PrivateKey rootCAPrivateKey;
	private X500PrivateCredential rootCAPrivateCredential;
	private Locale locale = new Locale("es")
	
	def messageSource
	def grailsApplication
	
	public void init() throws Exception {
		log.debug("-------------  init ----------------- ");
		VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, ContextVS.PROVIDER, KEY_SIZE);
		try {
			new File(ERROR_DIR).mkdirs();
			Properties props = new Properties();

			DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd' 'HH:mm:ss");
			String dateStr = formatter.format(new Date(CERT_VALID_FROM));

			String strSubjectDN = messageSource.getMessage('rootCASubjectDN', [dateStr].toArray(), locale)
			
			rootCAKeyStore = createRootKeyStore(
					 CERT_VALID_FROM, ROOT_KEYSTORE_PERIOD,
					 PASSWORD.toCharArray(), ROOT_ALIAS, strSubjectDN);
			rootCACert = (X509Certificate)rootCAKeyStore.
					getCertificate(ROOT_ALIAS);
			rootCAPrivateKey = (PrivateKey)rootCAKeyStore.
					getKey(ROOT_ALIAS,PASSWORD.toCharArray());
			rootCAPrivateCredential = new X500PrivateCredential(
					 rootCACert, rootCAPrivateKey,  ROOT_ALIAS);
			
			userTest = new UserVSBase();
			userTest.setNif(NifUtils.getNif(1234567));
			userTest.setNombre("Test Publisher User");
			userTest.setEmail("testPublisherUser@votingsystem.com");
			KeyStore userKeySTore = KeyStoreUtil.createUserKeyStore(
					CERT_VALID_FROM, USER_KEYSTORE_PERIOD, PASSWORD.toCharArray(),
					END_ENTITY_ALIAS, rootCAPrivateCredential,
					"GIVENNAME=NameTestPublisherUser, SURNAME=SurnameTestPublisherUser" +
					", SERIALNUMBER=" + userTest.getNif());
			userTest.setKeyStore(userKeySTore);
			httpHelper = new HttpHelper();
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex);
		}
		
	}
	
	public getMessage(String key, String[] args) {
		return messageSource.getMessage(key, args, locale)
	}
	
	
	public KeyStore createRootKeyStore(long begin, long period,
		char[] password, String rootAlias, String strSubjectDN) throws Exception {
		KeyStore store = KeyStore.getInstance("JKS");
		store.load(null, null);
		Date dateBegin = new Date(begin) 
		Date dateFinish = new Date(begin + period)
		KeyPair rootPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
		X509Certificate rootCert = CertUtil.generateV3RootCert(
				rootPair, dateBegin, dateFinish, strSubjectDN);
		X500PrivateCredential rootCredential = new X500PrivateCredential(
				rootCert, rootPair.getPrivate(), rootAlias);
		store.setCertificateEntry(
				rootCredential.getAlias(), rootCredential.getCertificate());
		Certificate[] certChain =  [rootCredential.getCertificate()].toArray(Certificate[])
		store.setKeyEntry(rootCredential.getAlias(), rootCredential.getPrivateKey(), password, certChain);
		return store;
	}
	
	/**
	 * @return the rootCACert
	 */
	public X509Certificate getRootCACert() {
		return rootCACert;
	}
	
	public String getRootCAServiceURL(String serverURL) {
		if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
		return serverURL + "certificado/addCertificateAuthority";
	}
	
	public setLocale(Locale locale) {
		this.locale = locale;
		ACH.setLocale(locale)
	}
	
	public static String getServerInfoURL(String serverURL) {
		if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
		return serverURL + "infoServidor";
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

	public static String getRutaArchivosVoto(EventVS evento, String nif) {
		String ruta = APPVOTODIR + StringUtils.getCadenaNormalizada(
				evento.getControlAcceso().getServerURL()) +
				"-" + nif + "-" + evento.getEventoId() + File.separator;
		return ruta;
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

	public HttpHelper getHttpHelper() {
		return httpHelper;
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
	
	public PKIXParameters getSessionPKIXParameters()
		throws InvalidAlgorithmParameterException, Exception {
			log.debug("getSessionPKIXParameters");
		Set<TrustAnchor> anchors = accessControl.getTrustAnchors()
		TrustAnchor anchorUserCert = new TrustAnchor(rootCACert, null);
		anchors.add(anchorUserCert);
		PKIXParameters sessionPKIXParams = new PKIXParameters(anchors);
		sessionPKIXParams.setRevocationEnabled(false); // tell system do not check CRL's
		return sessionPKIXParams;
	}

	/**
	 * @param controlCenter the controlCenter to set
	 */
	public void setControlCenter(ActorVS controlCenter) {
		this.controlCenter = controlCenter;
	}
	
	public String getCancelEventURL() {
		if (accessControl == null) return null;
		String serverURL = accessControl.getServerURL();
		if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
		return serverURL + "evento/cancelled";
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
	
	public KeyStore generateTestDNIe(String userNIF) throws Exception {
		//logger.info("crearMockDNIe - userNIF: " + userNIF);
		KeyStore keyStore = KeyStoreUtil.createUserKeyStore(
				CERT_VALID_FROM, USER_KEYSTORE_PERIOD,
				PASSWORD.toCharArray(), END_ENTITY_ALIAS, rootCAPrivateCredential,
				"GIVENNAME=NombreDe" + userNIF + " ,SURNAME=ApellidoDe" + userNIF
				+ ", SERIALNUMBER=" + userNIF);
		byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, PASSWORD.toCharArray());
		String userSubPath = SimulationUtils.getUserDirPath(userNIF)
		copyFileToSimulationDir(keyStoreBytes, userSubPath,  PREFIJO_USER_JKS + userNIF + SUFIJO_USER_JKS)
		return keyStore;
	}
	
	private copyFileToSimulationDir(byte[] fileToCopy, String subPath, String fileName) {
		String baseDirPath = "${grailsApplication.config.VotingSystem.simulationFilesBaseDir}"
		File newFileDir = new File(baseDirPath + subPath)
		newFileDir.mkdirs()
		File newFile = new File(newFileDir.absolutePath + "/" + fileName);
		log.debug("newFile.path: ${newFile.path}")
		newFile << new ByteArrayInputStream(fileToCopy)
	}

}
