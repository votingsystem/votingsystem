package org.votingsystem.model;

import eu.europa.esig.dss.DSSDocument;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.metadata.MetaInfDto;
import org.votingsystem.model.converter.MetaInfConverter;
import org.votingsystem.util.Constants;
import org.votingsystem.xml.XML;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="SIGNED_DOCUMENT")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn( name="DSS_DOCUMENT_TYPE", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("SIGNED_DOCUMENT")
@NamedQueries({
        @NamedQuery(name = SignedDocument.FIND_BY_MESSAGE_DIGEST, query =
                "SELECT s FROM SignedDocument s WHERE s.messageDigest =:messageDigest")
})
public class SignedDocument extends EntityBase implements Serializable {

    private static Logger log = Logger.getLogger(SignedDocument.class.getName());

    private static final long serialVersionUID = 1L;

    public static final String FIND_BY_MESSAGE_DIGEST = "SignedDocument.FIND_BY_MESSAGE_DIGEST";


    public enum Indication {TOTAL_PASSED(false), LOCAL_SIGNATURE(false), ERROR_ZERO_SIGNATURES(true), ERROR_SIGNATURES_COUNT(true),
            ERROR_SIGNER(true), ERROR_TIMESTAMP(true), ERROR(true), VALIDATION_ERROR(true);
        private boolean isError;
        Indication(boolean isError) {
            this.isError = isError;
        }

        public boolean isError() {
            return isError;
        }
    }

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name="DOCUMENT_TYPE", nullable=false)
    private SignedDocumentType signedDocumentType;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="RECEIPT_ID")
    private SignedDocument receipt;

    @Enumerated(EnumType.STRING)
    @Column(name="INDICATION", nullable=false)
    private Indication indication;

    @Column(name="META_INF", columnDefinition="TEXT")
    @Convert(converter = MetaInfConverter.class)
    private MetaInfDto metaInf;

    @Column(name="BODY", columnDefinition="TEXT")  private String body;

    //To avoid repeated messages
    @Column(name="MESSAGE_DIGEST", unique=true)
    private String messageDigest;


    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="document")
    private Set<Signature> signatures;
    @Transient private User anonSigner;


    public SignedDocument() {}

    public SignedDocument(DSSDocument signedDocument, SignedDocumentType signedDocumentType) throws IOException {
        this.signedDocumentType = signedDocumentType;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        signedDocument.writeTo(baos);
        body = new String(baos.toByteArray(), "UTF-8");
        baos.close();
    }

    public SignedDocument(DSSDocument signedDocument, SignedDocumentType signedDocumentType, String messageDigest)
            throws IOException {
        this(signedDocument, signedDocumentType);
        this.messageDigest = messageDigest;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

	public MetaInfDto getMetaInf() { return metaInf; }

	public SignedDocument setMetaInf(MetaInfDto metaInf) {
		this.metaInf = metaInf;
        return this;
	}

	public String getMessageDigest() {
		return messageDigest;
	}

	public void setMessageDigest(String base64ContentDigest) {
		this.messageDigest = base64ContentDigest;
	}

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public SignedDocumentType getSignedDocumentType() {
        return signedDocumentType;
    }

    public SignedDocument setSignedDocumentType(SignedDocumentType signedDocumentType) {
        this.signedDocumentType = signedDocumentType;
        return this;
    }

    public Indication getIndication() {
        return indication;
    }

    public SignedDocument setIndication(Indication indication) {
        this.indication = indication;
        return this;
    }

    public User getAnonSigner() {
        return anonSigner;
    }

    public SignedDocument setAnonSigner(User anonSigner) {
        this.anonSigner = anonSigner;
        return this;
    }

    public SignedDocument getReceipt() {
        return receipt;
    }

    public SignedDocument setReceipt(SignedDocument receipt) {
        this.receipt = receipt;
        return this;
    }

    public Set<Signature> getSignatures() {
        return signatures;
    }

    public SignedDocument setSignatures(Set<Signature> signatures) {
        this.signatures = signatures;
        return this;
    }

    @Override
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    @Override
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public Signature getFirstSignature() {
        Signature result = null;
        for(Signature signature : signatures) {
            if(result == null)
                result = signature;
            else if(result.getSignatureDate() != null)
                result = result.getSignatureDate().isBefore(signature.getSignatureDate()) ? result : signature;
        }
        return result;
    }

    public <T> T getSignedContent(Class<T> type) throws Exception {
        return XML.getMapper().readValue(body, type);
    }

    public X509Certificate getCurrencyCert() {
        return getAnonCert(Constants.CURRENCY_OID);
    }

    public X509Certificate getVoteCert() {
        return getAnonCert(Constants.VOTE_OID);
    }

    public X509Certificate getAnonCert(String certExtensionOID) {
        for(Signature signature : signatures) {
            if(signature.getSigningCert().getExtensionValue(certExtensionOID) != null)
                return signature.getSigningCert();
        }
        return null;
    }

}