package org.votingsystem.model.currency;


import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.CertificationRequestVS;

import javax.persistence.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="Currency")
public class Currency extends EntityVS implements Serializable  {

    private static Logger log = Logger.getLogger(Currency.class.getName());

    public static final long serialVersionUID = 1L;

    public enum State { OK, EXPENDED, LAPSED, UNKNOWN, ERROR_CERT_AUTHORITY, ERROR;} //Lapsed -> for not expended time limited currency

    public enum Type { LEFT_OVER, CHANGE, REQUEST}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="subject") private String subject;
    @Column(name="amount") private BigDecimal amount = null;
    @Column(name="currency", nullable=false) private String currencyCode;
    @Column(name="isTimeLimited") private Boolean timeLimited = Boolean.FALSE;

    @Column(name="hashCertVS") private String hashCertVS;
    @Column(name="originHashCertVS") private String originHashCertVS;
    @Column(name="currencyServerURL") private String currencyServerURL;
    @Column(name="reason") private String reason;
    @Column(name="metaInf") private String metaInf;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="currencyBatch") private CurrencyBatch currencyBatch;

    @Column(name="serialNumber", unique=true, nullable=false) private Long serialNumber;
    @Column(name="content", nullable=false) private byte[] content;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="authorityCertificateVS") private CertificateVS authorityCertificateVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactionvs") private TransactionVS transactionVS;

    @OneToOne @JoinColumn(name="cmsMessage") private CMSMessage cmsMessage;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validFrom", length=23) private Date validFrom;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @Transient private CertificationRequestVS certificationRequest;
    @Transient private X509Certificate x509AnonymousCert;
    @Transient private transient CMSSignedMessage cmsSignedMessage;
    @Transient private String toUserIBAN;
    @Transient private String toUserName;
    @Transient private CurrencyDto batchItemDto;
    @Transient private CurrencyCertExtensionDto certExtensionDto;

    public Currency() {}

    public Currency(CMSSignedMessage cmsSignedMessage) throws Exception {
        cmsSignedMessage.isValidSignature();
        this.cmsSignedMessage = cmsSignedMessage;
        initCertData(cmsSignedMessage.getCurrencyCert());
        batchItemDto = cmsSignedMessage.getSignedContent(CurrencyDto.class);
        if(!this.currencyCode.equals(batchItemDto.getCurrencyCode())) {
            throw new ExceptionVS(getErrorPrefix() +
                    "expected currencyCode '" + currencyCode + "' - found: '" + batchItemDto.getCurrencyCode());
        }
        if(!TagVS.WILDTAG.equals(certExtensionDto.getTag()) && !certExtensionDto.getTag().equals(batchItemDto.getTag()))
            throw new ExceptionVS("expected tag '" + certExtensionDto.getTag() + "' - found: '" +
                    batchItemDto.getTag());
        Date signatureTime = cmsSignedMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(signatureTime.after(x509AnonymousCert.getNotAfter())) throw new ExceptionVS(getErrorPrefix() + "valid to '" +
                x509AnonymousCert.getNotAfter().toString() + "' has signature date '" + signatureTime.toString() + "'");
        this.subject = batchItemDto.getSubject();
        this.toUserIBAN = batchItemDto.getToUserIBAN();
        this.toUserName = batchItemDto.getToUserName();
    }

    public Currency(String currencyServerURL, BigDecimal amount, String currencyCode,
                    Boolean timeLimited, String hashCertVS, TagVS tag) {
        this.amount = amount;
        this.currencyServerURL = currencyServerURL;
        this.currencyCode = currencyCode;
        this.tagVS = tag;
        this.timeLimited = timeLimited;
        try {
            this.hashCertVS = hashCertVS;
            certificationRequest = CertificationRequestVS.getCurrencyRequest(
                    ContextVS.SIGNATURE_ALGORITHM, ContextVS.PROVIDER,
                    currencyServerURL, hashCertVS, amount, this.currencyCode, timeLimited, tagVS.getName());
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

    public Currency(String currencyServerURL, BigDecimal amount, String currencyCode, Boolean timeLimited, TagVS tag) {
        this.amount = amount;
        this.currencyServerURL = currencyServerURL;
        this.currencyCode = currencyCode;
        this.tagVS = tag;
        this.timeLimited = timeLimited;
        try {
            this.originHashCertVS = UUID.randomUUID().toString();
            this.hashCertVS = StringUtils.getHashBase64(getOriginHashCertVS(), ContextVS.DATA_DIGEST_ALGORITHM);
            certificationRequest = CertificationRequestVS.getCurrencyRequest(
                    ContextVS.SIGNATURE_ALGORITHM, ContextVS.PROVIDER,
                    currencyServerURL, hashCertVS, amount, this.currencyCode, timeLimited, tag.getName());
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

    public static Currency FROM_CERT(X509Certificate x509AnonymousCert, TagVS tagVS,
                                     CertificateVS authorityCertificateVS) throws Exception {
        Currency currency = new Currency();
        currency.initCertData(x509AnonymousCert);
        currency.tagVS = tagVS;
        currency.authorityCertificateVS = authorityCertificateVS;
        currency.state = State.OK;
        return currency;
    }

    public CurrencyDto getBatchItemDto() {
        return batchItemDto;
    }

    public void setBatchItemDto(CurrencyDto batchItemDto) {
        this.batchItemDto = batchItemDto;
    }

    public Currency initCertData(X509Certificate x509AnonymousCert) throws Exception {
        this.x509AnonymousCert = x509AnonymousCert;
        content = x509AnonymousCert.getEncoded();
        serialNumber = x509AnonymousCert.getSerialNumber().longValue();
        certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509AnonymousCert, ContextVS.CURRENCY_OID);
        if(certExtensionDto == null) throw new ValidationExceptionVS("error missing cert extension data");
        amount = certExtensionDto.getAmount();
        currencyCode = certExtensionDto.getCurrencyCode();
        hashCertVS = certExtensionDto.getHashCertVS();
        timeLimited = certExtensionDto.getTimeLimited();
        tagVS = new TagVS(certExtensionDto.getTag());
        currencyServerURL = certExtensionDto.getCurrencyServerURL();
        validFrom = x509AnonymousCert.getNotBefore();
        validTo = x509AnonymousCert.getNotAfter();
        String subjectDN = x509AnonymousCert.getSubjectDN().toString();
        CurrencyDto certSubjectDto = CurrencyDto.getCertSubjectDto(subjectDN, hashCertVS);
        if(!certSubjectDto.getCurrencyServerURL().equals(certExtensionDto.getCurrencyServerURL()))
            throw new ValidationExceptionVS("currencyServerURL: " + currencyServerURL + " - certSubject: " + subjectDN);
        if(certSubjectDto.getAmount().compareTo(amount) != 0)
            throw new ValidationExceptionVS("amount: " + amount + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getCurrencyCode().equals(currencyCode))
            throw new ValidationExceptionVS("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getTag().equals(certExtensionDto.getTag()))
            throw new ValidationExceptionVS("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        return this;
    }

    public Currency checkRequestWithDB(Currency currencyRequest) throws ExceptionVS {
        if(!currencyRequest.getCurrencyServerURL().equals(currencyServerURL))
            throw new ExceptionVS("checkRequestWithDB_currencyServerURL");
        if(!currencyRequest.getHashCertVS().equals(hashCertVS))  throw new ExceptionVS("checkRequestWithDB_hashCertVS");
        if(!currencyRequest.getCurrencyCode().equals(currencyCode))
            throw new ExceptionVS("checkRequestWithDB_currencyCode");
        if (!currencyRequest.getCertExtensionDto().getTag().equals(tagVS.getName()))
            throw new ExceptionVS("checkRequestWithDB_TagVS");
        if(currencyRequest.getAmount().compareTo(amount) != 0)  throw new ExceptionVS("checkRequestWithDB_amount");
        this.cmsSignedMessage = currencyRequest.getCMS();
        this.x509AnonymousCert = currencyRequest.getX509AnonymousCert();
        this.toUserVS = currencyRequest.getToUserVS();
        this.toUserIBAN = currencyRequest.getToUserIBAN();
        this.subject = currencyRequest.getSubject();
        return this;
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

    public void setType(Type type) {
        this.type = type;
    }

    public void setX509AnonymousCert(X509Certificate x509AnonymousCert) {
        this.x509AnonymousCert = x509AnonymousCert;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    private String getErrorPrefix() {
        return "ERROR - Currency with hash: " + hashCertVS + " - ";
    }

    public TagVS getTagVS() {
        return tagVS;
    }

    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
    }

    public void initSigner(byte[] csrBytes) throws Exception {
        certificationRequest.initSigner(csrBytes);
        initCertData(certificationRequest.getCertificate());
    }

    public static Currency fromCertificationRequestVS(CertificationRequestVS certificationRequest) throws Exception {
        Currency currency = new Currency();
        currency.setCertificationRequest(certificationRequest);
        currency.initSigner(certificationRequest.getSignedCsr());
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

    public CMSSignedMessage getCMS() {
        return cmsSignedMessage;
    }

    public void setCMS(CMSSignedMessage cmsSignedMessage) {
        this.cmsSignedMessage = cmsSignedMessage;
    }

    public X509Certificate getX509AnonymousCert() {
        return x509AnonymousCert;
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

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyServerURL() {
        return currencyServerURL;
    }

    public void setCurrencyServerURL(String currencyServerURL) {
        this.currencyServerURL = currencyServerURL;
    }

    public String getOriginHashCertVS() {
        return originHashCertVS;
    }

    public void setOriginHashCertVS(String originHashCertVS) {
        this.originHashCertVS = originHashCertVS;
    }

    public CMSMessage getCmsMessage() {
        return cmsMessage;
    }

    public void setCmsMessage(CMSMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
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

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public CertificateVS getAuthorityCertificateVS() {
        return authorityCertificateVS;
    }

    public void setAuthorityCertificateVS(CertificateVS authorityCertificateVS) {
        this.authorityCertificateVS = authorityCertificateVS;
    }

    public State getState() {
        return state;
    }

    public Currency setState(State state) {
        this.state = state;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public Currency setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public TransactionVS getTransactionVS() {
        return transactionVS;
    }

    public Currency setTransactionVS(TransactionVS transactionParent) {
        this.transactionVS = transactionParent;
        return this;
    }

    public UserVS getToUserVS() {
        return toUserVS;
    }

    public void setToUserVS(UserVS userVS) {
        this.toUserVS = userVS;
    }

    public CertificationRequestVS getCertificationRequest() {
        return certificationRequest;
    }

    public void setCertificationRequest(CertificationRequestVS certificationRequest) {
        this.certificationRequest = certificationRequest;
    }

    public void validateReceipt(CMSSignedMessage cmsReceipt, Set<TrustAnchor> trustAnchor) throws Exception {
        if(!cmsSignedMessage.getSigner().getSignedContentDigestBase64().equals(
                cmsReceipt.getSigner().getSignedContentDigestBase64())){
            throw new ExceptionVS("Signer content digest mismatch");
        }
        CertUtils.verifyCertificate(trustAnchor, false, new ArrayList<>(cmsReceipt.getSignersCerts()));
        this.cmsSignedMessage = cmsReceipt;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(cmsSignedMessage != null) s.writeObject(cmsSignedMessage.toASN1Structure().getEncoded());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] cmsMessageBytes = (byte[]) s.readObject();
        if(cmsMessageBytes != null) {
            cmsSignedMessage = new CMSSignedMessage(cmsMessageBytes);
        }
        if(x509AnonymousCert != null) {
            validFrom = x509AnonymousCert.getNotBefore();
            validTo = x509AnonymousCert.getNotAfter();
        }
    }

}
