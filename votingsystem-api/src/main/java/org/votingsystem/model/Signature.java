package org.votingsystem.model;

import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.validation.TimestampToken;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="SIGNATURE", uniqueConstraints= @UniqueConstraint(columnNames = {"SIGNATURE_ID", "DOCUMENT_ID"}))
public class Signature implements Serializable {

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;

    @Column(name="SIGNATURE_ID", nullable=false)
    private String signatureId;

    @ManyToOne @JoinColumn(name="DOCUMENT_ID", nullable=false)
    private SignedDocument document;

    //Signatures without signers come from anonymous certificates
    @ManyToOne @JoinColumn(name = "SIGNER_ID")
    private User signer;

    @ManyToOne @JoinColumn(name = "CA_CERTIFICATE_ID")
    private Certificate certificateCA;

    @ManyToOne @JoinColumn(name = "SIGNER_CERTIFICATE_ID")
    private Certificate signerCertificate;

    /**
     * signed timestamp date
     */
    @Column(name="SIGNATURE_DATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime signatureDate;

    @Transient private X509Certificate signingCert;
    @Transient private DigestAlgorithm digestAlgorithm;
    @Transient private EncryptionAlgorithm encryptionAlgorithm;
    @Transient private TimestampToken timestampToken;

    public Signature() {}

    public Signature(User signer, String signatureId, LocalDateTime signatureDate) {
        this.signer = signer;
        this.setSignerCertificate(signer.getCertificate());
        this.signatureDate = signatureDate;
        this.signatureId = signatureId;
    }

    public Signature(User signer, Certificate signerCertificate, Certificate certificateCA, X509Certificate signingCert,
            SignedDocument document, String signatureId, LocalDateTime signatureDate) {
        this.signer = signer;
        this.signerCertificate = signerCertificate;
        this.certificateCA = certificateCA;
        this.signingCert = signingCert;
        this.document = document;
        this.signatureId = signatureId;
        this.signatureDate = signatureDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long signatureId) {
        this.id = signatureId;
    }

    public SignedDocument getDocument() {
        return document;
    }

    public Signature setDocument(SignedDocument documentId) {
        this.document = documentId;
        return this;
    }

    public User getSigner() {
        return signer;
    }

    public Signature setSigner(User signer) {
        this.signer = signer;
        return this;
    }

    public LocalDateTime getSignatureDate() {
        return signatureDate;
    }

    public void setSignatureDate(LocalDateTime signatureDate) {
        this.signatureDate = signatureDate;
    }

    public String getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(String signatureId) {
        this.signatureId = signatureId;
    }

    public X509Certificate getSigningCert() {
        return signingCert;
    }

    public Signature setSigningCert(X509Certificate signingCert) {
        this.signingCert = signingCert;
        return this;
    }

    public DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public Signature setDigestAlgorithm(DigestAlgorithm digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
        return this;
    }

    public EncryptionAlgorithm getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public Signature setEncryptionAlgorithm(EncryptionAlgorithm encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
        return this;
    }

    public TimestampToken getTimestampToken() {
        return timestampToken;
    }

    public Signature setTimestampToken(TimestampToken timestampToken) {
        this.timestampToken = timestampToken;
        return this;
    }

    public Certificate getCertificateCA() {
        return certificateCA;
    }

    public void setCertificateCA(Certificate certificateCA) {
        this.certificateCA = certificateCA;
    }

    public Certificate getSignerCertificate() {
        return signerCertificate;
    }

    public Signature setSignerCertificate(Certificate signerCertificate) {
        this.signerCertificate = signerCertificate;
        return this;
    }

}
