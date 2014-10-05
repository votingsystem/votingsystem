package org.votingsystem.vicket.model;

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
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.MetaInfMsg;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="Vicket")
public class Vicket implements Serializable  {

    private static Logger log = Logger.getLogger(Vicket.class);

    public static final long serialVersionUID = 1L;

    public Boolean getIsTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public enum State { OK, PROJECTED, REJECTED, CANCELLED, EXPENDED, LAPSED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="subject") private String subject;
    @Column(name="amount") private BigDecimal amount = null;
    @Column(name="currency", nullable=false) private String currencyCode;
    @Column(name="isTimeLimited") private Boolean isTimeLimited = Boolean.FALSE;

    @Column(name="hashCertVS") private String hashCertVS;
    @Column(name="originHashCertVS") private String originHashCertVS;
    @Column(name="vicketServerURL") private String vicketServerURL;
    @Column(name="reason") private String reason;

    @Column(name="serialNumber", unique=true, nullable=false) private Long serialNumber;
    @Column(name="content", nullable=false) @Lob private byte[] content;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="authorityCertificateVS") private CertificateVS authorityCertificateVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tag", nullable=false) private VicketTagVS tag;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactionvs") private TransactionVS transactionVS;

    @OneToOne private MessageSMIME cancelMessage;
    @OneToOne private MessageSMIME messageSMIME;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validFrom", length=23) private Date validFrom;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @Transient private CertificationRequestVS certificationRequest;
    @Transient private PKCS10CertificationRequest csr;
    @Transient private X509Certificate x509AnonymousCert;
    @Transient private SMIMEMessage smimeMessage;
    @Transient private String signedTagVS;
    @Transient private String toUserIBAN;
    @Transient private String toUserName;

    public Vicket() {}

    public Vicket(SMIMEMessage smimeMessage) throws ExceptionVS, IOException {
        this.smimeMessage = smimeMessage;
        x509AnonymousCert = smimeMessage.getCertWithCertExtension();
        JSONObject certExtensionData = CertUtil.getCertExtensionData(x509AnonymousCert, ContextVS.VICKET_OID);
        initCertData(certExtensionData, smimeMessage.getCertWithCertExtension().getSubjectDN().toString());
        validateSignedData();
    }

    public Vicket(PKCS10CertificationRequest csr) throws ExceptionVS {
        this.csr = csr;
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects();
        JSONObject certExtensionData = null;
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.VICKET_TAG:
                    String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString();
                    certExtensionData = (JSONObject) JSONSerializer.toJSON(certAttributeJSONStr);
                    break;
            }
        }
        initCertData(certExtensionData, info.getSubject().toString());
    }

    public Vicket initCertData(JSONObject certExtensionData, String subjectDN) throws ExceptionVS {
        if(certExtensionData == null) throw new ExceptionVS("Vicket without cert extension data");
        if(!certExtensionData.has("hashCertVS"))  throw new ExceptionVS("Vicket without cert hash");
        Vicket.CertSubject certSubject = Vicket.getCertSubject(subjectDN);
        vicketServerURL = certExtensionData.getString("vicketServerURL");
        hashCertVS = certExtensionData.getString("hashCertVS");
        if(hashCertVS == null) throw new ExceptionVS("Vicket without hash");
        amount = new BigDecimal(certExtensionData.getString("vicketValue"));
        currencyCode = certExtensionData.getString("currencyCode");
        signedTagVS = certExtensionData.getString("tagVS");
        if(!certSubject.getVicketServerURL().equals(vicketServerURL) ||
                certSubject.getVicketValue().compareTo(amount) != 0 ||
                !certSubject.getCurrencyCode().equals(currencyCode) ||
                !certSubject.gettagVS().equals(signedTagVS)) throw new ExceptionVS("Vicket with errors. SubjectDN: '" +
                subjectDN + "' - cert extension data: '" + certExtensionData.toString() + "'");
        return this;
    }

    public Vicket checkRequestWithDB(Vicket vicketRequest) throws ExceptionVS {
        if(!vicketRequest.getVicketServerURL().equals(vicketServerURL))  throw new ExceptionVS("checkRequestWithDB_vicketServerURL");
        if(!vicketRequest.getHashCertVS().equals(hashCertVS))  throw new ExceptionVS("checkRequestWithDB_hashCertVS");
        if(!vicketRequest.getCurrencyCode().equals(currencyCode))  throw new ExceptionVS("checkRequestWithDB_currencyCode");
        if(!vicketRequest.getSignedTagVS().equals(tag.getName()))  throw new ExceptionVS("checkRequestWithDB_TagVS");
        if(vicketRequest.getAmount().compareTo(amount) != 0)  throw new ExceptionVS("checkRequestWithDB_amount");
        this.smimeMessage = vicketRequest.getSMIMEMessage();
        this.x509AnonymousCert = vicketRequest.x509AnonymousCert;
        return this;
    }

    public void validateSignedData() throws ExceptionVS {
        JSONObject messageJSON = (JSONObject) JSONSerializer.toJSON(smimeMessage.getSignedContent());
        BigDecimal toUserVSAmount = new BigDecimal(messageJSON.getString("amount"));
        TypeVS operation = TypeVS.valueOf(messageJSON.getString("operation"));
        if(TypeVS.VICKET != operation) throw new ExceptionVS("Error - Vicket with invalid operation '" + operation.toString() + "'");
        if(amount.compareTo(toUserVSAmount) != 0) throw new ExceptionVS("Error - Vicket with value of '" + amount +
                "' has signed amount  '" + toUserVSAmount + "'");
        if(!currencyCode.equals(messageJSON.getString("currencyCode"))) throw new ExceptionVS("Error - Vicket with currencyCode '" +
                currencyCode + "' has signed currencyCode  '" + messageJSON.getString("currencyCode"));
        if(!signedTagVS.equals(messageJSON.getString("tagVS"))) throw new ExceptionVS("Error - Vicket with tag '" +
                signedTagVS + "' has signed tag  '" + messageJSON.getString("tagVS"));
        Date signatureTime = smimeMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(signatureTime.after(x509AnonymousCert.getNotAfter())) throw new ExceptionVS("Error - Vicket valid to '" +
                x509AnonymousCert.getNotAfter().toString() + "' has signature date '" + signatureTime.toString() + "'");
        subject = messageJSON.getString("subject");
        toUserIBAN = messageJSON.getString("toUserIBAN");
        toUserName = messageJSON.getString("toUserName");
        if(messageJSON.has("isTimeLimited")) isTimeLimited = messageJSON.getBoolean("isTimeLimited");
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

    public String getSignedTagVS() {
        return signedTagVS;
    }

    public JSONObject getCertExtensionData() throws IOException {
        return CertUtil.getCertExtensionData(x509AnonymousCert, ContextVS.VICKET_OID);
    }

    public SMIMEMessage getSMIMEMessage() {
        return smimeMessage;
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

    public Vicket(String vicketServerURL, BigDecimal amount, String currencyCode, VicketTagVS tag) {
        this.amount = amount;
        this.vicketServerURL = vicketServerURL;
        this.currencyCode = currencyCode;
        this.tag = tag;
        try {
            setOriginHashCertVS(UUID.randomUUID().toString());
            setHashCertVS(CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST));
            certificationRequest = CertificationRequestVS.getVicketRequest(
                    ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                    ContextVS.PROVIDER, vicketServerURL, hashCertVS, amount.toString(), this.currencyCode, tag.getName());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public Vicket loadCertData(X509Certificate x509AnonymousCert, DateUtils.TimePeriod timePeriod,
            CertificateVS authorityCertificateVS) throws CertificateEncodingException {
        this.x509AnonymousCert = x509AnonymousCert;
        this.serialNumber = x509AnonymousCert.getSerialNumber().longValue();
        this.content = x509AnonymousCert.getEncoded();
        this.state = Vicket.State.OK;
        this.validFrom = timePeriod.getDateFrom();
        this.validTo = timePeriod.getDateTo();
        this.authorityCertificateVS = authorityCertificateVS;
        return this;
    }

    public byte[] getIssuedCertPEM() throws IOException {
        return CertUtil.getPEMEncoded(x509AnonymousCert);
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

    public String getVicketServerURL() {
        return vicketServerURL;
    }

    public void setVicketServerURL(String vicketServerURL) {
        this.vicketServerURL = vicketServerURL;
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

    public Vicket setState(State state) {
        this.state = state;
        return this;
    }


    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public Vicket setTransactionVS(TransactionVS transactionParent) {
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

    public VicketTagVS getTag() {
        return tag;
    }

    public void setTag(VicketTagVS tag) {
        this.tag = tag;
    }

    public static CertSubject getCertSubject(String subjectDN) {
        return new CertSubject(subjectDN);
    }


    public JSONObject getTransactionRequest(String toUserName, String toUserIBAN, String subject, Boolean isTimeLimited) {
        this.toUserName = toUserName;
        this.toUserIBAN = toUserIBAN;
        this.subject = subject;
        this.isTimeLimited = isTimeLimited;
        Map dataMap = new HashMap();
        dataMap.put("operation", TypeVS.VICKET.toString());
        dataMap.put("subject", subject);
        dataMap.put("toUserName", toUserName);
        dataMap.put("toUserIBAN", toUserIBAN);
        dataMap.put("tagVS", tag.getName());
        dataMap.put("amount", amount.toString());
        dataMap.put("currencyCode", currencyCode);
        if(isTimeLimited != null) dataMap.put("isTimeLimited", isTimeLimited.booleanValue());
        dataMap.put("UUID", UUID.randomUUID().toString());
        return (JSONObject) JSONSerializer.toJSON(dataMap);
    }

    public Map getCSRDataMap() throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        result.put("currencyCode", currencyCode);
        result.put("tagVS", tag.getName());
        result.put("vicketValue", amount.toString());
        result.put("csr", new String(certificationRequest.getCsrPEM(), "UTF-8"));
        return result;
    }

    public static class CertSubject {
        String currencyCode;
        BigDecimal vicketValue;
        String vicketServerURL;
        String tagVS;
        public CertSubject(String subjectDN) {
            if (subjectDN.contains("CURRENCY_CODE:")) currencyCode = subjectDN.split("CURRENCY_CODE:")[1].split(",")[0];
            if (subjectDN.contains("VICKET_VALUE:")) vicketValue = new BigDecimal(subjectDN.split("VICKET_VALUE:")[1].split(",")[0]);
            if (subjectDN.contains("TAGVS:")) tagVS = subjectDN.split("TAGVS:")[1].split(",")[0];
            if (subjectDN.contains("vicketServerURL:")) vicketServerURL = subjectDN.split("vicketServerURL:")[1].split(",")[0];
        }
        public String getCurrencyCode() {return this.currencyCode;}
        public BigDecimal getVicketValue() {return this.vicketValue;}
        public String getVicketServerURL() {return this.vicketServerURL;}
        public String gettagVS() {return this.tagVS;}
    }

}
