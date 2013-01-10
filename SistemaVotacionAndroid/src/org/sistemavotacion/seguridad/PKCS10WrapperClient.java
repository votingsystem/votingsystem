package org.sistemavotacion.seguridad;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collection;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.CMSAttributes;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.bouncycastle2.mail.smime.SMIMEException;
import org.bouncycastle2.mail.smime.SMIMESigned;
import org.bouncycastle2.openssl.PEMWriter;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import javax.mail.Header;
import javax.mail.MessagingException;

import org.sistemavotacion.smime.SignedMailGenerator.Type;
import android.util.Log;

/**
 * 'Client side' PKCS10 wrapper class for the BouncyCastle 
 * {@link PKCS10CertificationRequest} object.
 * @author jgzornoza
 */
public class PKCS10WrapperClient {
    
	public static final String TAG = "PKCS10WrapperClient";
	
    /** Signature algorithm for the PKCS#10 request */
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    public static final String SIG_ALGORITHM = "SHA256WithRSAEncryption";
    
    public static final String MSG_FALLO_VERIFICACION = "Fallo en la verificación de CSR";
    
    public static final int KEY_SIZE = 1024;
    public static final String SIG_NAME = "RSA";
    public static final String PROVIDER = "BC";
    public static final String ALIAS_CLAVES = "certificadovoto";
    public static final String PASSWORD_CLAVES = "certificadovoto";
    
    private PKCS10CertificationRequest csr;
    private PrivateKey privateKey;
    private KeyPair keyPair;
    private SignedMailGenerator signedMailGenerator;
    private SignerInformation signerInformation;
    private KeyStore keyStore;
    private File signedFile;

    public PKCS10WrapperClient(int keySize, String keyName, 
            String sigName, String provider, String controlAccesoURL, String eventoId,
            String hashCertificadoVotoHEX) throws NoSuchAlgorithmException, 
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyName, provider);
        keyPairGenerator.initialize(keySize, new SecureRandom());
        keyPair = keyPairGenerator.genKeyPair();
        privateKey = keyPair.getPrivate();
        X500Principal subject = new X500Principal(
                "CN=controlAccesoURL:" + controlAccesoURL + 
                ", OU=eventoId:" + eventoId +
                ", OU=hashCertificadoVotoHEX:" + hashCertificadoVotoHEX); 
        csr = new PKCS10CertificationRequest( SIGNATURE_ALGORITHM, 
                subject, keyPair.getPublic(), null, keyPair.getPrivate(), provider);
    }
    
    public PKCS10WrapperClient(int keySize, String keyName, 
            String sigName, String provider, String nif, String email,
            String telefono, String deviceId) throws NoSuchAlgorithmException, 
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyName, provider);
        keyPairGenerator.initialize(keySize, new SecureRandom());
        keyPair = keyPairGenerator.genKeyPair();
        privateKey = keyPair.getPrivate();
        StringBuilder principal = new StringBuilder("CN=nif:" + nif);
        if (email != null) principal.append(", OU=email:" + email);
        if (telefono != null) principal.append(", OU=telefono:" + telefono);
        if (deviceId != null) principal.append(", OU=deviceId:" + deviceId);
        X500Principal subject = new X500Principal(principal.toString()); 
        csr = new PKCS10CertificationRequest( SIGNATURE_ALGORITHM, 
                subject, keyPair.getPublic(), null, keyPair.getPrivate(), provider);
    }
    
    public static PKCS10WrapperClient buildCSRVoto (int keySize, String keyName, 
            String sigName, String provider, String controlAccesoURL, String eventoId,
            String hashCertificadoVotoHEX) throws NoSuchAlgorithmException, 
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
    	return new PKCS10WrapperClient(keySize, keyName, sigName, 
    			provider, controlAccesoURL, eventoId, hashCertificadoVotoHEX);
    }
    
    public static PKCS10WrapperClient buildCSRUsuario (int keySize, String keyName, 
            String sigName, String provider, String nif, String email,
            String telefono, String deviceId) throws NoSuchAlgorithmException, 
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
    	return new PKCS10WrapperClient(keySize, keyName, sigName, provider,
    			nif, email, telefono, deviceId);
    }
    
    /**
     * @return The DER encoded byte array.
     */
    public byte[] getDEREncodedRequestCSR() {
        return this.csr.getEncoded();
    }

    /**
     * @return The PEM encoded string representation.
     */
    public byte[] getPEMEncodedRequestCSR() {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(bOut));
        try {
            pemWrt.writeObject(csr);
        } catch (IOException ex) {
        	Log.e("getPEMEncodedRequestCSR", ex.getMessage(), ex);
            return null;
        } finally {
            try {
                pemWrt.close();
                bOut.close();
            } catch (IOException ex) {
            	Log.e("getPEMEncodedRequestCSR", ex.getMessage(), ex);
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
    
    /**
     * @param privateKey the privateKey to set
     */
    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
    
    public String getPrivateKeyPEMString () throws IOException {
    	return KeyUtil.getPEMStringFromKey(privateKey);
    }
    
    private KeyStore initVoteSigner (byte[] csrFirmada) throws Exception {
        Collection<X509Certificate> certificados = CertUtil.fromPEMChainToX509Certs(csrFirmada);
    	Log.i("Número certificados en cadena: ", String.valueOf(certificados.size()));
        X509Certificate[] arrayCerts = new X509Certificate[certificados.size()];
        certificados.toArray(arrayCerts);
        signedMailGenerator = new SignedMailGenerator(privateKey, arrayCerts);
        //keyStore = KeyStore.getInstance("JKS");
        keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(ALIAS_CLAVES, privateKey, PASSWORD_CLAVES.toCharArray(), arrayCerts);
        return keyStore;
    }

    public void initSigner (KeyStore keyStore) throws Exception {
        this.keyStore = keyStore;
        signedMailGenerator = new SignedMailGenerator(keyStore, ALIAS_CLAVES, PASSWORD_CLAVES.toCharArray());
    }

    private File genSignedFile(String fromUser, String toUser, String textoAFirmar, 
            String asunto, Header header, Type signerType, byte[] csrFirmada) throws Exception {
        if (signedMailGenerator == null) throw new Exception ("signedMailGenerator no inicializado ");
        signedFile = signedMailGenerator.genFile(
                fromUser, toUser, textoAFirmar, asunto, header, signerType);
        return signedFile;
    }
    
    public TimeStampRequest getTimeStampRequest(String fromUser, String toUser, String textoAFirmar, 
            String asunto, Header header, Type signerType, byte[] csrFirmada) 
            throws FileNotFoundException, IOException, 
    		MessagingException, CMSException, SMIMEException, Exception {
    	File signedFile = genSignedFile(fromUser, toUser, textoAFirmar,asunto, header, signerType, csrFirmada);
 		SMIMESigned solicitudAccesoSMIME = SMIMEMessageWrapper.getSmimeSigned(null,
				new FileInputStream(signedFile), null);
		signerInformation = ((SignerInformation)
				solicitudAccesoSMIME.getSignerInfos().getSigners().iterator().next());
		AttributeTable table = signerInformation.getSignedAttributes();
		Attribute hash = table.get(CMSAttributes.messageDigest);
		ASN1OctetString as = ((ASN1OctetString)hash.getAttrValues().getObjectAt(0));
		//String octects = Base64.encodeToString(as.getOctets(), Base64.DEFAULT);
		//Log.d(TAG + ".obtenerSolicitudAcceso(...)", " - octects: " + octects);
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        return reqgen.generate(TSPAlgorithms.SHA256, as.getOctets());
    }
    
	public File getTimeStampedSignedFile(Attribute attribute) {
		signerInformation.getUnsignedAttributes().add(CMSAttributes.signingTime, attribute);
		return signedFile;
	}
    
    /**
     * @return the keyStore
     */
    public KeyStore getKeyStore() throws Exception {
        if (signedMailGenerator == null) throw new Exception ("signedMailGenerator no inicializado ");
        return keyStore;
    }
    
    public static KeyStore getKeyStore (byte[] csrFirmada, PrivateKey privateKey, 
    		char[] passwordClaves, String aliasClaves) throws Exception {
        Collection<X509Certificate> certificados = CertUtil.fromPEMChainToX509Certs(csrFirmada);
    	Log.i("Número certificados en cadena: ", String.valueOf(certificados.size()));
        X509Certificate[] arrayCerts = new X509Certificate[certificados.size()];
        certificados.toArray(arrayCerts);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(aliasClaves, privateKey, passwordClaves, arrayCerts);
        return keyStore;
    }

    
}