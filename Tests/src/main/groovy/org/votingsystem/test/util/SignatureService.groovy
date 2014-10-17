package org.votingsystem.test.util

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.callable.MessageTimeStamper
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.test.model.SimulationData
import org.votingsystem.util.*

import javax.mail.Header
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.security.auth.x500.X500PrivateCredential
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate

class SignatureService {

    private static Logger log = Logger.getLogger(SignatureService.class);

    private static Map<String, SignatureService> signatureServices	= new HashMap<>()

    private SignedMailGenerator signedMailGenerator;
	private X509Certificate certSigner;
    private X500PrivateCredential rootCAPrivateCredential;
    private PrivateKey serverPrivateKey;
    private Encryptor encryptor;


    public SignatureService(String keyStorePath, String keyAlias, String password) {
        init(keyStorePath, keyAlias, password)
    }

    public static SignatureService getAuthoritySignatureService() {
        String keyStorePath = ContextVS.getInstance().getConfig().authorityKeyStorePath
        String keyAlias = ContextVS.getInstance().config.authorityKeysAlias
        String password = ContextVS.getInstance().config.authorityKeysPassword
        return new SignatureService(keyStorePath, keyAlias, password)
    }

    public static SignatureService getUserVSSignatureService(String keyStorePath) {
        String keyAlias = ContextVS.getInstance().config.userVSKeysAlias
        String password = ContextVS.getInstance().config.userVSKeysPassword
        return new SignatureService(keyStorePath, keyAlias, password)
    }

    public static SignatureService getUserVSSignatureService(String nif, UserVS.Type userType) {
        if(signatureServices.get(nif) != null) return signatureServices.get(nif)
        String keyStorePath = null
        switch (userType) {
            case UserVS.Type.BANKVS:
                keyStorePath = "./certs/Cert_BankVS_${nif}.jks"
                break;
            default:
                keyStorePath = "./certs/Cert_UserVS_${nif}.jks"
                break;
        }
        log.debug("loading keystore: " + keyStorePath);
        SignatureService signatureService = getUserVSSignatureService(keyStorePath)
        signatureServices.put(nif, signatureService)
        return signatureService
    }

    public UserVS getUserVS() {
        return UserVS.getUserVS(certSigner)
    }

	public synchronized Map init(String keyStorePath, String keyAlias, String password) throws Exception {
		log.debug("init")
        byte[] keyStoreBytes = ContextVS.getInstance().getResourceBytes(keyStorePath)

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new ByteArrayInputStream(keyStoreBytes), password.toCharArray());
        java.security.cert.Certificate[] chain = keyStore.getCertificateChain(keyAlias);
		signedMailGenerator = new SignedMailGenerator(keyStore, keyAlias, password.toCharArray(),ContextVS.SIGN_MECHANISM);
		byte[] pemCertsArray
		for (int i = 0; i < chain.length; i++) {
			log.debug "Adding local kesystore cert '${i}' -> 'SubjectDN: ${chain[i].getSubjectDN()}'"
			if(!pemCertsArray) pemCertsArray = CertUtils.getPEMEncoded (chain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtils.getPEMEncoded (chain[i]))
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
		log.debug "getSignedFile - textToSign: ${textToSign}"
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header)
		File resultFile = File.createTempFile("smime", "p7m");
		resultFile.deleteOnExit();
		mimeMessage.writeTo(new FileOutputStream(resultFile));
		return resultFile
	}

    public SMIMEMessage getTimestampedSignedMimeMessage (String fromUser,String toUser,String textToSign,String subject,
                           Header... headers) {
        log.debug "getTimestampedSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
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
		log.debug "getSMIMEMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessage mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, headers)
		return mimeMessage
	}
		
	public synchronized SMIMEMessage getMultiSignedMimeMessage (
		String fromUser, String toUser,	final SMIMEMessage smimeMessage, String subject) {
		log.debug("getMultiSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
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
        log.debug("encryptToCMS ${new String(dataToEncrypt)}")
        return getEncryptor().encryptToCMS(dataToEncrypt, receiverCert);
    }


    public ResponseVS encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        log.debug("encryptMessage(...) - ");
        try {
            return getEncryptor().encryptMessage(bytesToEncrypt, publicKey);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS("dataToEncryptErrorMsg", statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to decrypt files attached to SMIME (not signed) messages
     */
    public ResponseVS decryptMessage (byte[] encryptedFile, Locale locale) {
        log.debug "decryptMessage"
        try {
            return getEncryptor().decryptMessage(encryptedFile);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS("encryptedMessageErrorMsg", statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to encrypt SMIME signed messages
     */
    ResponseVS encryptSMIMEMessage(byte[] bytesToEncrypt, X509Certificate receiverCert, Locale locale) throws Exception {
        log.debug("encryptSMIMEMessage(...) ");
        try {
            return getEncryptor().encryptSMIMEMessage(bytesToEncrypt, receiverCert);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS("dataToEncryptErrorMsg", statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to decrypt SMIME signed messages
     */
    ResponseVS decryptSMIMEMessage(byte[] encryptedMessageBytes, Locale locale) {
        log.debug("decryptSMIMEMessage ")
        try {
            return getEncryptor().decryptSMIMEMessage(encryptedMessageBytes);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS("encryptedMessageErrorMsg", statusCode:ResponseVS.SC_ERROR_REQUEST)
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

    private List<MockDNI> subscribeUsers(JSONObject subscriptionData, SimulationData simulationData,
              VicketServer vicketServer) throws ExceptionVS {
        log.debug("subscribeUser - Num. Users:" + simulationData.getUserBaseSimulationData().getNumUsers());
        List<MockDNI> userList = new ArrayList<MockDNI>();
        int fromFirstUser = simulationData.getUserBaseSimulationData().getUserIndex().intValue()
        int toLastUser = simulationData.getUserBaseSimulationData().getUserIndex().intValue() +
                simulationData.getUserBaseSimulationData().getNumUsers()
        for(int i = fromFirstUser; i < toLastUser; i++ ) {
            int userIndex = new Long(simulationData.getUserBaseSimulationData().getAndIncrementUserIndex()).intValue();
            String userNif = NifUtils.getNif(userIndex);
            KeyStore mockDnie = generateKeyStore(userNif);
            String toUser = vicketServer.getNameNormalized();
            String subject = "subscribeToGroupMsg - subscribeToGroupMsg"
            subscriptionData.put("UUID", UUID.randomUUID().toString())
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, ContextVS.END_ENTITY_ALIAS,
                    ContextVS.PASSWORD.toCharArray(), ContextVS.DNIe_SIGN_MECHANISM);
            userList.add(new MockDNI(userNif, mockDnie, signedMailGenerator));
            SMIMEMessage smimeMessage = signedMailGenerator.genMimeMessage(userNif, toUser,
                    subscriptionData.toString(), subject);
            SMIMESignedSender worker = new SMIMESignedSender(smimeMessage,
                    vicketServer.getGroupVSSubscriptionServiceURL(simulationData.getGroupId()),
                    vicketServer.getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null, null);
            ResponseVS responseVS = worker.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                throw new ExceptionVS("ERROR nif: " + userNif + " - msg:" + responseVS.getMessage());
            } else simulationData.getUserBaseSimulationData().getAndIncrementnumUserRequestsOK();
            if((i % 50) == 0) log.debug("Subscribed " + i + " of " +
                    simulationData.getUserBaseSimulationData().getNumUsers() + " users to groupVS");
        }
        log.debug("subscribeUser - '" + userList.size() + "' user subscribed")
        return userList
    }

    public List<JSONObject> validateUserVSSubscriptions(Long groupVSId, VicketServer vicketServer,
            Map<String, MockDNI> userVSMap) throws ExceptionVS {
        log.debug("validateUserVSSubscriptions");
        ResponseVS responseVS = HttpHelper.getInstance().getData(vicketServer.getGroupVSUsersServiceURL(
                groupVSId, 1000, 0, SubscriptionVS.State.PENDING, UserVS.State.ACTIVE),
                ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
        JSONObject dataJSON = JSONSerializer.toJSON(responseVS.getMessage())
        JSONArray jsonArray = dataJSON.getJSONArray("userVSList");
        List<JSONObject> usersToActivate = new ArrayList<>();
        for(int i = 0; i < jsonArray.size() ; i++) {
            JSONObject userSubscriptionData = jsonArray.get(i);
            if(UserVS.State.PENDING == UserVS.State.valueOf(userSubscriptionData.getString("state"))){
                JSONObject userVSData = userSubscriptionData.getJSONObject("uservs")
                if(userVSMap.get(userVSData.getString("NIF")) != null) usersToActivate.add(userSubscriptionData);
            }
        }
        if(usersToActivate.size() != userVSMap.values().size()) throw new ExceptionVS("Expected '" + userVSMap.values().size() +
                "' pending users and found '" + usersToActivate.size() + "'");
        List<JSONObject> requests = new ArrayList<>();
        for(JSONObject userToActivate:usersToActivate) {
            JSONObject request = new JSONObject();
            request.put("operation", TypeVS.VICKET_GROUP_USER_ACTIVATE.toString())
            request.put("groupvs", userToActivate.getJSONObject("groupvs"));
            request.put("uservs", userToActivate.getJSONObject("uservs"));
            requests.add(request)
        }
        String messageSubject = "TEST_ACTIVATE_GROUPVS_USERS"
        UserVS userVS = UserVS.getUserVS(certSigner)
        for(JSONObject request:requests) {
            SMIMEMessage smimeMessage = getTimestampedSignedMimeMessage(userVS.nif,
                    vicketServer.getNameNormalized(), request.toString(), messageSubject)
            responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    vicketServer.getGroupVSUsersActivationServiceURL())
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
        }
        return requests
    }
}