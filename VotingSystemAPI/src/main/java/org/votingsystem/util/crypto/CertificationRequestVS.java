package org.votingsystem.util.crypto;


import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.votingsystem.cms.CMSGenerator;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.voting.AnonymousDelegationCertExtensionDto;
import org.votingsystem.dto.voting.VoteCertExtensionDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

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
    
    private static Logger log = Logger.getLogger(CertificationRequestVS.class.getName());

    private transient PKCS10CertificationRequest csr;
    private transient CMSGenerator cmsGenerator;
    private transient KeyPair keyPair;
    private String signatureMechanism;
    private X509Certificate certificate;
    private byte[] signedCsr;

    private CertificationRequestVS(KeyPair keyPair, PKCS10CertificationRequest csr, String signatureMechanism) {
        this.keyPair = keyPair;
        this.csr = csr;
        this.signatureMechanism = signatureMechanism;
    }

    public static CertificationRequestVS getVoteRequest(String signatureMechanism,
            String provider, String accessControlURL, Long eventId, String hashCertVS)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException,
            IOException, OperatorCreationException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        X500Name subject = new X500Name("CN=accessControlURL:" + accessControlURL +", OU=eventId:" + eventId);
        VoteCertExtensionDto dto = new VoteCertExtensionDto(accessControlURL, hashCertVS, eventId);
        PKCS10CertificationRequestBuilder pkcs10Builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(ContextVS.VOTE_OID),
                new DERUTF8String(JSON.getMapper().writeValueAsString(dto)));
        pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(ContextVS.ANONYMOUS_CERT_OID), ASN1Boolean.getInstance(true));
        PKCS10CertificationRequest request = pkcs10Builder.build(new JcaContentSignerBuilder(
                signatureMechanism).setProvider(provider).build(keyPair.getPrivate()));
        return new CertificationRequestVS(keyPair, request, signatureMechanism);
    }

    public static CertificationRequestVS getAnonymousDelegationRequest(
            String signatureMechanism, String provider, String accessControlURL, String hashCertVS,
            Integer weeksOperationActive, Date validFrom, Date validTo) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException, OperatorCreationException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal("CN=accessControlURL:" + accessControlURL +
                ", OU=AnonymousRepresentativeDelegation");
        AnonymousDelegationCertExtensionDto dto = new AnonymousDelegationCertExtensionDto(accessControlURL, hashCertVS,
                weeksOperationActive, validFrom, validTo);
        PKCS10CertificationRequestBuilder pkcs10Builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_OID),
                new DERUTF8String(JSON.getMapper().writeValueAsString(dto)));
        pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(ContextVS.ANONYMOUS_CERT_OID), ASN1Boolean.getInstance(true));
        PKCS10CertificationRequest request = pkcs10Builder.build(new JcaContentSignerBuilder(
                signatureMechanism).setProvider(provider).build(keyPair.getPrivate()));
        return new CertificationRequestVS(keyPair, request, signatureMechanism);
    }

    public static CertificationRequestVS getUserRequest (String signatureMechanism,
            String provider, CertExtensionDto certExtensionDto) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException, OperatorCreationException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal(certExtensionDto.getPrincipal());
        PKCS10CertificationRequestBuilder pkcs10Builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(ContextVS.DEVICEVS_OID),
                new DERUTF8String(JSON.getMapper().writeValueAsString(certExtensionDto)));
        PKCS10CertificationRequest request = pkcs10Builder.build(new JcaContentSignerBuilder(
                signatureMechanism).setProvider(provider).build(keyPair.getPrivate()));
        return new CertificationRequestVS(keyPair, request, signatureMechanism);
    }

    public static CertificationRequestVS getCurrencyRequest(
              String signatureMechanism, String provider, String currencyServerURL, String hashCertVS,
              BigDecimal amount, String currencyCode, Boolean timeLimited, String tagVS) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException, OperatorCreationException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        tagVS = (tagVS == null)? TagVS.WILDTAG:tagVS.trim();
        X500Principal subject = new X500Principal("CN=currencyServerURL:" + currencyServerURL +
                ", OU=CURRENCY_VALUE:" + amount + ", OU=CURRENCY_CODE:" + currencyCode +
                ", OU=TAG:" + tagVS + ", OU=DigitalCurrency");
        CurrencyCertExtensionDto dto = new CurrencyCertExtensionDto(amount, currencyCode, hashCertVS, currencyServerURL,
                timeLimited, tagVS);
        PKCS10CertificationRequestBuilder pkcs10Builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(ContextVS.CURRENCY_OID),
                new DERUTF8String(JSON.getMapper().writeValueAsString(dto)));
        pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(ContextVS.ANONYMOUS_CERT_OID), ASN1Boolean.getInstance(true));
        PKCS10CertificationRequest request = pkcs10Builder.build(new JcaContentSignerBuilder(
                signatureMechanism).setProvider(provider).build(keyPair.getPrivate()));
        return new CertificationRequestVS(keyPair, request, signatureMechanism);
    }

    public void initSigner (byte[] signedCsr) throws Exception {
        Collection<X509Certificate> certificates = PEMUtils.fromPEMToX509CertCollection(signedCsr);
        log.info("initSigner - Num certs: " + certificates.size());
        if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
        certificate = certificates.iterator().next();
        X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
        certificates.toArray(arrayCerts);
        cmsGenerator = new CMSGenerator(keyPair.getPrivate(), arrayCerts, signatureMechanism);
        this.setSignedCsr(signedCsr);
    }
    
    public CMSSignedMessage signData(String textToSign) throws Exception {
        if (cmsGenerator == null) cmsGenerator = new CMSGenerator(
                keyPair.getPrivate(), Arrays.asList(certificate), signatureMechanism);
        return cmsGenerator.signData(textToSign);
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

    public byte[] getCsrPEM() throws Exception {
        return PEMUtils.getPEMEncoded(csr);
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