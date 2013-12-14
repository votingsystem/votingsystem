package org.votingsystem.signature.util;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import javax.mail.Header;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Collection;


/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class PKCS10WrapperClient {
    
    private static Logger logger = Logger.getLogger(PKCS10WrapperClient.class);

    private PKCS10CertificationRequest csr;
    private KeyPair keyPair;
    private String signatureMechanism;
    private X509Certificate certificate;
    private SignedMailGenerator signedMailGenerator;

    public PKCS10WrapperClient(int keySize, String keyName, String signatureMechanism, String provider,
            String accessControlURL, String eventId, String hashCertVoteHex) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        keyPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        this.signatureMechanism = signatureMechanism;
        X500Principal subject = new X500Principal("CN=accessControlURL:" + accessControlURL +", OU=eventId:" + eventId);
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        /* 0 -> accessControlURL
         * 1 -> eventId
         * 2 -> hashCertVoteHex */
        asn1EncodableVector.add(new DERUTF8String(accessControlURL));
        asn1EncodableVector.add(new DERUTF8String(eventId));
        asn1EncodableVector.add(new DERUTF8String(hashCertVoteHex));
        csr = new PKCS10CertificationRequest(signatureMechanism, subject, keyPair.getPublic(),
                new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
    }
    
    public void initSigner (byte[] signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(signedCsr);
        logger.debug("initSigner - Num certs: " + certificates.size());
        if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
        certificate = certificates.iterator().next();
        X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
        certificates.toArray(arrayCerts);
        signedMailGenerator = new SignedMailGenerator(keyPair.getPrivate(), arrayCerts, signatureMechanism);
    }
    
    public SMIMEMessageWrapper genMimeMessage(String fromUser, String toUser, 
            String textToSign, String subject, Header header) throws Exception {
        if (signedMailGenerator == null) throw new Exception (" --- SignedMailGenerator null --- ");
        return signedMailGenerator.genMimeMessage(fromUser, toUser, textToSign, subject);
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() throws Exception {
        return keyPair.getPrivate();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public byte[] getCsrDER() {
        return csr.getEncoded();
    }

    public byte[] getCsrPEM() throws Exception {
        return CertUtil.getPEMEncoded(csr);
    }

}