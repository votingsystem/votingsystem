package org.votingsystem.ocsp;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.cert.ocsp.jcajce.JcaBasicOCSPRespBuilder;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.votingsystem.model.Certificate;
import org.votingsystem.util.Constants;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RootCertOCSPInfo {

    private static final Logger log = Logger.getLogger(RootCertOCSPInfo.class.getName());


    private String hashAlgorithmOID = CMSSignedGenerator.DIGEST_SHA1;
    private String keyHash;
    private String nameHash;
    private X509Certificate x509Certificate;
    private X509CertificateHolder certHolder;
    private PrivateKey privateKey;
    private Certificate certificate;

    public RootCertOCSPInfo(Certificate certificate, X509Certificate x509Certificate, PrivateKey privateKey)
            throws Exception {
        this.certificate = certificate;
        this.x509Certificate = x509Certificate;
        this.privateKey = privateKey;
        certHolder = new X509CertificateHolder(this.x509Certificate.getEncoded());
        MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithmOID);
        nameHash = Base64.getEncoder().encodeToString(messageDigest.digest(
                certHolder.toASN1Structure().getSubject().getEncoded(ASN1Encoding.DER)));
        SubjectPublicKeyInfo info = certHolder.getSubjectPublicKeyInfo();
        keyHash = Base64.getEncoder().encodeToString(messageDigest.digest(info.getPublicKeyData().getBytes()));
    }

    public String getHashAlgorithmOID() {
        return hashAlgorithmOID;
    }

    public void setHashAlgorithmOID(String hashAlgorithmOID) {
        this.hashAlgorithmOID = hashAlgorithmOID;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getNameHash() {
        return nameHash;
    }

    public void setNameHash(String nameHash) {
        this.nameHash = nameHash;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }


    public X509CertificateHolder getCertHolder() {
        return certHolder;
    }

    public void setCertHolder(X509CertificateHolder certHolder) {
        this.certHolder = certHolder;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public byte[] generateOCSPResponse(Set<OCSPResponseData> responses)
            throws OperatorCreationException, OCSPException, IOException {
        DigestCalculatorProvider digCalcProv = new JcaDigestCalculatorProviderBuilder()
                .setProvider(Constants.PROVIDER).build();
        BasicOCSPRespBuilder respGen = new JcaBasicOCSPRespBuilder(x509Certificate.getPublicKey(),
                digCalcProv.get(RespID.HASH_SHA1));
        for(OCSPResponseData responseData : responses) {
            respGen.addResponse(responseData.certId, responseData.certificateStatus);
        }
        X509CertificateHolder[] chain = new X509CertificateHolder[]{certHolder};
        BasicOCSPResp resp = respGen.build(new JcaContentSignerBuilder("SHA1withRSA").setProvider(Constants.PROVIDER)
                .build(privateKey), chain, new Date());
        return new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, resp).getEncoded();
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public static class OCSPResponseData {

        private CertificateID certId;
        private CertificateStatus certificateStatus;

        public OCSPResponseData(CertificateID certId, CertificateStatus certificateStatus) {
            this.certId = certId;
            this.certificateStatus = certificateStatus;
        }

        public CertificateID getCertId() {
            return certId;
        }

        public CertificateStatus getCertificateStatus() {
            return certificateStatus;
        }
    }
}
