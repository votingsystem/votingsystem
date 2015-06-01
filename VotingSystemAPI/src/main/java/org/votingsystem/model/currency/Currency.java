package org.votingsystem.model.currency;


import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

import javax.persistence.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
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

    private static Logger log = Logger.getLogger(Currency.class.getSimpleName());

    public static final long serialVersionUID = 1L;

    public TagVS getTagVS() {
        return tagVS;
    }

    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
    }

    public enum State { OK, EXPENDED, LAPSED, ERROR;} //Lapsed -> for not expended time limited currency

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="subject") private String subject;
    @Column(name="amount") private BigDecimal amount = null;
    @Column(name="batchAmount") private BigDecimal batchAmount = null;
    @Column(name="currency", nullable=false) private String currencyCode;
    @Column(name="isTimeLimited") private Boolean timeLimited = Boolean.FALSE;

    @Column(name="hashCertVS") private String hashCertVS;
    @Column(name="originHashCertVS") private String originHashCertVS;
    @Column(name="currencyServerURL") private String currencyServerURL;
    @Column(name="reason") private String reason;
    @Column(name="metaInf") private String metaInf;
    @Column(name="batchUUID") private String batchUUID;

    @Column(name="serialNumber", unique=true, nullable=false) private Long serialNumber;
    @Column(name="content", nullable=false) private byte[] content;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="authorityCertificateVS") private CertificateVS authorityCertificateVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactionvs") private TransactionVS transactionVS;

    @OneToOne private MessageSMIME cancelMessage;
    @OneToOne private MessageSMIME messageSMIME;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validFrom", length=23) private Date validFrom;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @Transient private CertificationRequestVS certificationRequest;
    @Transient private X509Certificate x509AnonymousCert;
    @Transient private transient SMIMEMessage smimeMessage;
    @Transient private String toUserIBAN;
    @Transient private String toUserName;
    @Transient private CurrencyCertExtensionDto certExtensionDto;

    public Currency() {}

    public Currency(SMIMEMessage smimeMessage) throws Exception {
        smimeMessage.isValidSignature();
        this.smimeMessage = smimeMessage;
        initCertData(smimeMessage.getCurrencyCert());
        CurrencyDto batchItemDto = JSON.getMapper().readValue(smimeMessage.getSignedContent(), CurrencyDto.class);
        this.batchUUID = batchItemDto.getBatchUUID();
        this.batchAmount = batchItemDto.getBatchAmount();
        if(TypeVS.CURRENCY_SEND != batchItemDto.getOperation())
            throw new ExceptionVS("Expected operation 'CURRENCY_SEND' - found: " + batchItemDto.getOperation() + "'");
        if(!this.currencyCode.equals(batchItemDto.getCurrencyCode())) {
            throw new ExceptionVS(getErrorPrefix() +
                    "expected currencyCode '" + currencyCode + "' - found: '" + batchItemDto.getCurrencyCode());
        }
        tagVS = new TagVS(batchItemDto.getTag());
        if(!TagVS.WILDTAG.equals(certExtensionDto.getTag()) && !certExtensionDto.getTag().equals(tagVS.getName()))
            throw new ExceptionVS("expected tag '" + certExtensionDto.getTag() + "' - found: '" +
                    batchItemDto.getTag());
        Date signatureTime = smimeMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(signatureTime.after(x509AnonymousCert.getNotAfter())) throw new ExceptionVS(getErrorPrefix() + "valid to '" +
                x509AnonymousCert.getNotAfter().toString() + "' has signature date '" + signatureTime.toString() + "'");
        this.subject = batchItemDto.getSubject();
        this.toUserIBAN = batchItemDto.getToUserIBAN();
        this.toUserName = batchItemDto.getToUserName();
        this.timeLimited = batchItemDto.isTimeLimited();
    }

    public Currency(String currencyServerURL, BigDecimal amount, String currencyCode, Boolean timeLimited, TagVS tag) {
        this.amount = amount;
        this.currencyServerURL = currencyServerURL;
        this.currencyCode = currencyCode;
        this.tagVS = tag;
        this.timeLimited = timeLimited;
        try {
            this.originHashCertVS = UUID.randomUUID().toString();
            this.hashCertVS = CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST);
            certificationRequest = CertificationRequestVS.getCurrencyRequest(
                    ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM, ContextVS.PROVIDER,
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
        this.smimeMessage = currencyRequest.getSMIME();
        this.x509AnonymousCert = currencyRequest.getX509AnonymousCert();
        this.toUserVS = currencyRequest.getToUserVS();
        this.toUserIBAN = currencyRequest.getToUserIBAN();
        this.subject = currencyRequest.getSubject();
        return this;
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

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    private String getErrorPrefix() {
        return "ERROR - Currency with hash: " + hashCertVS + " - ";
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

    public String getBatchUUID() {
        return batchUUID;
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

    public SMIMEMessage getSMIME() {
        return smimeMessage;
    }

    public void setSMIME(SMIMEMessage smimeMessage) {
        this.smimeMessage = smimeMessage;
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

    public MessageSMIME getCancelMessage() {
        return cancelMessage;
    }

    public void setCancelMessage(MessageSMIME cancelMessage) {
        this.cancelMessage = cancelMessage;
    }

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public void setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
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

    public static boolean checkIfTimeLimited(Date notBefore, Date notAfter) {
        LocalDate notBeforeLD = notBefore.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate notAfterLD = notAfter.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Period validPeriod = Period.between(notBeforeLD, notAfterLD);
        return validPeriod.getDays() <= 7;//one week
    }

    public void validateReceipt(SMIMEMessage smimeReceipt, Set<TrustAnchor> trustAnchor) throws Exception {
        if(!smimeMessage.getSigner().getSignedContentDigestBase64().equals(
                smimeReceipt.getSigner().getSignedContentDigestBase64())){
            throw new ExceptionVS("Signer content digest mismatch");
        }
        CertUtils.verifyCertificate(trustAnchor, false, new ArrayList<>(smimeReceipt.getSignersCerts()));
        this.smimeMessage = smimeReceipt;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(smimeMessage != null) s.writeObject(smimeMessage.getBytes());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] smimeMessageBytes = (byte[]) s.readObject();
        if(smimeMessageBytes != null) {
            smimeMessage = new SMIMEMessage(smimeMessageBytes);
        }
        if(x509AnonymousCert != null) {
            validFrom = x509AnonymousCert.getNotBefore();
            validTo = x509AnonymousCert.getNotAfter();
        }
    }

}
