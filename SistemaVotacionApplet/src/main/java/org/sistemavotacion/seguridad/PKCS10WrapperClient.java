package org.sistemavotacion.seguridad;

import static org.sistemavotacion.Contexto.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMWriter;
import org.sistemavotacion.smime.SignedMailGenerator;
import javax.mail.Header;
import org.sistemavotacion.smime.SignedMailGenerator.Type;

/**
 * 'Client side' PKCS10 wrapper class for the BouncyCastle 
 * {@link PKCS10CertificationRequest} object.
 * @author jgzornoza
 */
public class PKCS10WrapperClient {
    
    private static Logger logger = LoggerFactory.getLogger(PKCS10WrapperClient.class);

    private PKCS10CertificationRequest csr;
    private PrivateKey privateKey;
    private SignedMailGenerator signedMailGenerator;
    private KeyStore keyStore;

    public PKCS10WrapperClient(int keySize, String keyName, 
            String sigName, String provider, String controlAccesoURL, String eventoId,
            String hashCertificadoVotoHEX) throws NoSuchAlgorithmException, 
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyName, provider);
        keyPairGenerator.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        privateKey = keyPair.getPrivate();
        X500Principal subject = new X500Principal(
                "CN=controlAccesoURL:" + controlAccesoURL + 
                ", OU=eventoId:" + eventoId +
                ", OU=hashCertificadoVotoHEX:" + hashCertificadoVotoHEX); 
        csr = new PKCS10CertificationRequest(VOTE_SIGN_MECHANISM, 
                subject, keyPair.getPublic(), null, keyPair.getPrivate(), provider);
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
            logger.error(ex.getMessage(), ex);
            return null;
        } finally {
            try {
                pemWrt.close();
                bOut.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
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
     * @param privateKey the privateKey to set
     */
    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
    
    public void initSigner (byte[] csrFirmada) throws Exception {
        Collection<X509Certificate> certificados = CertUtil.fromPEMChainToX509Certs(csrFirmada);
        logger.debug("Número certificados en cadena: " + certificados.size());
        X509Certificate[] arrayCerts = new X509Certificate[certificados.size()];
        certificados.toArray(arrayCerts);
        signedMailGenerator = new SignedMailGenerator(
                privateKey, arrayCerts,VOTE_SIGN_MECHANISM);
        keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(ALIAS_CLAVES, privateKey, PASSWORD_CLAVES.toCharArray(), arrayCerts);
    }
    //(KeyStore keyStore, String keyAlias, char[] password)
    public void initSigner (KeyStore keyStore) throws Exception {
        this.keyStore = keyStore;
        signedMailGenerator = new SignedMailGenerator(keyStore, ALIAS_CLAVES, 
                PASSWORD_CLAVES.toCharArray(),VOTE_SIGN_MECHANISM);
    }
    
    public String genSignedString(String fromUser, String toUser, String textoAFirmar, 
            String asunto, Header header, Type signerType) throws Exception {
        if (signedMailGenerator == null) 
        	throw new Exception ("signedMailGenerator no inicializado ");
        String result = signedMailGenerator.genString(
                fromUser, toUser, textoAFirmar, asunto, header, signerType);
        return result;
    }
    
    public File genSignedFile(String fromUser, String toUser, String textoAFirmar, 
            String asunto, Header header, Type signerType, File output) throws Exception {
        if (signedMailGenerator == null) 
        	throw new Exception ("signedMailGenerator no inicializado ");
        File result = signedMailGenerator.genFile(fromUser, toUser, 
        		textoAFirmar, asunto, header, signerType, output);
        return result;
    }

    /**
     * @return the keyStore
     */
    public KeyStore getKeyStore() throws Exception {
        if (signedMailGenerator == null) throw new Exception ("signedMailGenerator no inicializado ");
        return keyStore;
    }
}