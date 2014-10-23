package org.votingsystem.test.util

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.callable.MessageTimeStamper
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.*

import javax.mail.Header
import javax.mail.internet.InternetAddress
import javax.security.auth.x500.X500PrivateCredential
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap

class SignatureService {

    private static Logger log = Logger.getLogger(SignatureService.class);

    private static ConcurrentHashMap<String, SignatureService> signatureServices	= new ConcurrentHashMap<>()

    private SMIMESignedGeneratorVS signedMailGenerator;
	private X509Certificate certSigner;
    private Certificate[] certSignerChain;
    private X500PrivateCredential rootCAPrivateCredential;
    private PrivateKey privateKey;
    private Encryptor encryptor;
    private static SignatureService authoritySignatureService;
    private KeyStore keyStore
    private UserVS userVS;

    public SignatureService(KeyStore keyStore, String keyAlias, String password) {
        init(keyStore, keyAlias, password)
    }

    public synchronized Map init(KeyStore keyStore, String keyAlias, String password) throws Exception {
        log.debug("init")
        this.keyStore = keyStore
        certSignerChain = keyStore.getCertificateChain(keyAlias);
        signedMailGenerator = new SMIMESignedGeneratorVS(keyStore, keyAlias, password.toCharArray(),ContextVS.SIGN_MECHANISM);
        byte[] pemCertsArray
        for (int i = 0; i < certSignerChain.length; i++) {
            log.debug "Adding local kesystore cert '${i}' -> 'SubjectDN: ${certSignerChain[i].getSubjectDN()}'"
            if(!pemCertsArray) pemCertsArray = CertUtils.getPEMEncoded (certSignerChain[i])
            else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtils.getPEMEncoded (certSignerChain[i]))
        }
        certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        privateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray())
        rootCAPrivateCredential = new X500PrivateCredential(certSigner, privateKey,  ContextVS.ROOT_ALIAS);
        encryptor = new Encryptor(certSigner, privateKey);
        return [signedMailGenerator:signedMailGenerator, encryptor:encryptor, certSigner:certSigner,
                privateKey:privateKey, rootCAPrivateCredential:rootCAPrivateCredential];
    }

    public static SignatureService getAuthoritySignatureService() {
        if(authoritySignatureService != null) return authoritySignatureService
        String keyStorePath = ContextVS.getInstance().getConfig().authorityKeyStorePath
        String keyAlias = ContextVS.getInstance().config.authorityKeysAlias
        String password = ContextVS.getInstance().config.authorityKeysPassword
        KeyStore keyStore = loadKeyStore(keyStorePath, keyAlias, password);
        authoritySignatureService = new SignatureService(keyStore, keyAlias, password)
        signatureServices.put(authoritySignatureService.getUserVS().nif, authoritySignatureService)
        return authoritySignatureService
    }

    public static SignatureService getUserVSSignatureService(String nif, UserVS.Type userType) {
        if(signatureServices.get(nif) != null) {
            SignatureService sv = signatureServices.get(nif)
            return sv
        }
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
        String keyAlias = ContextVS.getInstance().config.userVSKeysAlias
        String password = ContextVS.getInstance().config.userVSKeysPassword
        KeyStore keyStore = loadKeyStore(keyStorePath, keyAlias, password);
        SignatureService signatureService = new SignatureService(keyStore, keyAlias, password)
        signatureServices.put(nif, signatureService)
        return signatureService
    }

    public static SignatureService genUserVSSignatureService(String nif) {
        KeyStore mockDnie = getAuthoritySignatureService().generateKeyStore(nif)
        SignatureService signatureService = new SignatureService(mockDnie, ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD);
        signatureServices.put(nif, signatureService)
        return signatureService
    }

    public UserVS getUserVS() {
        if(userVS == null) userVS = UserVS.getUserVS(certSigner)
        return userVS
    }

    public  KeyStore getKeyStore() {
        return keyStore;
    }

    public X509Certificate getCertSigner() {
        if(certSigner == null) certSigner = init().certSigner
        return certSigner
    }

    public Certificate[] getCertSignerChain() {
        return certSignerChain;
    }

    public PrivateKey getPrivateKey() {
        if(privateKey == null) privateKey = init().privateKey
        return privateKey
    }
    private X500PrivateCredential getRootCAPrivateCredential() {
        if(rootCAPrivateCredential == null) rootCAPrivateCredential = init().rootCAPrivateCredential
        return rootCAPrivateCredential
    }


    public static KeyStore loadKeyStore(String keyStorePath, String keyAlias, String password) {
        byte[] keyStoreBytes = ContextVS.getInstance().getResourceBytes(keyStorePath)
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new ByteArrayInputStream(keyStoreBytes), password.toCharArray());
        return keyStore
    }

    public SMIMEMessage getSMIMETimeStamped (String fromUser,String toUser,String textToSign,String subject,
                       Header... headers) {
        log.debug "getSMIMETimeStamped - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
        if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessage smimeMessage = getSignedMailGenerator().getSMIME(
                fromUser, toUser, textToSign, subject, headers)
        MessageTimeStamper timeStamper = new MessageTimeStamper(
                smimeMessage, "${ContextVS.getInstance().config.urlTimeStampServer}/timeStamp")
        ResponseVS responseVS = timeStamper.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        return timeStamper.getSMIME();
    }
		
	public SMIMEMessage getSMIME (String fromUser,String toUser,String textToSign,String subject, Header... headers) {
		log.debug "getSMIME - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessage mimeMessage = getSignedMailGenerator().getSMIME(fromUser, toUser, textToSign, subject, headers)
		return mimeMessage
	}
		
	public synchronized SMIMEMessage getSMIMEMultiSigned (
		String fromUser, String toUser,	final SMIMEMessage smimeMessage, String subject) {
		log.debug("getSMIMEMultiSigned - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
		if(fromUser) {
			fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setFrom(new InternetAddress(fromUser))
		} 
		if(toUser) {
			toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setHeader("To", toUser)
		}
		SMIMEMessage multiSignedMessage = getSignedMailGenerator().getSMIMEMultiSigned(smimeMessage, subject);
		return multiSignedMessage
	}


    public ResponseVS encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        return getEncryptor().encryptToCMS(dataToEncrypt, receiverCert);
    }

    public byte[] decryptCMS (byte[] encryptedFile, Locale locale) {
        return getEncryptor().decryptCMS(encryptedFile);
    }

    public ResponseVS encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        return getEncryptor().encryptMessage(bytesToEncrypt, publicKey);
    }

    /**
     * Method to decrypt files attached to SMIME (not signed) messages
     */
    public ResponseVS decryptMessage (byte[] encryptedFile, Locale locale) {
        return getEncryptor().decryptMessage(encryptedFile);
    }

    /**
     * Method to encrypt SMIME signed messages
     */
    ResponseVS encryptSMIME(byte[] bytesToEncrypt, X509Certificate receiverCert, Locale locale) throws Exception {
        return getEncryptor().encryptSMIME(bytesToEncrypt, receiverCert);
    }

    /**
     * Method to decrypt SMIME signed messages
     */
    ResponseVS decryptSMIME(byte[] encryptedMessageBytes) {
        return getEncryptor().decryptSMIME(encryptedMessageBytes);
    }

    private Encryptor getEncryptor() {
        if(encryptor == null) encryptor = init().encryptor
        return encryptor;
    }

	private SMIMESignedGeneratorVS getSignedMailGenerator() {
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
        log.debug("subscribeUser - Num. Users:" + simulationData.getNumRequestsProjected());
        List<MockDNI> userList = new ArrayList<MockDNI>();
        int fromFirstUser = simulationData.getUserBaseSimulationData().getUserIndex().intValue()
        int toLastUser = simulationData.getUserBaseSimulationData().getUserIndex().intValue() +
                simulationData.getNumRequestsProjected()
        for(int i = fromFirstUser; i < toLastUser; i++ ) {
            int userIndex = new Long(simulationData.getUserBaseSimulationData().getAndIncrementUserIndex()).intValue();
            String userNif = NifUtils.getNif(userIndex);
            KeyStore mockDnie = generateKeyStore(userNif);
            String toUser = vicketServer.getNameNormalized();
            String subject = "subscribeToGroupMsg - subscribeToGroupMsg"
            subscriptionData.put("UUID", UUID.randomUUID().toString())
            SMIMESignedGeneratorVS signedMailGenerator = new SMIMESignedGeneratorVS(mockDnie, ContextVS.END_ENTITY_ALIAS,
                    ContextVS.PASSWORD.toCharArray(), ContextVS.DNIe_SIGN_MECHANISM);
            userList.add(new MockDNI(userNif, mockDnie, signedMailGenerator));
            SMIMEMessage smimeMessage = signedMailGenerator.getSMIME(userNif, toUser,
                    subscriptionData.toString(), subject);
            SMIMESignedSender worker = new SMIMESignedSender(smimeMessage,
                    vicketServer.getGroupVSSubscriptionServiceURL(simulationData.getGroupId()),
                    vicketServer.getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null, null);
            ResponseVS responseVS = worker.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                throw new ExceptionVS("ERROR nif: " + userNif + " - msg:" + responseVS.getMessage());
            } else simulationData.getAndIncrementNumRequestsOK();
            if((i % 50) == 0) log.debug("Subscribed " + i + " of " +
                    simulationData.getUserBaseSimulationData().getNumRequestsProjected() + " users to groupVS");
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
        if(usersToActivate.size() != userVSMap.size()) throw new ExceptionVS("Expected '" + userVSMap.size() +
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
            SMIMEMessage smimeMessage = getSMIMETimeStamped(userVS.nif,
                    vicketServer.getNameNormalized(), request.toString(), messageSubject)
            responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    vicketServer.getGroupVSUsersActivationServiceURL())
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage())
        }
        return requests
    }
}