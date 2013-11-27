package org.votingsystem.signature.util;

import android.util.Log;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.bouncycastle2.openssl.PEMWriter;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;

import javax.mail.Header;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PKCS10WrapperClient {
    
	public static final String TAG = "PKCS10WrapperClient";
    
    public static final String ALIAS_CLAVES = "certificadovoto";
    public static final String PASSWORD_CLAVES = "certificadovoto";
    
    private PKCS10CertificationRequest csr;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private X509Certificate certificate;
    private KeyPair keyPair;
    private SignedMailGenerator signedMailGenerator;
    private KeyStore keyStore;
    private String signatureAlgorithm;

    public PKCS10WrapperClient(int keySize, String keyName, String signatureAlgorithm, 
    		String provider, String accessControlURL, String eventId,
            String hashCertVoteHex) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyName, provider);
        keyPairGenerator.initialize(keySize, new SecureRandom());
        keyPair = keyPairGenerator.genKeyPair();
        privateKey = keyPair.getPrivate();
        this.signatureAlgorithm = signatureAlgorithm;
        publicKey = keyPair.getPublic();
        X500Principal subject = new X500Principal(
                "CN=accessControlURL:" + accessControlURL + 
                ", OU=eventId:" + eventId +
                ", OU=hashCertVoteHex:" + hashCertVoteHex);
        csr = new PKCS10CertificationRequest(signatureAlgorithm, subject, 
        		keyPair.getPublic(), null, keyPair.getPrivate(), provider);
    }
    
    public PKCS10WrapperClient(int keySize, String keyName, 
            String signatureAlgorithm, String provider, String nif, String email,
            String phone, String deviceId, String givenName, String surName)
            		throws NoSuchAlgorithmException, 
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyName, provider);
        keyPairGenerator.initialize(keySize, new SecureRandom());
        keyPair = keyPairGenerator.genKeyPair();
        privateKey = keyPair.getPrivate();
        this.signatureAlgorithm = signatureAlgorithm;
        String principal = "SERIALNUMBER=" + nif + 
        		", OU=deviceId:" + deviceId + ", GIVENNAME=" + givenName + 
        		", SURNAME=" + surName;
        if (email != null) principal.concat(", OU=email:" + email);
        if (phone != null) principal.concat(", OU=phone:" + phone);
        X500Principal subject = new X500Principal(principal.toString()); 
        csr = new PKCS10CertificationRequest(signatureAlgorithm, subject, 
        		keyPair.getPublic(), null, keyPair.getPrivate(), provider);
    }

    public static PKCS10WrapperClient buildCSRVoto (int keySize, String keyName, 
            String sigName, String provider, String accessControlURL, String eventId,
            String hashCertVoteHex) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
    	return new PKCS10WrapperClient(keySize, keyName, sigName, 
    			provider, accessControlURL, eventId, hashCertVoteHex);
    }
    
    public static PKCS10WrapperClient buildCSRUsuario (int keySize, String keyName, 
            String sigName, String provider, String nif, String email,
            String phone, String deviceId, String givenName, String surName) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
    	return new PKCS10WrapperClient(keySize, keyName, sigName, provider,
    			nif, email, phone, deviceId, givenName, surName);
    }
    
    /**
     * @return The DER encoded byte array.
     */
    public byte[] getCsrDER() {
        return this.csr.getEncoded();
    }

    /**
     * @return The PEM encoded string representation.
     */
    public byte[] getCsrPEM() {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(bOut));
        try {
            pemWrt.writeObject(csr);
        } catch (IOException ex) {
        	Log.e("getCsrPEM", ex.getMessage(), ex);
            return null;
        } finally {
            try {
                pemWrt.close();
                bOut.close();
            } catch (IOException ex) {
            	Log.e("getCsrPEM", ex.getMessage(), ex);
            }
        }
        return bOut.toByteArray();
    }

    /**
     * @return the privateKey
     */
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * @return the privateKey
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }
    
    public String getPrivateKeyPEMString () throws IOException {
    	return KeyUtil.getPEMStringFromKey(privateKey);
    }
    
    public void initSigner (byte[] csrFirmada) throws Exception {
        Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(csrFirmada);
        certificate = certificates.iterator().next();
        X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
        certificates.toArray(arrayCerts);
        signedMailGenerator = new SignedMailGenerator(
                privateKey, arrayCerts, signatureAlgorithm);
    }


    public SMIMEMessageWrapper genSignedMessage(String fromUser, String toUser, 
    		String textToSign, String subject, Header header) throws Exception {
        if (signedMailGenerator == null) 
        	throw new Exception ("signedMailGenerator no inicializado ");
        return signedMailGenerator.genMimeMessage(
        		fromUser, toUser, textToSign, subject, header);
    }
    
    /**
     * @return the keyStore
     */
    public KeyStore getKeyStore() throws Exception {
        if (signedMailGenerator == null) 
        	throw new Exception ("signedMailGenerator no inicializado ");
        return keyStore;
    }
    
    public static KeyStore getKeyStore (byte[] csrFirmada, PrivateKey privateKey, 
    		char[] passwordClaves, String aliasClaves) throws Exception {
        Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(csrFirmada);
    	Log.i("Número certificates en cadena: ", String.valueOf(certificates.size()));
        X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
        certificates.toArray(arrayCerts);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(aliasClaves, privateKey, passwordClaves, arrayCerts);
        return keyStore;
    }


    /**
     * @return the publicKey
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }


    /**
     * @return the certificate
     */
    public X509Certificate getCertificate() {
        return certificate;
    }

}