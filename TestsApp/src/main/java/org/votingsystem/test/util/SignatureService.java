package org.votingsystem.test.util;

import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.cms.CMSGenerator;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.Encryptor;
import org.votingsystem.util.crypto.KeyStoreUtil;
import org.votingsystem.util.crypto.PEMUtils;

import javax.security.auth.x500.X500PrivateCredential;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;


public class SignatureService {

    private static Logger log = Logger.getLogger(SignatureService.class.getName());

    private static ConcurrentHashMap<String, SignatureService> signatureServices	= new ConcurrentHashMap<>();

    private CMSGenerator cmsGenerator;
	private X509Certificate x509Certificate;
    private Certificate[] certificateChain;
    private X500PrivateCredential rootCAPrivateCredential;
    private PrivateKey privateKey;
    private Encryptor encryptor;
    private static SignatureService authoritySignatureService;
    private KeyStore keyStore;
    private User user;


    public SignatureService(KeyStore keyStore, String keyAlias, String password) throws Exception {
        init(keyStore, keyAlias, password);
    }

    public synchronized void init(KeyStore keyStore, String keyAlias, String password) throws Exception {
        log.info("init");
        this.keyStore = keyStore;
        certificateChain = keyStore.getCertificateChain(keyAlias);
        cmsGenerator = new CMSGenerator(keyStore, keyAlias, password.toCharArray(),ContextVS.SIGNATURE_ALGORITHM);
        byte[] certificateCollectionPEM = null;
        for (int i = 0; i < certificateChain.length; i++) {
            log.info("Adding local kesystore");
            if(certificateCollectionPEM == null) certificateCollectionPEM = PEMUtils.getPEMEncoded (certificateChain[i]);
            else certificateCollectionPEM = FileUtils.concat(certificateCollectionPEM, PEMUtils.getPEMEncoded (certificateChain[i]));
        }
        x509Certificate = (X509Certificate) keyStore.getCertificate(keyAlias);
        user = User.FROM_X509_CERT(x509Certificate);
        privateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
        rootCAPrivateCredential = new X500PrivateCredential(x509Certificate, privateKey,  "rootAlias");
        encryptor = new Encryptor(x509Certificate, privateKey);
    }

    public static SignatureService getAuthoritySignatureService() throws Exception {
        if(authoritySignatureService != null) return authoritySignatureService;
        String keyStorePath = ContextVS.getInstance().getProperty("authorityKeyStorePath");
        String keyAlias = ContextVS.getInstance().getProperty("userKeyAlias");
        String password = ContextVS.getInstance().getProperty("userKeyPassword");
        KeyStore keyStore = loadKeyStore(keyStorePath, password);
        authoritySignatureService = new SignatureService(keyStore, keyAlias, password);
        signatureServices.put(authoritySignatureService.getUser().getNif(), authoritySignatureService);
        return authoritySignatureService;
    }

    public static SignatureService getUserSignatureService(String nif, User.Type userType) throws Exception {
        SignatureService signatureService = signatureServices.get(nif);
        if(signatureService != null)
                return signatureService;
        String keyStorePath = format("./certs/Cert_{0}_{1}.jks", userType.toString(), nif);
        log.info("loading keystore: " + keyStorePath);
        String keyAlias = ContextVS.getInstance().getProperty("userKeyAlias", "userKeyAlias");
        String password = ContextVS.getInstance().getProperty("userKeyPassword", "userKeyPassword");
        KeyStore keyStore = loadKeyStore(keyStorePath, password);
        signatureService = new SignatureService(keyStore, keyAlias, password);
        signatureServices.put(nif, signatureService);
        return signatureService;
    }

    public static SignatureService load(String nif) throws Exception {
        KeyStore mockDnie = getAuthoritySignatureService().generateKeyStore(nif);
        SignatureService signatureService = new SignatureService(mockDnie, ContextVS.END_ENTITY_ALIAS, ContextVS.PASSWORD);
        signatureServices.put(nif, signatureService);
        return signatureService;
    }

    public User getUser() {
        return user;
    }

    public  KeyStore getKeyStore() {
        return keyStore;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public Certificate[] getCertificateChain() {
        return certificateChain;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
    private X500PrivateCredential getRootCAPrivateCredential() {
        return rootCAPrivateCredential;
    }

    public static KeyStore loadKeyStore(String keyStorePath, String password) throws Exception {
        byte[] keyStoreBytes = ContextVS.getInstance().getResourceBytes(keyStorePath);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new ByteArrayInputStream(keyStoreBytes), password.toCharArray());
        return keyStore;
    }
		
	public CMSSignedMessage signData(byte[] contentToSign) throws Exception {
		return cmsGenerator.signData(contentToSign);
	}

    public CMSSignedMessage signDataWithTimeStamp(byte[] contentToSign) throws Exception {
        TimeStampRequest timeStampRequest = cmsGenerator.getTimeStampRequest(contentToSign);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(timeStampRequest.getEncoded(), ContentType.TIMESTAMP_QUERY,
                ContextVS.getInstance().getTimeStampServiceURL());
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] bytesToken = responseVS.getMessageBytes();
            TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
            return cmsGenerator.signDataWithTimeStamp(contentToSign, timeStampToken);
        } else throw new ExceptionVS(responseVS.getMessage());

    }

	public synchronized CMSSignedMessage addSignature (CMSSignedMessage cmsMessage) throws Exception {
		log.info("addSignature");
		return new CMSSignedMessage(cmsGenerator.addSignature(cmsMessage));
	}

    public byte[] encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        return encryptor.encryptToCMS(dataToEncrypt, receiverCert);
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt) throws Exception {
        return encryptor.encryptToCMS(dataToEncrypt, x509Certificate);
    }

    public byte[] decryptCMS (byte[] encryptedFile) throws Exception {
        return encryptor.decryptCMS(encryptedFile);
    }

    public byte[] encryptToCMS(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        return encryptor.encryptToCMS(bytesToEncrypt, publicKey);
    }

    public KeyStore generateKeyStore(String userNIF) throws Exception {
        Date dateBegin = new Date();
        Date dateFinish = DateUtils.addDays(dateBegin, 365).getTime(); //one year
        KeyStore keyStore = KeyStoreUtil.createUserKeyStore(dateBegin.getTime(), dateFinish.getTime() - dateBegin.getTime(),
                ContextVS.PASSWORD.toCharArray(), ContextVS.END_ENTITY_ALIAS, getRootCAPrivateCredential(),
                "GIVENNAME=FirstName_" + userNIF + " ,SURNAME=lastName_" + userNIF + ", SERIALNUMBER=" + userNIF);
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, ContextVS.PASSWORD.toCharArray());
        String userSubPath = StringUtils.getUserDirPath(userNIF);
        ContextVS.getInstance().copyFile(keyStoreBytes, userSubPath,  "user_" + userNIF + ".jks");
        return keyStore;
    }

}