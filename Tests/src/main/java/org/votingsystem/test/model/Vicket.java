package org.votingsystem.test.model;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.model.*;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.DateUtils;
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
public class Vicket implements Serializable  {

    private static Logger log = Logger.getLogger(Vicket.class);

    public static final long serialVersionUID = 1L;

    public VicketTagVS getTag() {
        return tag;
    }

    public void setTag(VicketTagVS tag) {
        this.tag = tag;
    }

    public enum State { OK, PROJECTED, REJECTED, CANCELLED, EXPENDED, LAPSED;}

    private Long id;
    private BigDecimal amount = null;
    private String currencyCode;

    private String hashCertVS;
    private String originHashCertVS;
    private String vicketServerURL;
    private String reason;

    private Long serialNumber;
    private byte[] content;
    private State state;

    private UserVS userVS;

    private CertificateVS authorityCertificateVS;

    private VicketTagVS tag;

    private TransactionVS transactionParent;

    private MessageSMIME cancelMessage;
    private MessageSMIME messageSMIME;

    private Date validFrom;
    private Date validTo;

    private Date dateCreated;
    private Date lastUpdated;

    private CertificationRequestVS certificationRequest;
    private PKCS10CertificationRequest csr;
    private X509Certificate x509AnonymousCert;

    public Vicket() {}

    public Vicket(PKCS10CertificationRequest csr, BigDecimal vicketValue, String currencyCode, String hashCertVS,
                  String vicketServerURL) {
        this.csr = csr;
        this.amount = vicketValue;
        this.currencyCode = currencyCode;
        this.hashCertVS = hashCertVS;
        this.vicketServerURL = vicketServerURL;
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

    public void setState(State state) {
        this.state = state;
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

    public TransactionVS getTransactionParent() {
        return transactionParent;
    }

    public void setTransactionParent(TransactionVS transactionParent) {
        this.transactionParent = transactionParent;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public CertificationRequestVS getCertificationRequest() {
        return certificationRequest;
    }

    public void setCertificationRequest(CertificationRequestVS certificationRequest) {
        this.certificationRequest = certificationRequest;
    }

    public static CertSubject getCertSubject(String subjectDN) {
        return new CertSubject(subjectDN);
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
        String vicketValue;
        String vicketServerURL;
        String tagVS;
        public CertSubject(String subjectDN) {
            if (subjectDN.contains("CURRENCY_CODE:")) currencyCode = subjectDN.split("CURRENCY_CODE:")[1].split(",")[0];
            if (subjectDN.contains("VICKET_VALUE:")) vicketValue = subjectDN.split("VICKET_VALUE:")[1].split(",")[0];
            if (subjectDN.contains("TAGVS:")) tagVS = subjectDN.split("TAGVS:")[1].split(",")[0];
            if (subjectDN.contains("vicketServerURL:")) vicketServerURL = subjectDN.split("vicketServerURL:")[1].split(",")[0];
        }
        public String getCurrencyCode() {return this.currencyCode;}
        public String getVicketValue() {return this.vicketValue;}
        public String getVicketServerURL() {return this.vicketServerURL;}
        public String gettagVS() {return this.tagVS;}
    }

}
