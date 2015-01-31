package org.votingsystem.cooin.model;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;

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

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="Cooin")
public class Cooin implements Serializable  {

    private static Logger log = Logger.getLogger(Cooin.class);

    public static final long serialVersionUID = 1L;

    public enum State { OK, EXPENDED, LAPSED, ERROR;} //Lapsed -> for not expended time limited cooins

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="subject") private String subject;
    @Column(name="amount") private BigDecimal amount = null;
    @Column(name="paymentMethod") @Enumerated(EnumType.STRING) private Payment paymentMethod;
    @Column(name="currency", nullable=false) private String currencyCode;
    @Column(name="isTimeLimited") private Boolean isTimeLimited = Boolean.FALSE;

    @Column(name="hashCertVS") private String hashCertVS;
    @Column(name="originHashCertVS") private String originHashCertVS;
    @Column(name="cooinServerURL") private String cooinServerURL;
    @Column(name="reason") private String reason;
    @Column(name="metaInf") private String metaInf;
    @Column(name="batchUUID") private String batchUUID;

    @Column(name="serialNumber", unique=true, nullable=false) private Long serialNumber;
    @Column(name="content", nullable=false) @Lob private byte[] content;
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
    @Transient private String certTagVS;
    @Transient private String toUserIBAN;
    @Transient private String toUserName;
    @Transient private Cooin.CertSubject certSubject;

    public Cooin() {}

    public Cooin(SMIMEMessage smimeMessage) throws Exception {
        this.smimeMessage = smimeMessage;
        x509AnonymousCert = smimeMessage.getCooinCert();
        JSONObject certExtensionData = CertUtils.getCertExtensionData(x509AnonymousCert, ContextVS.COOIN_OID);
        initCertData(certExtensionData, smimeMessage.getCooinCert().getSubjectDN().toString());
        validateSignedData();
    }

    public Cooin(PKCS10CertificationRequest csr) throws ExceptionVS {
        this.csr = csr;
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects();
        JSONObject certExtensionData = null;
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.COOIN_TAG:
                    String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString();
                    certExtensionData = (JSONObject) JSONSerializer.toJSON(certAttributeJSONStr);
                    break;
            }
        }
        initCertData(certExtensionData, info.getSubject().toString());
    }

    public Cooin initCertData(JSONObject certExtensionData, String subjectDN) throws ExceptionVS {
        if(certExtensionData == null) throw new ExceptionVS("Cooin without cert extension data");
        if(!certExtensionData.has("hashCertVS"))  throw new ExceptionVS("Cooin without cert hash");
        cooinServerURL = certExtensionData.getString("cooinServerURL");
        hashCertVS = certExtensionData.getString("hashCertVS");
        if(hashCertVS == null) throw new ExceptionVS("Cooin without hash");
        certSubject = new Cooin.CertSubject(subjectDN, hashCertVS);
        amount = new BigDecimal(certExtensionData.getString("cooinValue"));
        currencyCode = certExtensionData.getString("currencyCode");
        certTagVS = certExtensionData.getString("tag");
        if(!certSubject.getCooinServerURL().equals(cooinServerURL) ||
                certSubject.getCooinValue().compareTo(amount) != 0 ||
                !certSubject.getCurrencyCode().equals(currencyCode) ||
                !certSubject.getTag().equals(certTagVS)) throw new ExceptionVS("Cooin with errors. SubjectDN: '" +
                subjectDN + "' - cert extension data: '" + certExtensionData.toString() + "'");
        return this;
    }

    public Cooin.CertSubject getCertSubject() {return certSubject;}

    public Cooin checkRequestWithDB(Cooin cooinRequest) throws ExceptionVS {
        if(!cooinRequest.getCooinServerURL().equals(cooinServerURL))  throw new ExceptionVS("checkRequestWithDB_cooinServerURL");
        if(!cooinRequest.getHashCertVS().equals(hashCertVS))  throw new ExceptionVS("checkRequestWithDB_hashCertVS");
        if(!cooinRequest.getCurrencyCode().equals(currencyCode))  throw new ExceptionVS("checkRequestWithDB_currencyCode");
        if(!cooinRequest.getCertTagVS().equals(tag.getName()))  throw new ExceptionVS("checkRequestWithDB_TagVS");
        if(cooinRequest.getAmount().compareTo(amount) != 0)  throw new ExceptionVS("checkRequestWithDB_amount");
        this.smimeMessage = cooinRequest.getSMIME();
        this.x509AnonymousCert = cooinRequest.getX509AnonymousCert();
        this.toUserVS = cooinRequest.getToUserVS();
        this.toUserIBAN = cooinRequest.getToUserIBAN();
        this.subject = cooinRequest.getSubject();
        return this;
    }

    private String getErrorPrefix() {
        return "ERROR - Cooin with hash: " + hashCertVS + " - ";
    }

    public void validateSignedData() throws Exception {
        JSONObject messageJSON = (JSONObject) JSONSerializer.toJSON(smimeMessage.getSignedContent());
        if(messageJSON.has("amount")) {
            BigDecimal toUserVSAmount = new BigDecimal(messageJSON.getString("amount"));
            if(amount.compareTo(toUserVSAmount) != 0) throw new ExceptionVS(getErrorPrefix() + "and value '" + amount +
                    "' has signed amount  '" + toUserVSAmount + "'");
        } else if(messageJSON.has("batchAmount")) {
            this.batchAmount = new BigDecimal(messageJSON.getString("batchAmount"));
        }
        if(messageJSON.has("batchUUID")) this.batchUUID = messageJSON.getString("batchUUID");
        if(messageJSON.has("paymentMethod")) this.paymentMethod = Payment.valueOf(messageJSON.getString("paymentMethod"));
        operation = TypeVS.valueOf(messageJSON.getString("operation"));
        if(TypeVS.COOIN_SEND != operation)
            throw new ExceptionVS("Error - Cooin with invalid operation '" + operation.toString() + "'");

        if(!currencyCode.equals(messageJSON.getString("currencyCode"))) throw new ExceptionVS(getErrorPrefix() +
                "currencyCode '" + currencyCode + "' has signed currencyCode  '" + messageJSON.getString("currencyCode"));
        tag = new TagVS(messageJSON.getString("tag"));
        if(!TagVS.WILDTAG.equals(certTagVS) && !certTagVS.equals(tag.getName()))
                throw new ExceptionVS(getErrorPrefix() + "tag '" + certTagVS + "' has signed tag  '" +
                messageJSON.getString("tag"));
        Date signatureTime = smimeMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(signatureTime.after(x509AnonymousCert.getNotAfter())) throw new ExceptionVS(getErrorPrefix() + "valid to '" +
                x509AnonymousCert.getNotAfter().toString() + "' has signature date '" + signatureTime.toString() + "'");
        subject = messageJSON.getString("subject");
        toUserIBAN = messageJSON.getString("toUserIBAN");
        toUserName = messageJSON.getString("toUserName");
        if(messageJSON.has("isTimeLimited")) isTimeLimited = messageJSON.getBoolean("isTimeLimited");
    }

    public void initSigner(byte[] csrBytes) throws Exception {
        certificationRequest.initSigner(csrBytes);
        x509AnonymousCert = certificationRequest.getCertificate();
        JSONObject certExtensionData = CertUtils.getCertExtensionData(x509AnonymousCert, ContextVS.COOIN_OID);
        initCertData(certExtensionData, x509AnonymousCert.getSubjectDN().toString());
        certSubject.addDateInfo(x509AnonymousCert);
    }

    public static Cooin load(CertificationRequestVS certificationRequest) throws Exception {
        Cooin cooin = new Cooin();
        cooin.setCertificationRequest(certificationRequest);
        cooin.initSigner(certificationRequest.getSignedCsr());
        return cooin;
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

    public String getCertTagVS() {
        return certTagVS;
    }

    public JSONObject getCertExtensionData() throws IOException {
        return CertUtils.getCertExtensionData(x509AnonymousCert, ContextVS.COOIN_OID);
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

    public Cooin(String cooinServerURL, BigDecimal amount, String currencyCode, TagVS tag) {
        this.amount = amount;
        this.cooinServerURL = cooinServerURL;
        this.currencyCode = currencyCode;
        this.tag = tag;
        try {
            setOriginHashCertVS(UUID.randomUUID().toString());
            setHashCertVS(CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST));
            certificationRequest = CertificationRequestVS.getCooinRequest(
                    ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                    ContextVS.PROVIDER, cooinServerURL, hashCertVS, amount.toString(), this.currencyCode, tag.getName());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public Cooin loadCertData(X509Certificate x509AnonymousCert, DateUtils.TimePeriod timePeriod,
            CertificateVS authorityCertificateVS) throws CertificateEncodingException {
        this.x509AnonymousCert = x509AnonymousCert;
        this.serialNumber = x509AnonymousCert.getSerialNumber().longValue();
        this.content = x509AnonymousCert.getEncoded();
        this.state = Cooin.State.OK;
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

    public String getCooinServerURL() {
        return cooinServerURL;
    }

    public void setCooinServerURL(String cooinServerURL) {
        this.cooinServerURL = cooinServerURL;
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

    public Cooin setState(State state) {
        this.state = state;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public Cooin setReason(String reason) {
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

    public Cooin setTransactionVS(TransactionVS transactionParent) {
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
        return tag;
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
            log.debug("validateReceipt - Cert validated: " + cert.getSubjectDN().toString());
        }
        this.smimeMessage = smimeReceipt;
        if(file != null) {
            FileUtils.copyStreamToFile(new ByteArrayInputStream(ObjectUtils.serializeObject(this)), file);
            file.renameTo(new File(file.getParent() + File.separator + "EXPENDED_" + file.getName()));
        }
    }

    public JSONObject getTransaction(String toUserName, String toUserIBAN, String subject,
                                     Boolean isTimeLimited) throws ExceptionVS {
        this.toUserName = toUserName;
        this.toUserIBAN = toUserIBAN;
        this.subject = subject;
        if(isTimeLimited == false && checkIfTimeLimited(x509AnonymousCert.getNotBefore(),
                x509AnonymousCert.getNotAfter())) {
            throw new ExceptionVS("Time limited Cooin with 'isTimeLimited' signature param set to false");
        }
        Map dataMap = new HashMap();
        dataMap.put("operation", TypeVS.COOIN_SEND.toString());
        dataMap.put("subject", subject);
        dataMap.put("toUserName", toUserName);
        dataMap.put("toUserIBAN", toUserIBAN);
        dataMap.put("tag", tag.getName());
        dataMap.put("amount", amount.toString());
        dataMap.put("currencyCode", currencyCode);
        dataMap.put("isTimeLimited", isTimeLimited);
        dataMap.put("UUID", UUID.randomUUID().toString());
        return (JSONObject) JSONSerializer.toJSON(dataMap);
    }

    public Map getCSRDataMap() throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        result.put("currencyCode", currencyCode);
        result.put("tag", tag.getName());
        result.put("cooinValue", amount.toString());
        result.put("csr", new String(certificationRequest.getCsrPEM(), "UTF-8"));
        return result;
    }

    public static Map<String, BigDecimal> getCurrencyMap(List<Cooin> cooinList) {
        Map<String, BigDecimal> currencyMap = new HashMap<String, BigDecimal>();
        for(Cooin cooin : cooinList){
            if(currencyMap.containsKey(cooin.getCurrencyCode())) currencyMap.put(cooin.getCurrencyCode(),
                    currencyMap.get(cooin.getCurrencyCode()).add(cooin.getAmount()));
            else currencyMap.put(cooin.getCurrencyCode(), cooin.getAmount());
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

        public TransactionVSData(JSONObject formData) {
            this.subject = formData.getString("subject");
            this.toUserName = formData.getString("toUserName");
            this.toUserIBAN = formData.getString("toUserIBAN");
            this.isTimeLimited = formData.getBoolean("isTimeLimited");
        }

        public JSONObject getJSON() {
            JSONObject formData = new JSONObject();
            formData.put("subject", subject);
            formData.put("toUserName", toUserName);
            formData.put("toUserIBAN", toUserIBAN);
            formData.put("isTimeLimited", isTimeLimited);
            return formData;
        }
    }

    public static class CertSubject implements Serializable {

        public static final long serialVersionUID = 1L;

        private String currencyCode;
        private BigDecimal cooinValue;
        private String cooinServerURL;
        private String tag;
        private String hashCertVS;
        private Map<String, String> dataMap;
        private Date notBefore;
        private Date notAfter;

        public CertSubject(String subjectDN, String hashCertVS) {
            this.hashCertVS = hashCertVS;
            if (subjectDN.contains("CURRENCY_CODE:")) currencyCode = subjectDN.split("CURRENCY_CODE:")[1].split(",")[0];
            if (subjectDN.contains("COOIN_VALUE:")) cooinValue = new BigDecimal(subjectDN.split("COOIN_VALUE:")[1].split(",")[0]);
            if (subjectDN.contains("TAG:")) tag = subjectDN.split("TAG:")[1].split(",")[0];
            if (subjectDN.contains("cooinServerURL:")) cooinServerURL = subjectDN.split("cooinServerURL:")[1].split(",")[0];
            dataMap = new HashMap<>();
            dataMap.put("cooinServerURL", cooinServerURL);
            dataMap.put("currencyCode", currencyCode);
            dataMap.put("cooinValue", cooinValue.toString());
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
        public BigDecimal getCooinValue() {return this.cooinValue;}
        public String getCooinServerURL() {return this.cooinServerURL;}
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
