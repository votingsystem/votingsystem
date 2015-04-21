package org.votingsystem.model.currency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.TransactionVSDto;
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
import org.votingsystem.util.*;
import org.votingsystem.util.currency.Payment;

import javax.persistence.*;
import java.io.*;
import java.math.BigDecimal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
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

    public enum State { OK, EXPENDED, LAPSED, ERROR;} //Lapsed -> for not expended time limited currency

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="subject") private String subject;
    @Column(name="amount") private BigDecimal amount = null;
    @Column(name="paymentMethod") @Enumerated(EnumType.STRING) private Payment paymentMethod;
    @Column(name="currency", nullable=false) private String currencyCode;
    @Column(name="isTimeLimited") private Boolean isTimeLimited = Boolean.FALSE;

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
    @JoinColumn(name="tagVS", nullable=false) private TagVS tag;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactionvs") private TransactionVS transactionVS;

    @OneToOne private MessageSMIME cancelMessage;
    @OneToOne private MessageSMIME messageSMIME;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validFrom", length=23) private Date validFrom;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @Transient private TypeVS operation;
    @Transient private BigDecimal batchAmount;
    @Transient private CertificationRequestVS certificationRequest;
    @Transient private PKCS10CertificationRequest csr;
    @Transient private X509Certificate x509AnonymousCert;
    @Transient private transient SMIMEMessage smimeMessage;
    @Transient private File file;
    @Transient private String toUserIBAN;
    @Transient private String toUserName;
    @Transient private Currency.CertSubject certSubject;
    @Transient private CurrencyCertExtensionDto certExtensionDto;

    public Currency() {}

    public Currency(SMIMEMessage smimeMessage) throws Exception {
        this.smimeMessage = smimeMessage;
        x509AnonymousCert = smimeMessage.getCurrencyCert();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509AnonymousCert, ContextVS.CURRENCY_OID);
        initCertData(certExtensionDto, smimeMessage.getCurrencyCert().getSubjectDN().toString());
        validateSignedData();
    }

    public Currency(PKCS10CertificationRequest csr) throws ExceptionVS, IOException {
        this.csr = csr;
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects();
        CurrencyCertExtensionDto certExtensionDto = null;
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.CURRENCY_TAG:
                    String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString();
                    certExtensionDto = JSON.getMapper().readValue(certAttributeJSONStr, CurrencyCertExtensionDto.class);
                    break;
            }
        }
        initCertData(certExtensionDto, info.getSubject().toString());
    }

    public Currency initCertData(CurrencyCertExtensionDto certExtensionDto, String subjectDN) throws ExceptionVS {
        if(certExtensionDto == null) throw new ValidationExceptionVS("error missing cert extension data");
        this.certExtensionDto = certExtensionDto;
        certSubject = new CertSubject(subjectDN, hashCertVS);
        hashCertVS = certExtensionDto.getHashCertVS();
        currencyServerURL = certExtensionDto.getCurrencyServerURL();
        if(!certSubject.getCurrencyServerURL().equals(certExtensionDto.getCurrencyServerURL()))
            throw new ValidationExceptionVS("currencyServerURL: " + currencyServerURL + " - certSubject: " + certSubject);
        amount = certExtensionDto.getAmount();
        if(certSubject.getCurrencyValue().compareTo(amount) != 0)
            throw new ValidationExceptionVS("amount: " + amount + " - certSubject: " + certSubject);
        currencyCode = certExtensionDto.getCurrencyCode();
        if(!certSubject.getCurrencyCode().equals(currencyCode))
            throw new ValidationExceptionVS("currencyCode: " + currencyCode + " - certSubject: " + certSubject);
        if(!certSubject.getTag().equals(certExtensionDto.getTag()))
            throw new ValidationExceptionVS("currencyCode: " + currencyCode + " - certSubject: " + certSubject);
        return this;
    }

    public CertSubject getCertSubject() {return certSubject;}

    public Currency checkRequestWithDB(Currency currencyRequest) throws ExceptionVS {
        if(!currencyRequest.getCurrencyServerURL().equals(currencyServerURL))  throw new ExceptionVS("checkRequestWithDB_currencyServerURL");
        if(!currencyRequest.getHashCertVS().equals(hashCertVS))  throw new ExceptionVS("checkRequestWithDB_hashCertVS");
        if(!currencyRequest.getCurrencyCode().equals(currencyCode))  throw new ExceptionVS("checkRequestWithDB_currencyCode");
        if(!currencyRequest.getCertExtensionDto().getTag().equals(tag.getName()))  throw new ExceptionVS("checkRequestWithDB_TagVS");
        if(currencyRequest.getAmount().compareTo(amount) != 0)  throw new ExceptionVS("checkRequestWithDB_amount");
        this.smimeMessage = currencyRequest.getSMIME();
        this.x509AnonymousCert = currencyRequest.getX509AnonymousCert();
        this.toUserVS = currencyRequest.getToUserVS();
        this.toUserIBAN = currencyRequest.getToUserIBAN();
        this.subject = currencyRequest.getSubject();
        return this;
    }

    private String getErrorPrefix() {
        return "ERROR - Currency with hash: " + hashCertVS + " - ";
    }

    public void validateSignedData() throws Exception {
        ObjectNode dataJSON = (ObjectNode) new ObjectMapper().readTree(smimeMessage.getSignedContent());
        if(dataJSON.has("amount")) {
            BigDecimal toUserVSAmount = new BigDecimal(dataJSON.get("amount").asText());
            if(amount.compareTo(toUserVSAmount) != 0) throw new ExceptionVS(getErrorPrefix() + "and value '" + amount +
                    "' has signed amount  '" + toUserVSAmount + "'");
        } else if(dataJSON.has("batchAmount")) {
            this.batchAmount = new BigDecimal(dataJSON.get("batchAmount").asText());
        }
        if(dataJSON.has("batchUUID")) this.batchUUID = dataJSON.get("batchUUID").asText();
        if(dataJSON.has("paymentMethod")) this.paymentMethod = Payment.valueOf(dataJSON.get("paymentMethod").asText());
        operation = TypeVS.valueOf(dataJSON.get("operation").asText());
        if(TypeVS.CURRENCY_SEND != operation)
            throw new ExceptionVS("Error - Currency with invalid operation '" + operation.toString() + "'");

        if(!currencyCode.equals(dataJSON.get("currencyCode").asText())) throw new ExceptionVS(getErrorPrefix() +
                "currencyCode '" + currencyCode + "' has signed currencyCode  '" + dataJSON.get("currencyCode").asText());
        tag = new TagVS(dataJSON.get("tag").asText());
        if(!TagVS.WILDTAG.equals(certExtensionDto.getTag()) && !certExtensionDto.getTag().equals(tag.getName()))
                throw new ExceptionVS(getErrorPrefix() + "tag '" + certExtensionDto.getTag() + "' has signed tag  '" +
                dataJSON.get("tag"));
        Date signatureTime = smimeMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(signatureTime.after(x509AnonymousCert.getNotAfter())) throw new ExceptionVS(getErrorPrefix() + "valid to '" +
                x509AnonymousCert.getNotAfter().toString() + "' has signature date '" + signatureTime.toString() + "'");
        subject = dataJSON.get("subject").asText();
        toUserIBAN = dataJSON.get("toUserIBAN").asText();
        toUserName = dataJSON.get("toUserName").asText();
        if(dataJSON.has("isTimeLimited")) isTimeLimited = dataJSON.get("isTimeLimited").asBoolean();
    }

    public void initSigner(byte[] csrBytes) throws Exception {
        certificationRequest.initSigner(csrBytes);
        x509AnonymousCert = certificationRequest.getCertificate();
        validFrom = x509AnonymousCert.getNotBefore();
        validTo = x509AnonymousCert.getNotAfter();
        CurrencyCertExtensionDto certExtensionData = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509AnonymousCert, ContextVS.CURRENCY_OID);
        initCertData(certExtensionData, x509AnonymousCert.getSubjectDN().toString());
        certSubject.addDateInfo(x509AnonymousCert);
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

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public Boolean getIsTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
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

    public Map<String, String> getCertExtensionData() throws IOException {
        return CertUtils.getCertExtensionData(x509AnonymousCert, ContextVS.CURRENCY_OID);
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

    public PKCS10CertificationRequest getCsr() {
        return csr;
    }

    public void setCsr(PKCS10CertificationRequest csr) {
        this.csr = csr;
    }

    public Currency(String currencyServerURL, BigDecimal amount, String currencyCode, TagVS tag) {
        this.amount = amount;
        this.currencyServerURL = currencyServerURL;
        this.currencyCode = currencyCode;
        this.tag = tag;
        try {
            setOriginHashCertVS(UUID.randomUUID().toString());
            setHashCertVS(CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST));
            certificationRequest = CertificationRequestVS.getCurrencyRequest(
                    ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                    ContextVS.PROVIDER, currencyServerURL, hashCertVS, amount, this.currencyCode, tag.getName());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public Currency loadCertData(X509Certificate x509AnonymousCert, TimePeriod timePeriod,
            CertificateVS authorityCertificateVS) throws CertificateEncodingException {
        this.x509AnonymousCert = x509AnonymousCert;
        this.serialNumber = x509AnonymousCert.getSerialNumber().longValue();
        this.content = x509AnonymousCert.getEncoded();
        this.state = State.OK;
        this.validFrom = timePeriod.getDateFrom();
        this.validTo = timePeriod.getDateTo();
        this.authorityCertificateVS = authorityCertificateVS;
        return this;
    }


    public String getMetaInf() {
        return metaInf;
    }

    public void setMetaInf(String metaInf) {
        this.metaInf = metaInf;
    }

    public byte[] getIssuedCertPEM() throws IOException {
        return CertUtils.getPEMEncoded(x509AnonymousCert);
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

    public Payment getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Payment paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public static boolean checkIfTimeLimited(Date notBefore, Date notAfter) {
        LocalDate notBeforeLD = notBefore.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate notAfterLD = notAfter.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Period validPeriod = Period.between(notBeforeLD, notAfterLD);
        return validPeriod.getDays() <= 7;//one week
    }

    public TagVS getTag() {
        if(tag != null) return tag;
        else return new TagVS(certExtensionDto.getTag());
    }

    public void setTag(TagVS tag) {
        this.tag = tag;
    }

    public void validateReceipt(SMIMEMessage smimeReceipt, Set<TrustAnchor> trustAnchor)
            throws Exception {
        if(!smimeMessage.getSigner().getContentDigestBase64().equals(smimeReceipt.getSigner().getContentDigestBase64())){
            throw new ExceptionVS("Signer content digest mismatch");
        }
        for(X509Certificate cert : smimeReceipt.getSignersCerts()) {
            CertUtils.verifyCertificate(trustAnchor, false, Arrays.asList(cert));
            log.info("validateReceipt - Cert validated: " + cert.getSubjectDN().toString());
        }
        this.smimeMessage = smimeReceipt;
        if(file != null) {
            FileUtils.copyStreamToFile(new ByteArrayInputStream(ObjectUtils.serializeObject(this)), file);
            file.renameTo(new File(file.getParent() + File.separator + "EXPENDED_" + file.getName()));
        }
    }

    public TransactionVSDto getSendRequest(String toUserName, String toUserIBAN, String subject,
                                     Boolean isTimeLimited) throws ExceptionVS {
        this.toUserName = toUserName;
        this.toUserIBAN = toUserIBAN;
        this.subject = subject;
        if(isTimeLimited == false && checkIfTimeLimited(x509AnonymousCert.getNotBefore(),
                x509AnonymousCert.getNotAfter())) {
            throw new ExceptionVS("Time limited Currency with 'isTimeLimited' signature param set to false");
        }
        return TransactionVSDto.CURRENCY_SEND(toUserName, subject, amount, currencyCode, toUserIBAN,
                isTimeLimited, tag.getName());
    }

    public static Map<String, BigDecimal> getCurrencyMap(Collection<Currency> currencyList) {
        Map<String, BigDecimal> currencyMap = new HashMap<String, BigDecimal>();
        for(Currency currency : currencyList){
            if(currencyMap.containsKey(currency.getCurrencyCode())) currencyMap.put(currency.getCurrencyCode(),
                    currencyMap.get(currency.getCurrencyCode()).add(currency.getAmount()));
            else currencyMap.put(currency.getCurrencyCode(), currency.getAmount());
        }
        return currencyMap;
    }

    public static class TransactionVSData {

        private String subject, toUserName, toUserIBAN;
        private Boolean isTimeLimited;

        public String getSubject(){return subject;}
        public String getToUserName(){return toUserName;}
        public String getToUserIBAN(){return toUserIBAN;}
        public Boolean getIsTimeLimited(){return isTimeLimited;}

        public TransactionVSData() {}

        public TransactionVSData(String subject, String toUserName, String toUserIBAN, Boolean isTimeLimited) {
            this.subject = subject;
            this.toUserName = toUserName;
            this.toUserIBAN = toUserIBAN;
            this.isTimeLimited = isTimeLimited;
        }

        public TransactionVSData(Map formData) {
            this.subject = (String) formData.get("subject");
            this.toUserName = (String) formData.get("toUserName");
            this.toUserIBAN = (String) formData.get("toUserIBAN");
            this.isTimeLimited = (Boolean) formData.get("isTimeLimited");
        }

        public Map getJSON() {
            Map result = new HashMap<>();
            result.put("subject", subject);
            result.put("toUserName", toUserName);
            result.put("toUserIBAN", toUserIBAN);
            result.put("isTimeLimited", isTimeLimited);
            return result;
        }
    }

    public static class CertSubject implements Serializable {

        public static final long serialVersionUID = 1L;

        private String currencyCode;
        private BigDecimal currencyValue;
        private String currencyServerURL;
        private String tag;
        private String hashCertVS;
        private Map<String, String> dataMap;
        private Date notBefore;
        private Date notAfter;

        public CertSubject(String subjectDN, String hashCertVS) {
            this.hashCertVS = hashCertVS;
            if (subjectDN.contains("CURRENCY_CODE:")) currencyCode = subjectDN.split("CURRENCY_CODE:")[1].split(",")[0];
            if (subjectDN.contains("CURRENCY_VALUE:")) currencyValue = new BigDecimal(subjectDN.split("CURRENCY_VALUE:")[1].split(",")[0]);
            if (subjectDN.contains("TAG:")) tag = subjectDN.split("TAG:")[1].split(",")[0];
            if (subjectDN.contains("currencyServerURL:")) currencyServerURL = subjectDN.split("currencyServerURL:")[1].split(",")[0];
            dataMap = new HashMap<>();
            dataMap.put("currencyServerURL", currencyServerURL);
            dataMap.put("currencyCode", currencyCode);
            dataMap.put("currencyValue", currencyValue.toString());
            dataMap.put("tag", tag);
            dataMap.put("hashCertVS", hashCertVS);
        }

        public void addDateInfo(X509Certificate x509AnonymousCert) {
            this.notBefore = x509AnonymousCert.getNotBefore();
            this.notAfter = x509AnonymousCert.getNotAfter();
            dataMap.put("notBefore", DateUtils.getDateStr(x509AnonymousCert.getNotBefore()));
            dataMap.put("notAfter", DateUtils.getDateStr(x509AnonymousCert.getNotAfter()));
        }

        public String getCurrencyCode() {return this.currencyCode;}
        public BigDecimal getCurrencyValue() {return this.currencyValue;}
        public String getCurrencyServerURL() {return this.currencyServerURL;}
        public String getTag() {return this.tag;}
        public Map<String, String> getDataMap() {return dataMap;}
        public Date getNotBefore() { return notBefore; }
        public Date getNotAfter() { return notAfter; }

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
            smimeMessage = new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
        }
        if(x509AnonymousCert != null) {
            validFrom = x509AnonymousCert.getNotBefore();
            validTo = x509AnonymousCert.getNotAfter();
        }

    }

}
