package org.votingsystem.test.pkcs7;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.crypto.Encryptor;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.cms.CMSGenerator;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.FileUtils;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class CMSSignatureBuilder {

    private static Logger log = Logger.getLogger(CMSSignatureBuilder.class.getName());

    private static ConcurrentHashMap<String, CMSSignatureBuilder> signatureServices	= new ConcurrentHashMap<>();

    private CMSGenerator cmsGenerator;
	private X509Certificate x509Certificate;
    private Certificate[] certificateChain;
    private PrivateKey privateKey;
    private Encryptor encryptor;
    private KeyStore keyStore;
    private User user;


    public CMSSignatureBuilder(KeyStore keyStore, String keyAlias, String password) throws Exception {
        init(keyStore, keyAlias, password);
    }

    public CMSSignatureBuilder(MockDNIe mockDNIe) throws Exception {
        this.keyStore = mockDNIe.getKeyStore();
        this.certificateChain = mockDNIe.getCertificateChain();
        this.x509Certificate = (X509Certificate) keyStore.getCertificate(mockDNIe.getKeyAlias());
        this.cmsGenerator = new CMSGenerator(keyStore, mockDNIe.getKeyAlias(), mockDNIe.getPassword().toCharArray(),
                Constants.SIGNATURE_ALGORITHM);
        this.privateKey = (PrivateKey)keyStore.getKey(mockDNIe.getKeyAlias(), mockDNIe.getPassword().toCharArray());
    }

    public void init(KeyStore keyStore, String keyAlias, String password) throws Exception {
        log.info("init");
        this.keyStore = keyStore;
        certificateChain = keyStore.getCertificateChain(keyAlias);
        cmsGenerator = new CMSGenerator(keyStore, keyAlias, password.toCharArray(), Constants.SIGNATURE_ALGORITHM);
        byte[] certificateCollectionPEM = null;
        for (int i = 0; i < certificateChain.length; i++) {
            log.info("Adding local kesystore");
            if(certificateCollectionPEM == null)
                certificateCollectionPEM = PEMUtils.getPEMEncoded (certificateChain[i]);
            else certificateCollectionPEM = FileUtils.concat(certificateCollectionPEM, PEMUtils.getPEMEncoded (certificateChain[i]));
        }
        x509Certificate = (X509Certificate) keyStore.getCertificate(keyAlias);
        user = User.FROM_CERT(x509Certificate, User.Type.USER);
        privateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
        encryptor = new Encryptor(x509Certificate, privateKey);
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
		
	public CMSSignedMessage signData(byte[] contentToSign) throws Exception {
		return cmsGenerator.signData(contentToSign);
	}

    public CMSSignedMessage signDataWithTimeStamp(byte[] contentToSign) throws Exception {
        TimeStampRequest timeStampRequest = cmsGenerator.getTimeStampRequest(contentToSign);
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(timeStampRequest.getEncoded(),
                MediaType.TIMESTAMP_QUERY, org.votingsystem.test.Constants.TIMESTAMP_SERVICE_URL);
        if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
            byte[] bytesToken = responseDto.getMessageBytes();
            TimeStampResponse timeStampResponse = new TimeStampResponse(bytesToken);
            TimeStampToken timeStampToken = timeStampResponse.getTimeStampToken();
            return cmsGenerator.signDataWithTimeStamp(contentToSign, timeStampToken);
        } else throw new ValidationException(responseDto.getMessage());
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

}