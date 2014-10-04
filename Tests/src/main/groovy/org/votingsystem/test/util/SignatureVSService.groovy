package org.votingsystem.test.util

import org.apache.log4j.Logger
import org.votingsystem.callable.MessageTimeStamper
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.FileUtils
import org.votingsystem.util.StringUtils

import javax.mail.Header
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.security.auth.x500.X500PrivateCredential
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate

class SignatureVSService {

    private static Logger logger = Logger.getLogger(SignatureVSService.class);

	private SignedMailGenerator signedMailGenerator;
	private X509Certificate certSigner;
    private X500PrivateCredential rootCAPrivateCredential;
    private PrivateKey serverPrivateKey;
    private Encryptor encryptor;

    public SignatureVSService(String keyStorePath, String keyAlias, String password) {
        init(keyStorePath, keyAlias, password)
    }

    public UserVS getUserVS() {
        return UserVS.getUserVS(certSigner)
    }

	public synchronized Map init(String keyStorePath, String keyAlias, String password) throws Exception {
		logger.debug("init")
        byte[] keyStoreBytes = ContextVS.getInstance().getResourceBytes(keyStorePath)

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new ByteArrayInputStream(keyStoreBytes), password.toCharArray());
        java.security.cert.Certificate[] chain = keyStore.getCertificateChain(keyAlias);
		signedMailGenerator = new SignedMailGenerator(keyStore, keyAlias, password.toCharArray(),ContextVS.SIGN_MECHANISM);
		byte[] pemCertsArray
		for (int i = 0; i < chain.length; i++) {
			logger.debug "Adding local kesystore cert '${i}' -> 'SubjectDN: ${chain[i].getSubjectDN()}'"
			if(!pemCertsArray) pemCertsArray = CertUtil.getPEMEncoded (chain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtil.getPEMEncoded (chain[i]))
		}
		certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        serverPrivateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray())
        rootCAPrivateCredential = new X500PrivateCredential(certSigner, serverPrivateKey,  ContextVS.ROOT_ALIAS);

        encryptor = new Encryptor(certSigner, serverPrivateKey);
        return [signedMailGenerator:signedMailGenerator, encryptor:encryptor, rootCAPrivateCredential:rootCAPrivateCredential,
                certSigner:certSigner, serverPrivateKey:serverPrivateKey];
	}

    public X509Certificate getServerCert() {
        if(certSigner == null) certSigner = init().certSigner
        return certSigner
    }

    private PrivateKey getServerPrivateKey() {
        if(serverPrivateKey == null) serverPrivateKey = init().serverPrivateKey
        return serverPrivateKey
    }
    private X500PrivateCredential getRootCAPrivateCredential() {
        if(rootCAPrivateCredential == null) rootCAPrivateCredential = init().rootCAPrivateCredential
        return rootCAPrivateCredential
    }


	public File getSignedFile (String fromUser, String toUser,
		String textToSign, String subject, Header header) {
		logger.debug "getSignedFile - textToSign: ${textToSign}"
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header)
		File resultFile = File.createTempFile("smime", "p7m");
		resultFile.deleteOnExit();
		mimeMessage.writeTo(new FileOutputStream(resultFile));
		return resultFile
	}

    public SMIMEMessage getTimestampedSignedMimeMessage (String fromUser,String toUser,String textToSign,String subject,
                           Header... headers) {
        logger.debug "getTimestampedSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
        if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessage smimeMessage = getSignedMailGenerator().genMimeMessage(
                fromUser, toUser, textToSign, subject, headers)
        MessageTimeStamper timeStamper = new MessageTimeStamper(
                smimeMessage, "${ContextVS.getInstance().config.urlTimeStampServer}/timeStamp")
        ResponseVS responseVS = timeStamper.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        return timeStamper.getSmimeMessage();
    }
		
	public SMIMEMessage getSMIMEMessage (String fromUser,String toUser,String textToSign,String subject, Header... headers) {
		logger.debug "getSMIMEMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessage mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, headers)
		return mimeMessage
	}
		
	public synchronized SMIMEMessage getMultiSignedMimeMessage (
		String fromUser, String toUser,	final SMIMEMessage smimeMessage, String subject) {
		logger.debug("getMultiSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
		if(fromUser) {
			fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setFrom(new InternetAddress(fromUser))
		} 
		if(toUser) {
			toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setHeader("To", toUser)
		}
		SMIMEMessage multiSignedMessage = getSignedMailGenerator().genMultiSignedMessage(smimeMessage, subject);
		return multiSignedMessage
	}


    public ResponseVS encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        logger.debug("encryptToCMS ${new String(dataToEncrypt)}")
        return getEncryptor().encryptToCMS(dataToEncrypt, receiverCert);
    }


    public ResponseVS encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        logger.debug("encryptMessage(...) - ");
        try {
            return getEncryptor().encryptMessage(bytesToEncrypt, publicKey);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(messageSource.getMessage('dataToEncryptErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to decrypt files attached to SMIME (not signed) messages
     */
    public ResponseVS decryptMessage (byte[] encryptedFile, Locale locale) {
        logger.debug "decryptMessage"
        try {
            return getEncryptor().decryptMessage(encryptedFile);
        } catch(Exception ex) {
            logger.error (ex.getMessage(), ex)
            return new ResponseVS(message:messageSource.getMessage('encryptedMessageErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to encrypt SMIME signed messages
     */
    ResponseVS encryptSMIMEMessage(byte[] bytesToEncrypt, X509Certificate receiverCert, Locale locale) throws Exception {
        logger.debug("encryptSMIMEMessage(...) ");
        try {
            return getEncryptor().encryptSMIMEMessage(bytesToEncrypt, receiverCert);
        } catch(Exception ex) {
            logger.error (ex.getMessage(), ex)
            return new ResponseVS(messageSource.getMessage('dataToEncryptErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to decrypt SMIME signed messages
     */
    ResponseVS decryptSMIMEMessage(byte[] encryptedMessageBytes, Locale locale) {
        logger.debug("decryptSMIMEMessage ")
        try {
            return getEncryptor().decryptSMIMEMessage(encryptedMessageBytes);
        } catch(Exception ex) {
            logger.error (ex.getMessage(), ex)
            return new ResponseVS(message:messageSource.getMessage('encryptedMessageErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    private Encryptor getEncryptor() {
        if(encryptor == null) encryptor = init().encryptor
        return encryptor;
    }

	private SignedMailGenerator getSignedMailGenerator() {
		if(signedMailGenerator == null) signedMailGenerator = init().signedMailGenerator
		return signedMailGenerator
	}

    public KeyStore generateKeyStore(String userNIF) throws Exception {
        KeyStore keyStore = KeyStoreUtil.createUserKeyStore(ContextVS.CERT_VALID_FROM, ContextVS.USER_KEYSTORE_PERIOD,
                ContextVS.PASSWORD.toCharArray(), ContextVS.END_ENTITY_ALIAS, getRootCAPrivateCredential(),
                "GIVENNAME=FirstName_" + userNIF + " ,SURNAME=lastName_" + userNIF + ", SERIALNUMBER=" + userNIF);
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, ContextVS.PASSWORD.toCharArray());
        String userSubPath = StringUtils.getUserDirPath(userNIF);
        ContextVS.getInstance().copyFile(keyStoreBytes, userSubPath,  "userVS_" + userNIF + ".jks");
        return keyStore;
    }
}