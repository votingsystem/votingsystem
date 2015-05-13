package org.votingsystem.signature.util;


import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.voting.AnonymousDelegationCertExtensionDto;
import org.votingsystem.dto.voting.VoteCertExtensionDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

import javax.mail.Header;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**

 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertificationRequestVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    
    private static Logger log = Logger.getLogger(CertificationRequestVS.class.getSimpleName());

    private transient PKCS10CertificationRequest csr;
    private transient SMIMESignedGeneratorVS SMIMESignedGeneratorVS;
    private transient KeyPair keyPair;
    private String signatureMechanism;
    private X509Certificate certificate;
    private byte[] signedCsr;

    private CertificationRequestVS(KeyPair keyPair, PKCS10CertificationRequest csr, String signatureMechanism) {
        this.keyPair = keyPair;
        this.csr = csr;
        this.signatureMechanism = signatureMechanism;
    }

    public static CertificationRequestVS getVoteRequest(int keySize, String keyName, String signatureMechanism,
            String provider, String accessControlURL, Long eventId, String hashCertVS)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException,
            IOException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal("CN=accessControlURL:" + accessControlURL +", OU=eventId:" + eventId);
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        VoteCertExtensionDto dto = new VoteCertExtensionDto(accessControlURL, hashCertVS, eventId);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.VOTE_TAG,
                new DERUTF8String(JSON.getMapper().writeValueAsString(dto))));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public static CertificationRequestVS getAnonymousDelegationRequest(int keySize, String keyName,
            String signatureMechanism, String provider, String accessControlURL, String hashCertVS,
            Integer weeksOperationActive, Date validFrom, Date validTo) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal("CN=accessControlURL:" + accessControlURL +
                ", OU=AnonymousRepresentativeDelegation");
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        AnonymousDelegationCertExtensionDto dto = new AnonymousDelegationCertExtensionDto(accessControlURL, hashCertVS,
                weeksOperationActive, validFrom, validTo);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG,
                new DERUTF8String(JSON.getMapper().writeValueAsString(dto))));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public static CertificationRequestVS getUserRequest (int keySize, String keyName, String signatureMechanism,
            String provider, CertExtensionDto certExtensionDto) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.DEVICEVS_TAG,
                new DERUTF8String(JSON.getMapper().writeValueAsString(certExtensionDto))));
        X500Principal subject = new X500Principal(certExtensionDto.getPrincipal());
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public static CertificationRequestVS getCurrencyRequest(int keySize, String keyName,
              String signatureMechanism, String provider, String currencyServerURL, String hashCertVS,
              BigDecimal amount, String currencyCode, Boolean timeLimited, String tagVS) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        tagVS = (tagVS == null)? TagVS.WILDTAG:tagVS;
        X500Principal subject = new X500Principal("CN=currencyServerURL:" + currencyServerURL +
                ", OU=CURRENCY_VALUE:" + amount + ", OU=CURRENCY_CODE:" + currencyCode +
                ", OU=TAG:" + tagVS + ", OU=DigitalCurrency");
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        CurrencyCertExtensionDto dto = new CurrencyCertExtensionDto(amount, currencyCode, hashCertVS, currencyServerURL,
                timeLimited, tagVS);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.CURRENCY_TAG,
                new DERUTF8String(JSON.getMapper().writeValueAsString(dto))));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public void initSigner (byte[] signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(signedCsr);
        log.info("initSigner - Num certs: " + certificates.size());
        if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
        certificate = certificates.iterator().next();
        X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
        certificates.toArray(arrayCerts);
        SMIMESignedGeneratorVS = new SMIMESignedGeneratorVS(keyPair.getPrivate(), arrayCerts, signatureMechanism);
        this.setSignedCsr(signedCsr);
    }
    
    public SMIMEMessage getSMIME(String fromUser, String toUser,
            String textToSign, String subject, Header header) throws Exception {
        if (SMIMESignedGeneratorVS == null) SMIMESignedGeneratorVS = new SMIMESignedGeneratorVS(
                keyPair.getPrivate(), Arrays.asList(certificate), signatureMechanism);
        return SMIMESignedGeneratorVS.getSMIME(fromUser, toUser, textToSign, subject);
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
        return CertUtils.getPEMEncoded(csr);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(certificate != null) s.writeObject(certificate.getEncoded());
            else s.writeObject(null);
            if(keyPair != null) {//this is to deserialize private keys outside android environments
                s.writeObject(keyPair.getPublic().getEncoded());
                s.writeObject(keyPair.getPrivate().getEncoded());
            } else {
                s.writeObject(null);
                s.writeObject(null);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException, NoSuchAlgorithmException,
            InvalidKeySpecException {
        s.defaultReadObject();
        byte[] certificateBytes = (byte[]) s.readObject();
        if(certificateBytes != null) {
            try {
                certificate = CertUtils.loadCertificate(certificateBytes);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        try {
            byte[] publicKeyBytes = (byte[]) s.readObject();
            PublicKey publicKey =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            byte[] privateKeyBytes = (byte[]) s.readObject();
            PrivateKey privateKey =  KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            if(privateKey != null && publicKey != null) keyPair = new KeyPair(publicKey, privateKey);
        } catch(Exception ex) {log.log(Level.SEVERE, ex.getMessage(), ex);}
    }

    public byte[] getSignedCsr() {
        return signedCsr;
    }

    public void setSignedCsr(byte[] signedCsr) {
        this.signedCsr = signedCsr;
    }
}