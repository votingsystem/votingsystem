package org.votingsystem.model.currency;

import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.crypto.CertificationRequest;
import org.votingsystem.crypto.HashUtils;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.currency.CurrencyUtils;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.EntityBase;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.throwable.CertificateRequestException;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.DateUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="CURRENCY")
public class Currency extends EntityBase implements Serializable  {

    public static final long serialVersionUID = 1L;

    public CMSSignedMessage getCmsSignedMessage() {
        return cmsSignedMessage;
    }

    public void setCmsSignedMessage(CMSSignedMessage cmsSignedMessage) {
        this.cmsSignedMessage = cmsSignedMessage;
    }

    //Lapsed -> for not expended time limited currency
    public enum State { OK, EXPENDED, LAPSED, UNKNOWN, ERROR_CERT_AUTHORITY, ERROR;}

    public enum Type { LEFT_OVER, CHANGE, REQUEST }

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;
    
    @Column(name="SUBJECT") 
    private String subject;
    
    @Column(name="AMOUNT") 
    private BigDecimal amount = null;
    
    @Column(name="CURRENCY", nullable=false) @Enumerated(EnumType.STRING)
    private CurrencyCode currencyCode;

    @Column(name="UUID") private String UUID;

    @Column(name="REVOCATION_HASH")
    private String revocationHash;
    
    @Column(name="ORIGIN_REVOCATION_HASH") 
    private String originRevocationHash;
    
    @Column(name="CURRENCY_ENTITY")
    private String currencyEntity;

    @Column(name="REASON")
    private String reason;

    @Column(name="META_INF") 
    private String metaInf;
    
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="CURRENCY_BATCH") 
    private CurrencyBatch currencyBatch;

    @Column(name="SERIAL_NUMBER", unique=true, nullable=false) 
    private Long serialNumber;
    
    @Column(name="CONTENT", nullable=false) 
    private byte[] content;
    
    @Column(name="STATE", nullable=false) @Enumerated(EnumType.STRING) 
    private State state;
    
    @Column(name="TYPE", nullable=false) @Enumerated(EnumType.STRING) 
    private Type type;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="TO_USER") 
    private User toUser;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="AUTHORITY_CERTIFICATE") 
    private Certificate authorityCertificate;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="TRANSACTION")
    private Transaction transaction;

    @OneToOne @JoinColumn(name="SIGNED_DOCUMENT_ID")
    private SignedDocument signedDocument;

    @Column(name="VALID_FROM", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime validFrom;

    @Column(name="VALID_TO", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime validTo;

    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;

    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    @Transient private CertificationRequest certificationRequest;
    @Transient private X509Certificate currencyCertificate;
    @Transient private String toUserIBAN;
    @Transient private String toUserName;
    @Transient private CurrencyDto batchItemDto;
    @Transient private CMSSignedMessage cmsSignedMessage;
    @Transient private CurrencyCertExtensionDto certExtensionDto;

    public Currency() {}

    public Currency(CMSSignedMessage cmsSignedMessage) throws Exception {
        cmsSignedMessage.checkSignatureInfo();
        initCertData(cmsSignedMessage.getCurrencyCert());
        batchItemDto = cmsSignedMessage.getSignedContent(CurrencyDto.class);
        CurrencyUtils.checkDto(this, batchItemDto);
        this.subject = batchItemDto.getSubject();
        this.toUserIBAN = batchItemDto.getToUserIBAN();
        this.toUserName = batchItemDto.getToUserName();
    }

    public Currency(String currencyEntity, BigDecimal amount, CurrencyCode currencyCode,
            String revocationHash, String originRevocationHash) throws NoSuchAlgorithmException,
            CertificateRequestException  {
        this.amount = amount;
        this.currencyEntity = currencyEntity;
        this.currencyCode = currencyCode;
        this.revocationHash = revocationHash;
        this.originRevocationHash = originRevocationHash;
        certificationRequest = CertificationRequest.getCurrencyRequest(currencyEntity, revocationHash, amount, currencyCode);
    }

    public Currency initCertData(X509Certificate currencyCertificate) throws Exception {
        this.currencyCertificate = currencyCertificate;
        content = currencyCertificate.getEncoded();
        UUID = CertificateUtils.getHash(currencyCertificate);
        serialNumber = currencyCertificate.getSerialNumber().longValue();
        certExtensionDto = CertificateUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                currencyCertificate, Constants.CURRENCY_OID);
        if(certExtensionDto == null)
            throw new ValidationException("error missing currency cert extension data");
        amount = certExtensionDto.getAmount();
        currencyCode = certExtensionDto.getCurrencyCode();
        revocationHash = certExtensionDto.getRevocationHash();
        currencyEntity = certExtensionDto.getCurrencyEntity();
        validFrom = DateUtils.getLocalDateFromUTCDate(currencyCertificate.getNotBefore());
        validTo = DateUtils.getLocalDateFromUTCDate(currencyCertificate.getNotAfter());
        String subjectDN = currencyCertificate.getSubjectDN().toString();
        CurrencyDto.CertSubjectDto certSubject = CurrencyDto.getCertSubjectDto(subjectDN);
        if(!certSubject.getCurrencyEntity().equals(certExtensionDto.getCurrencyEntity()))
            throw new ValidationException("currencyEntity: " + currencyEntity + " - certSubject: " + subjectDN);
        if(certSubject.getAmount().compareTo(amount) != 0)
            throw new ValidationException("amount: " + amount + " - certSubject: " + subjectDN);
        if(!certSubject.getCurrencyCode().equals(currencyCode))
            throw new ValidationException("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        return this;
    }

    public CurrencyDto getBatchItemDto() {
        return batchItemDto;
    }

    public void setBatchItemDto(CurrencyDto batchItemDto) {
        this.batchItemDto = batchItemDto;
    }

    public CurrencyBatch getCurrencyBatch() {
        return currencyBatch;
    }

    public Currency setCurrencyBatch(CurrencyBatch currencyBatch) {
        this.currencyBatch = currencyBatch;
        return this;
    }

    public Type getType() {
        return type;
    }

    public Currency setType(Type type) {
        this.type = type;
        return this;
    }

    public void setCurrencyCertificate(X509Certificate currencyCertificate) {
        this.currencyCertificate = currencyCertificate;
    }

    private String getErrorPrefix() {
        return "ERROR - Currency with revocation hash: " + revocationHash + " - ";
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void initSigner(byte[] csrBytes) throws Exception {
        certificationRequest.setSignedCsr(csrBytes);
        initCertData(certificationRequest.getCertificate());
    }

    public static Currency fromCertificationRequest(CertificationRequest certificationRequest) throws Exception {
        Currency currency = new Currency();
        currency.setCertificationRequest(certificationRequest);
        currency.initSigner(certificationRequest.getCsrCertificate());
        return currency;
    }

    public CurrencyCertExtensionDto getCertExtensionDto() {
        return certExtensionDto;
    }

    public void setCertExtensionDto(CurrencyCertExtensionDto certExtensionDto) {
        this.certExtensionDto = certExtensionDto;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public String getToUserName() {
        return toUserName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public X509Certificate getCurrencyCertificate() {
        return currencyCertificate;
    }

    public String getMetaInf() {
        return metaInf;
    }

    public void setMetaInf(String metaInf) {
        this.metaInf = metaInf;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyEntity() {
        return currencyEntity;
    }

    public void setCurrencyEntity(String currencyServerEntityId) {
        this.currencyEntity = currencyServerEntityId;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public void setOriginRevocationHash(String originRevocationHash) {
        this.originRevocationHash = originRevocationHash;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public void setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
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
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Certificate getAuthorityCertificate() {
        return authorityCertificate;
    }

    public Currency setAuthorityCertificate(Certificate authorityCertificate) {
        this.authorityCertificate = authorityCertificate;
        return this;
    }

    public State getState() {
        return state;
    }

    public Currency setState(State state) {
        this.state = state;
        return this;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Currency setTransaction(Transaction transactionParent) {
        this.transaction = transactionParent;
        return this;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User user) {
        this.toUser = user;
    }

    public CertificationRequest getCertificationRequest() {
        return certificationRequest;
    }

    public void setCertificationRequest(CertificationRequest certificationRequest) {
        this.certificationRequest = certificationRequest;
    }

    public static Currency build(String currencyEntity, BigDecimal amount, CurrencyCode currencyCode)
            throws NoSuchAlgorithmException, CertificateRequestException {
        String originRevocationHash = java.util.UUID.randomUUID().toString();
        String revocationHash = HashUtils.getHashBase64(originRevocationHash.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        Currency currency = new Currency(currencyEntity, amount, currencyCode, revocationHash, originRevocationHash);
        currency.setCertificationRequest(CertificationRequest.getCurrencyRequest(currencyEntity, currency.getRevocationHash(),
                amount, currencyCode));
        return currency;
    }

    public static Currency FROM_CERT(X509Certificate currencyCert, Certificate authorityCertificate) throws Exception {
        Currency currency = new Currency().setState(State.OK).setAuthorityCertificate(authorityCertificate);
        currency.initCertData(currencyCert);
        return currency;
    }

}
