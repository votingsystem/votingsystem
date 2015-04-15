package org.votingsystem.test.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.dto.currency.SubscriptionVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.*;

import javax.mail.Header;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;


public class SignatureService {

    private static Logger log = Logger.getLogger(SignatureService.class.getSimpleName());

    private static ConcurrentHashMap<String, SignatureService> signatureServices	= new ConcurrentHashMap<>();

    private SMIMESignedGeneratorVS signedMailGenerator;
	private X509Certificate certSigner;
    private Certificate[] certSignerChain;
    private X500PrivateCredential rootCAPrivateCredential;
    private PrivateKey privateKey;
    private Encryptor encryptor;
    private static SignatureService authoritySignatureService;
    private KeyStore keyStore;
    private UserVS userVS;

    public SignatureService(KeyStore keyStore, String keyAlias, String password) throws Exception {
        init(keyStore, keyAlias, password);
    }

    public synchronized void init(KeyStore keyStore, String keyAlias, String password) throws Exception {
        log.info("init");
        this.keyStore = keyStore;
        certSignerChain = keyStore.getCertificateChain(keyAlias);
        signedMailGenerator = new SMIMESignedGeneratorVS(keyStore, keyAlias, password.toCharArray(),ContextVS.SIGN_MECHANISM);
        byte[] pemCertsArray = null;
        for (int i = 0; i < certSignerChain.length; i++) {
            log.info("Adding local kesystore");
            if(pemCertsArray == null) pemCertsArray = CertUtils.getPEMEncoded (certSignerChain[i]);
            else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtils.getPEMEncoded (certSignerChain[i]));
        }
        certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        privateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
        rootCAPrivateCredential = new X500PrivateCredential(certSigner, privateKey,  "rootAlias");
        encryptor = new Encryptor(certSigner, privateKey);
    }

    public static SignatureService getAuthoritySignatureService() throws Exception {
        if(authoritySignatureService != null) return authoritySignatureService;
        String keyStorePath = ContextVS.getInstance().getProperty("authorityKeyStorePath");
        String keyAlias = ContextVS.getInstance().getProperty("userVSKeyAlias");
        String password = ContextVS.getInstance().getProperty("userVSKeyPassword");
        KeyStore keyStore = loadKeyStore(keyStorePath, keyAlias, password);
        authoritySignatureService = new SignatureService(keyStore, keyAlias, password);
        signatureServices.put(authoritySignatureService.getUserVS().getNif(), authoritySignatureService);
        return authoritySignatureService;
    }

    public static SignatureService getUserVSSignatureService(String nif, UserVS.Type userType) throws Exception {
        if(signatureServices.get(nif) != null) {
            SignatureService sv = signatureServices.get(nif);
            return sv;
        }
        String keyStorePath = format("./certs/Cert_{0}_{1}.jks", userType.toString(), nif);
        log.info("loading keystore: " + keyStorePath);
        String keyAlias = ContextVS.getInstance().getProperty("userVSKeyAlias", "userVSKeyAlias");
        String password = ContextVS.getInstance().getProperty("userVSKeyPassword", "userVSKeyPassword");
        KeyStore keyStore = loadKeyStore(keyStorePath, keyAlias, password);

        SignatureService signatureService = new SignatureService(keyStore, keyAlias, password);
        signatureServices.put(nif, signatureService);
        return signatureService;
    }

    public static SignatureService genUserVSSignatureService(String nif) throws Exception {
        KeyStore mockDnie = getAuthoritySignatureService().generateKeyStore(nif);
        SignatureService signatureService = new SignatureService(mockDnie, ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD);
        signatureServices.put(nif, signatureService);
        return signatureService;
    }

    public UserVS getUserVS() {
        if(userVS == null) userVS = UserVS.getUserVS(certSigner);
        return userVS;
    }

    public  KeyStore getKeyStore() {
        return keyStore;
    }

    public X509Certificate getCertSigner() {
        return certSigner;
    }

    public Certificate[] getCertSignerChain() {
        return certSignerChain;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
    private X500PrivateCredential getRootCAPrivateCredential() {
        return rootCAPrivateCredential;
    }

    public static KeyStore loadKeyStore(String keyStorePath, String keyAlias, String password) throws Exception {
        byte[] keyStoreBytes = ContextVS.getInstance().getResourceBytes(keyStorePath);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new ByteArrayInputStream(keyStoreBytes), password.toCharArray());
        return keyStore;
    }

    public SMIMEMessage getSMIMETimeStamped (String fromUser,String toUser,String textToSign,String subject,
                       Header... headers) throws Exception {
        log.info(format("getSMIMETimeStamped - subject {0} - fromUser {1} to user {2}", subject,fromUser, toUser));
        SMIMEMessage smimeMessage = getSignedMailGenerator().getSMIME(fromUser, toUser, textToSign, subject, headers);
        String timeStampServiceURL = ActorVS.getTimeStampServiceURL(ContextVS.getInstance().getProperty("timeStampServerURL"));
        MessageTimeStamper timeStamper = new MessageTimeStamper(
                smimeMessage,  timeStampServiceURL);
        return timeStamper.call();
    }
		
	public SMIMEMessage getSMIME (String fromUser,String toUser,String textToSign,String subject,
                                  Header... headers) throws Exception {
		log.info("getSMIME");
		return getSignedMailGenerator().getSMIME(fromUser, toUser, textToSign, subject, headers);
	}
		
	public synchronized SMIMEMessage getSMIMEMultiSigned (
		String fromUser, String toUser,	final SMIMEMessage smimeMessage, String subject) throws Exception {
		log.info(format("getSMIMEMultiSigned - subject {0} - fromUser {1} to user {2}",
                subject,fromUser, toUser));
		return getSignedMailGenerator().getSMIMEMultiSigned(fromUser, toUser, smimeMessage, subject);
	}

    public byte[] encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        return getEncryptor().encryptToCMS(dataToEncrypt, receiverCert);
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt) throws Exception {
        return getEncryptor().encryptToCMS(dataToEncrypt, certSigner);
    }

    public byte[] decryptCMS (byte[] encryptedFile) throws Exception {
        return getEncryptor().decryptCMS(encryptedFile);
    }

    public byte[] encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        return getEncryptor().encryptMessage(bytesToEncrypt, publicKey);
    }

    public byte[] decryptMessage (byte[] encryptedFile) throws Exception {
        return getEncryptor().decryptMessage(encryptedFile);
    }

    ResponseVS encryptSMIME(byte[] bytesToEncrypt, X509Certificate receiverCert) throws Exception {
        return getEncryptor().encryptSMIME(bytesToEncrypt, receiverCert);
    }

    ResponseVS decryptSMIME(byte[] encryptedMessageBytes) throws Exception {
        return getEncryptor().decryptSMIME(encryptedMessageBytes);
    }

    private Encryptor getEncryptor() {
        return encryptor;
    }

	private SMIMESignedGeneratorVS getSignedMailGenerator() {
		return signedMailGenerator;
	}

    public KeyStore generateKeyStore(String userNIF) throws Exception {
        Date dateBegin = new Date();
        Date dateFinish = DateUtils.addDays(dateBegin, 365).getTime(); //one year
        KeyStore keyStore = KeyStoreUtil.createUserKeyStore(dateBegin.getTime(), dateFinish.getTime() - dateBegin.getTime(),
                ContextVS.PASSWORD.toCharArray(), ContextVS.END_ENTITY_ALIAS, getRootCAPrivateCredential(),
                "GIVENNAME=FirstName_" + userNIF + " ,SURNAME=lastName_" + userNIF + ", SERIALNUMBER=" + userNIF);
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, ContextVS.PASSWORD.toCharArray());
        String userSubPath = StringUtils.getUserDirPath(userNIF);
        ContextVS.getInstance().copyFile(keyStoreBytes, userSubPath,  "userVS_" + userNIF + ".jks");
        return keyStore;
    }

    public List<MockDNI> subscribeUsers(GroupVSDto groupVSDto, SimulationData simulationData,
                            CurrencyServer currencyServer) throws Exception {
        log.info("subscribeUser - Num. Users:" + simulationData.getNumRequestsProjected());
        List<MockDNI> userList = new ArrayList<MockDNI>();
        int fromFirstUser = simulationData.getUserBaseSimulationData().getUserIndex().intValue();
        int toLastUser = simulationData.getUserBaseSimulationData().getUserIndex().intValue() +
                simulationData.getNumRequestsProjected();
        for(int i = fromFirstUser; i < toLastUser; i++ ) {
            int userIndex = new Long(simulationData.getUserBaseSimulationData().getAndIncrementUserIndex()).intValue();
            String userNif = NifUtils.getNif(userIndex);
            KeyStore mockDnie = generateKeyStore(userNif);
            String toUser = currencyServer.getName();
            String subject = "subscribeToGroupMsg - subscribeToGroupMsg";
            groupVSDto.setUUID(UUID.randomUUID().toString());
            SMIMESignedGeneratorVS signedMailGenerator = new SMIMESignedGeneratorVS(mockDnie, ContextVS.END_ENTITY_ALIAS,
                    ContextVS.PASSWORD.toCharArray(), ContextVS.DNIe_SIGN_MECHANISM);
            userList.add(new MockDNI(userNif, mockDnie, signedMailGenerator));
            SMIMEMessage smimeMessage = signedMailGenerator.getSMIME(userNif, toUser,
                    JSON.getMapper().writeValueAsString(groupVSDto), subject);
            SMIMESignedSender worker = new SMIMESignedSender(smimeMessage,
                    currencyServer.getGroupVSSubscriptionServiceURL(simulationData.getGroupId()),
                    currencyServer.getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null, null);
            ResponseVS responseVS = worker.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                throw new org.votingsystem.throwable.ExceptionVS("ERROR nif: " + userNif + " - msg:" + responseVS.getMessage());
            } else simulationData.getAndIncrementNumRequestsOK();
            if((i % 50) == 0) log.info("Subscribed " + i + " of " +
                    simulationData.getUserBaseSimulationData().getNumRequestsProjected() + " users to groupVS");
        }
        log.info("subscribeUser - '" + userList.size() + "' user subscribed");
        return userList;
    }

    public List<SubscriptionVSDto> validateUserVSSubscriptions(Long groupVSId, CurrencyServer currencyServer,
            Map<String, MockDNI> userVSMap) throws Exception {
        log.info("validateUserVSSubscriptions");
        ResultListDto<SubscriptionVSDto> subscriptionVSDtoList = HttpHelper.getInstance().getData(new TypeReference<ResultListDto<SubscriptionVSDto>>(){},
                currencyServer.getGroupVSUsersServiceURL( groupVSId, 1000, 0, SubscriptionVS.State.PENDING, UserVS.State.ACTIVE),
                MediaTypeVS.JSON);
        for(SubscriptionVSDto subscriptionVSDto : subscriptionVSDtoList.getResultList()) {
            if(subscriptionVSDto.getState() == SubscriptionVS.State.PENDING) {
                subscriptionVSDto.loadActivationRequest();
            }
        }
        String messageSubject = "TEST_ACTIVATE_GROUPVS_USERS";
        UserVS userVS = UserVS.getUserVS(certSigner);
        for (SubscriptionVSDto subscriptionVSDto : subscriptionVSDtoList.getResultList()) {
            SMIMEMessage smimeMessage = getSMIMETimeStamped(userVS.getNif(), currencyServer.getName(),
                    JSON.getMapper().writeValueAsString(subscriptionVSDto), messageSubject);
            ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    currencyServer.getGroupVSUsersActivationServiceURL());
            if (ResponseVS.SC_OK != responseVS.getStatusCode()) throw new org.votingsystem.throwable.ExceptionVS(
                    responseVS.getMessage());
        }
        return subscriptionVSDtoList.getResultList();
    }

}