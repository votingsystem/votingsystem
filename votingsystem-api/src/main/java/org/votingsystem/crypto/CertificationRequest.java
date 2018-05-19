package org.votingsystem.crypto;

import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.crypto.cms.CMSGenerator;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.crypto.cms.CMSUtils;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.model.Device;
import org.votingsystem.throwable.CertificateRequestException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.JSON;
import org.votingsystem.xades.XAdESSignature;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertificationRequest implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    
    private static Logger log = Logger.getLogger(CertificationRequest.class.getName());

    private transient PKCS10CertificationRequest csr;
    private transient KeyPair keyPair;
    private String signatureMechanism;
    private X509Certificate certificate;
    private byte[] csrCertificate;

    private CertificationRequest(KeyPair keyPair, PKCS10CertificationRequest csr, String signatureMechanism) {
        this.keyPair = keyPair;
        this.csr = csr;
        this.signatureMechanism = signatureMechanism;
    }

    public static CertificationRequest getVoteRequest(String indentityServiceEntity, String votingServiceEntity,
            String electionUUID, String revocationHash) throws CertificateRequestException {
        try {
            KeyPair keyPair = KeyGenerator.INSTANCE.genKeyPair();
            X500Name subject = new X500Name("CN=identityService:" + indentityServiceEntity +
                    ";votingService:" + votingServiceEntity + ", OU=electionUUID:" + electionUUID);
            CertVoteExtensionDto dto = new CertVoteExtensionDto(indentityServiceEntity, votingServiceEntity,
                    revocationHash, electionUUID);
            PKCS10CertificationRequestBuilder pkcs10Builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
            pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(Constants.VOTE_OID),
                    new DERUTF8String(new JSON().getMapper().writeValueAsString(dto)));
            pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(Constants.ANON_CERT_OID), ASN1Boolean.getInstance(true));
            PKCS10CertificationRequest request = pkcs10Builder.build(new JcaContentSignerBuilder(
                    Constants.SIGNATURE_ALGORITHM).setProvider(Constants.PROVIDER).build(keyPair.getPrivate()));
            return new CertificationRequest(keyPair, request, Constants.SIGNATURE_ALGORITHM);
        } catch (Exception ex) {
            throw new CertificateRequestException(ex.getMessage(), ex);
        }
    }

    public static CertificationRequest getCurrencyRequest(String currencyEntity, String revocationHash,
            BigDecimal amount, CurrencyCode currencyCode) throws CertificateRequestException {
        try {
            KeyPair keyPair = KeyGenerator.INSTANCE.genKeyPair();
            X500Principal subject = new X500Principal("CN=currencyEntity:" + currencyEntity +
                    ", OU=CURRENCY_VALUE:" + amount + ", OU=CURRENCY_CODE:" + currencyCode + ", OU=DigitalCurrency");
            CurrencyCertExtensionDto dto = new CurrencyCertExtensionDto(amount, currencyCode, revocationHash,
                    currencyEntity);
            PKCS10CertificationRequestBuilder pkcs10Builder = new JcaPKCS10CertificationRequestBuilder(subject,
                    keyPair.getPublic());
            pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(Constants.CURRENCY_OID),
                    new DERUTF8String(new JSON().getMapper().writeValueAsString(dto)));
            pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(Constants.ANON_CERT_OID), ASN1Boolean.getInstance(true));
            PKCS10CertificationRequest request = pkcs10Builder.build(new JcaContentSignerBuilder(
                    Constants.SIGNATURE_ALGORITHM).setProvider(Constants.PROVIDER).build(keyPair.getPrivate()));
            return new CertificationRequest(keyPair, request, Constants.SIGNATURE_ALGORITHM);
        } catch (Exception ex) {
            throw new CertificateRequestException(ex.getMessage(), ex);
        }
    }

    public static CertificationRequest getUserRequest(String numId, String email, String phone, String deviceName,
            String deviceUUID, String givenName, String surName, Device.Type deviceType) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException, CertificateRequestException {
        try {
            KeyPair keyPair = KeyGenerator.INSTANCE.genKeyPair();
            X500Principal subject = new X500Principal("SERIALNUMBER=" + numId + ", GIVENNAME=" + givenName + ", SURNAME=" + surName);
            CertExtensionDto dto = new CertExtensionDto(deviceName, deviceUUID,
                    email, phone, deviceType).setNumId(numId).setGivenname(givenName).setSurname(surName);
            PKCS10CertificationRequestBuilder pkcs10Builder = new JcaPKCS10CertificationRequestBuilder(subject,
                    keyPair.getPublic());
            pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(Constants.DEVICE_OID),
                    new DERUTF8String(new JSON().getMapper().writeValueAsString(dto)));
            PKCS10CertificationRequest request = pkcs10Builder.build(new JcaContentSignerBuilder(
                    Constants.SIGNATURE_ALGORITHM).setProvider(Constants.PROVIDER).build(keyPair.getPrivate()));
            return new CertificationRequest(keyPair, request, Constants.SIGNATURE_ALGORITHM);
        } catch (Exception ex) {
            throw new CertificateRequestException(ex.getMessage(), ex);
        }
    }

    public static CertificationRequest getUserRequest (CertExtensionDto certExtensionDto)
            throws CertificateRequestException {
        try {
            KeyPair keyPair = KeyGenerator.INSTANCE.genKeyPair();
            X500Principal subject = new X500Principal(certExtensionDto.getPrincipal());
            PKCS10CertificationRequestBuilder pkcs10Builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
            pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(Constants.DEVICE_OID),
                    new DERUTF8String(new JSON().getMapper().writeValueAsString(certExtensionDto)));
            PKCS10CertificationRequest request = pkcs10Builder.build(new JcaContentSignerBuilder(
                    Constants.SIGNATURE_ALGORITHM).setProvider(Constants.PROVIDER).build(keyPair.getPrivate()));
            return new CertificationRequest(keyPair, request, Constants.SIGNATURE_ALGORITHM);
        } catch (Exception ex) {
            throw new CertificateRequestException(ex.getMessage(), ex);
        }
    }
    
    public byte[] signXAdESWithTimeStamp(byte[] xmlToSign, String timeStampServiceURL) throws Exception {
        Collection<X509Certificate> certificates = PEMUtils.fromPEMToX509CertCollection(csrCertificate);
        X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
        certificates.toArray(arrayCerts);
        return new XAdESSignature().sign(xmlToSign,
                new SignatureTokenConnection(keyPair.getPrivate(), arrayCerts), new TSPHttpSource(timeStampServiceURL));
    }

    public byte[] signPKCS7WithTimeStamp(byte[] contentToSign, String timeStampServiceURL) throws Exception {
        CMSGenerator cmsGenerator = new CMSGenerator(keyPair.getPrivate(), Arrays.asList(certificate), signatureMechanism);
        TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(signatureMechanism, contentToSign, timeStampServiceURL);
        CMSSignedMessage cmsSignedMessage = cmsGenerator.signDataWithTimeStamp(contentToSign, timeStampToken);
        return cmsSignedMessage.toPEM();
    }

    public X509Certificate getCertificate() throws Exception {
        if(certificate == null)
            return PEMUtils.fromPEMToX509Cert(csrCertificate);
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
                certificate = CertificateUtils.loadCertificate(certificateBytes);
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


    public CertificationRequest setSignedCsr(byte[] csrCertificate) {
        this.csrCertificate = csrCertificate;
        return this;
    }

    public String getSignatureMechanism() {
        return signatureMechanism;
    }

    public void setSignatureMechanism(String signatureMechanism) {
        this.signatureMechanism = signatureMechanism;
    }

    public byte[] getCsrCertificate() {
        return csrCertificate;
    }

    public CertificationRequest setCsrCertificate(byte[] csrCertificate) {
        this.csrCertificate = csrCertificate;
        return this;
    }
}