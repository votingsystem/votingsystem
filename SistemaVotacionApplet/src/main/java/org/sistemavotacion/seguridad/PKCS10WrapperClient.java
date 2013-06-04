package org.sistemavotacion.seguridad;

import static org.sistemavotacion.Contexto.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import javax.security.auth.x500.X500Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMWriter;
import org.sistemavotacion.smime.SignedMailGenerator;
import javax.mail.Header;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.VotingSystemKeyGenerator;

/**
 * 'Client side' PKCS10 wrapper class for the BouncyCastle 
 * {@link PKCS10CertificationRequest} object.
 * @author jgzornoza
 */
public class PKCS10WrapperClient {
    
    private static Logger logger = LoggerFactory.getLogger(PKCS10WrapperClient.class);

    private PKCS10CertificationRequest csr;
    private KeyPair keyPair;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private X509Certificate certificate;
    private SignedMailGenerator signedMailGenerator;

    public PKCS10WrapperClient(int keySize, String keyName,
            String sigName, String provider, String controlAccesoURL, String eventoId,
            String hashCertificadoVotoHEX) throws NoSuchAlgorithmException, 
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        keyPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
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
        return csr.getEncoded();
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
    public PrivateKey getPrivateKey() throws Exception {
        return privateKey;
    }
    
    public void initSigner (byte[] csrFirmada) throws Exception {
        Collection<X509Certificate> certificados = 
                CertUtil.fromPEMToX509CertCollection(csrFirmada);
        logger.debug("initSigner - Num certs: " + certificados.size());
        if(certificados.isEmpty()) return;
        certificate = certificados.iterator().next();
        X509Certificate[] arrayCerts = new X509Certificate[certificados.size()];
        certificados.toArray(arrayCerts);
        signedMailGenerator = new SignedMailGenerator(
                privateKey, arrayCerts, VOTE_SIGN_MECHANISM);
    }
    
    public SMIMEMessageWrapper genMimeMessage(String fromUser, String toUser, 
            String textoAFirmar, String asunto, Header header) throws Exception {
        if (signedMailGenerator == null) 
        	throw new Exception ("signedMailGenerator null");
        return signedMailGenerator.genMimeMessage(
                fromUser, toUser, textoAFirmar, asunto, header);
    }

    /**
     * @return the certificate
     */
    public X509Certificate getCertificate() {
        return certificate;
    }

    /**
     * @return the publicKey
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * @return the keyPair
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }

    /**
     * @param keyPair the keyPair to set
     */
    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

}