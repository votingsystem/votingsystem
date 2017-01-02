package org.votingsystem.model;

import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author votingsystem
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

    /**
     * signed timestamp date
     */
    @Column(name="SIGNATURE_DATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime signatureDate;


    public Signature() {}

    public Signature(User signer, SignedDocument document, String signatureId, LocalDateTime signatureDate) {
        this.signer = signer;
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

    public void setDocument(SignedDocument documentId) {
        this.document = documentId;
    }

    public User getSigner() {
        return signer;
    }

    public void setSigner(User signer) {
        this.signer = signer;
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

}
